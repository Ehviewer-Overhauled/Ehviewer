// Copyright 2021 The Fuse-Archive Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// ----------------

// fuse-archive read-only mounts an archive or compressed file (e.g. foo.tar,
// foo.tar.gz, foo.xz, foo.zip) as a FUSE file system
// (https://en.wikipedia.org/wiki/Filesystem_in_Userspace).
//
// To build:
//   g++ -O3 main.cc `pkg-config libarchive fuse --cflags --libs` -o example
//
// To use:
//   ./example ../test/data/archive.zip the/path/to/the/mountpoint
//   ls -l                              the/path/to/the/mountpoint
//   fusermount -u                      the/path/to/the/mountpoint
//
// Pass the "-f" flag to "./example" for foreground operation.

// ---- Preprocessor

#define FUSE_USE_VERSION 35

#include <archive.h>
#include <archive_entry.h>
#include <errno.h>
#include <fcntl.h>
#include <fuse.h>
#include <langinfo.h>
#include <locale.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <syslog.h>

#include <atomic>
#include <chrono>
#include <climits>
#include <memory>
#include <string>
#include <thread>
#include <unordered_map>
#include <vector>

// fuse_file_info.fh is a uint64_t. Check that it can hold a pointer.
#if UINTPTR_MAX > UINT64_MAX
#error "fuse-archive requires that casting a uintptr_t to uint64_t is lossless"
#endif

#define TRY(operation)               \
  do {                               \
    int try_status_code = operation; \
    if (try_status_code) {           \
      return try_status_code;        \
    }                                \
  } while (false)

// ---- Exit Codes

// These are values passed to the exit function, or returned by main. These are
// (Linux or Linux-like) application exit codes, not library error codes.
//
// Note that, unless the -f command line option was passed for foreground
// operation, the parent process may very well ignore the exit code value after
// daemonization succeeds.

#define EXIT_CODE_GENERIC_FAILURE 1
// Exit code 2 is skipped: https://tldp.org/LDP/abs/html/exitcodes.html

#define EXIT_CODE_CANNOT_OPEN_ARCHIVE 11

#define EXIT_CODE_PASSPHRASE_REQUIRED 20
#define EXIT_CODE_PASSPHRASE_INCORRECT 21
#define EXIT_CODE_PASSPHRASE_NOT_SUPPORTED 22

#define EXIT_CODE_INVALID_RAW_ARCHIVE 30
#define EXIT_CODE_INVALID_ARCHIVE_HEADER 31
#define EXIT_CODE_INVALID_ARCHIVE_CONTENTS 32

// ---- Compile-time Configuration

#define PROGRAM_NAME "fuse-archive"

#ifndef FUSE_ARCHIVE_VERSION
#define FUSE_ARCHIVE_VERSION "0.1.11"
#endif

#ifndef BLOCK_SIZE
#define BLOCK_SIZE 16384
#endif

#ifndef NUM_SAVED_READERS
#define NUM_SAVED_READERS 8
#endif

#ifndef NUM_SIDE_BUFFERS
#define NUM_SIDE_BUFFERS 8
#elif NUM_SIDE_BUFFERS <= 0
#error "invalid NUM_SIDE_BUFFERS"
#endif

// This defaults to 128 KiB (0x20000 bytes) because, on a vanilla x86_64 Debian
// Linux, that seems to be the largest buffer size passed to my_read.
#ifndef SIDE_BUFFER_SIZE
#define SIDE_BUFFER_SIZE 131072
#elif SIDE_BUFFER_SIZE <= 0
#error "invalid SIDE_BUFFER_SIZE"
#endif

// ---- Platform specifics

#if defined(__FreeBSD__) || defined(__OpenBSD__) || defined(__APPLE__)
#define lseek64 lseek
#endif

// ---- Globals

static struct options {
    bool help;
    bool version;

    bool passphrase;
    bool quiet;
    bool redact;
    const char* asyncprogress;
} g_options = {};

enum {
    // Options shared with libfuse's helper.c.
    MY_KEY_HELP = 100,
    MY_KEY_VERSION = 101,

    // Options exclusive to fuse-archive.
    MY_KEY_ASYNCPROGRESS = 200,
    MY_KEY_EAGER = 201,
    MY_KEY_PASSPHRASE = 202,
    MY_KEY_QUIET = 203,
    MY_KEY_REDACT = 204,
};

static struct fuse_opt g_fuse_opts[] = {
        FUSE_OPT_KEY("-h", MY_KEY_HELP),                           //
        FUSE_OPT_KEY("--help", MY_KEY_HELP),                       //
        FUSE_OPT_KEY("-V", MY_KEY_VERSION),                        //
        FUSE_OPT_KEY("--version", MY_KEY_VERSION),                 //
        FUSE_OPT_KEY("--asyncprogress=%s", MY_KEY_ASYNCPROGRESS),  //
        FUSE_OPT_KEY("asyncprogress=%s", MY_KEY_ASYNCPROGRESS),    //
        FUSE_OPT_KEY("--passphrase", MY_KEY_PASSPHRASE),           //
        FUSE_OPT_KEY("passphrase", MY_KEY_PASSPHRASE),             //
        FUSE_OPT_KEY("--quiet", MY_KEY_QUIET),                     //
        FUSE_OPT_KEY("-q", MY_KEY_QUIET),                          //
        FUSE_OPT_KEY("--redact", MY_KEY_REDACT),                   //
        FUSE_OPT_KEY("redact", MY_KEY_REDACT),                     //
        // The remaining options are listed for e.g. "-o formatraw" command line
        // compatibility with the https://github.com/cybernoid/archivemount program
        // but are otherwise ignored. For example, this program detects 'raw'
        // archives automatically and only supports read-only, not read-write.
        FUSE_OPT_KEY("formatraw", FUSE_OPT_KEY_DISCARD),  //
        FUSE_OPT_KEY("nobackup", FUSE_OPT_KEY_DISCARD),   //
        FUSE_OPT_KEY("nosave", FUSE_OPT_KEY_DISCARD),     //
        FUSE_OPT_KEY("readonly", FUSE_OPT_KEY_DISCARD),   //
        FUSE_OPT_END,
};

// g_archive_filename is the command line argument naming the archive file.
static const char* g_archive_filename = NULL;

// g_archive_fd is the file descriptor returned by opening g_archive_filename.
static int g_archive_fd = -1;

// g_archive_file_size is the size of the g_archive_filename file.
static int64_t g_archive_file_size = 0;

// g_archive_fd_position_current is the read position of g_archive_fd.
//
// etc_hwm is the etc_current high water mark (the largest value seen). When
// compared to g_archive_file_size, it proxies what proportion of the archive
// has been processed. This matters for 'raw' archives that need a complete
// decompression pass (as they do not have a table of contents within to
// explicitly record the decompressed file size).
static int64_t g_archive_fd_position_current = 0;
static std::atomic<int64_t> g_archive_fd_position_hwm;

// g_archive_realpath holds the canonicalised absolute path of the archive
// file. The command line argument may give a relative filename (one that
// doesn't start with a slash) and the fuse_main function may change the
// current working directory, so subsequent archive_read_open_filename calls
// use this absolute filepath instead. g_archive_filename is still used for
// logging. g_archive_realpath is allocated in pre_initialize and never freed.
static const char* g_archive_realpath = NULL;

// g_passphrase_buffer and g_passphrase_length combine to hold the passphrase,
// if given. The buffer is NUL-terminated but g_passphrase_length excludes the
// final '\0'. If no passphrase was given, g_passphrase_length is zero.
#define PASSPHRASE_BUFFER_LENGTH 1024
static char g_passphrase_buffer[PASSPHRASE_BUFFER_LENGTH] = {};
static int g_passphrase_length = 0;

// g_archive_is_raw is whether the archive file is 'cooked' or 'raw'.
//
// We support 'cooked' archive files (e.g. foo.tar.gz or foo.zip) but also what
// libarchive calls 'raw' files (e.g. foo.gz), which are compressed but not
// explicitly an archive (a collection of files). libarchive can still present
// it as an implicit archive containing 1 file.
static bool g_archive_is_raw = false;

// g_raw_decompressed_size is the implicit size of the sole archive entry, for
// 'raw' archives (which don't always explicitly record the decompressed size).
static int64_t g_raw_decompressed_size = 0;

// g_uid and g_gid are the user/group IDs for the files we serve. They're the
// same as the current uid/gid.
//
// libfuse will override my_getattr's use of these variables if the "-o uid=N"
// or "-o gid=N" command line options are set.
static uid_t g_uid = 0;
static gid_t g_gid = 0;

// We serve ls and stat requests from an in-memory directory tree of nodes.
// Building that tree is one of the first things that we do.
//
// Building is split into two parts and the bulk of it is done in the second
// part, lazily (unless the --asyncprogress flag is set), so that the main
// function can call fuse_main (to bind the mountpoint and daemonize) as fast as
// possible (although the main function still does a preliminary check that the
// archive_filename command line argument actually names an existing file that
// looks like a valid archive).
//
// These global variables connect those two parts.
static int g_initialize_status_code = 0;
static struct archive* g_initialize_archive = nullptr;
static struct archive_entry* g_initialize_archive_entry = nullptr;
static int64_t g_initialize_index_within_archive = -1;

// g_displayed_progress is whether we have printed a progress message.
static bool g_displayed_progress = false;

// These global variables are the in-memory directory tree of nodes.
//
// g_root_node being non-nullptr means that initialization is complete (when
// the --asyncprogress option is not used).
//
// g_asyncprogress_complete being true means that initialization is complete
// (when the --asyncprogress option is used).
//
// Building the directory tree can take minutes, for archive file formats like
// .tar.gz that are compressed but also do not contain an explicit on-disk
// directory of archive entries. Using the --asyncprogress option makes serving
// the first FUSE request (such as a top-level "ls") instantaneous, at the
// expense of making it harder (at the file system level) to determine when the
// initialization is complete and the actual directory tree is being served.
static std::unordered_map<std::string, struct node*> g_nodes_by_name;
static std::vector<struct node*> g_nodes_by_index;
static struct node* g_root_node = nullptr;
static std::atomic<bool> g_asyncprogress_complete;

// g_shutting_down (when the --asyncprogress option is used) communicates to
// the worker thread that the main thread is shutting down.
//
// Normally, the worker thread populates the g_nodes_by_etc containers, then
// flips g_asyncprogress_complete, then the main thread reads those containers.
// All of the container reads happen after all of the container writes, even
// though reads and writes happen in different threads.
//
// However, if the mountpoint is unmounted (e.g. by "fusermount -u") before the
// worker thread is done, the main thread's main function's return can trigger
// those containers' destructors and we now have concurrent and racy writes on
// those global variables.
//
// To avoid a segfault (or other undefined behavior), g_shutting_down tells the
// worker thread to stop early and the main thread then joins it before
// implicitly running the g_nodes_by_etc destructors.
static std::atomic<bool> g_shutting_down;

// g_saved_readers is a cache of warm readers. libarchive is designed for
// streaming access, not random access, and generally does not support seeking
// backwards. For example, if some other program reads "/foo", "/bar" and then
// "/baz" sequentially from an archive (via this program) and those correspond
// to the 60th, 40th and 50th archive entries in that archive, then:
//
//  - A naive implementation (calling archive_read_free when each FUSE file is
//    closed) would have to start iterating from the first archive entry each
//    time a FUSE file is opened, for 150 iterations (60 + 40 + 50) in total.
//  - Saving readers in an LRU (Least Recently Used) cache (calling
//    release_reader when each FUSE file is closed) allows just 110 iterations
//    (60 + 40 + 10) in total. The reader for "/bar" can be re-used for "/baz".
//
// Re-use eligibility is based on the archive entries' sequential numerical
// indexes within the archive, not on their string pathnames.
//
// When copying all of the files out of an archive (e.g. "cp -r" from the
// command line) and the files are accessed in the natural order, caching
// readers means that the overall time can be linear instead of quadratic.
//
// Each array element is a pair. The first half of the pair is a unique_ptr for
// the reader. The second half of the pair is a uint64_t LRU priority value.
// Higher/lower values are more/less recently used and the release_reader
// function evicts the array element with the lowest LRU priority value.
static std::pair<std::unique_ptr<struct reader>, uint64_t>
        g_saved_readers[NUM_SAVED_READERS] = {};

// g_side_buffer_data and g_side_buffer_metadata combine to hold side buffers:
// statically allocated buffers used as a destination for decompressed bytes
// when reader::advance_offset isn't a no-op. These buffers are roughly
// equivalent to Unix's /dev/null or Go's io.Discard as a first approximation.
// However, since we are already producing valid decompressed bytes, by saving
// them (and their metadata), we may be able to serve some subsequent my_read
// requests cheaply, without having to spin up another libarchive decompressor
// to walk forward from the start of the archive entry.
//
// In particular (https://crbug.com/1245925#c18), even when libfuse is single-
// threaded, we have seen kernel readahead causing the offset arguments in a
// sequence of my_read calls to sometimes arrive out-of-order, where
// conceptually consecutive reads are swapped. With side buffers, we can serve
// the second-to-arrive request by a cheap memcpy instead of an expensive
// "re-do decompression from the start". That side-buffer was filled by a
// reader::advance_offset side-effect from serving the first-to-arrive request.
static uint8_t g_side_buffer_data[NUM_SIDE_BUFFERS][SIDE_BUFFER_SIZE] = {};
static struct side_buffer_metadata {
    int64_t index_within_archive;
    int64_t offset_within_entry;
    int64_t length;
    uint64_t lru_priority;

    static uint64_t next_lru_priority;

    bool contains(int64_t index_within_archive,
                  int64_t offset_within_entry,
                  uint64_t length) {
        if ((this->index_within_archive >= 0) &&
            (this->index_within_archive == index_within_archive) &&
            (this->offset_within_entry <= offset_within_entry)) {
            int64_t o = offset_within_entry - this->offset_within_entry;
            return (this->length >= o) &&
                   (static_cast<uint64_t>(this->length - o) >= length);
        }
        return false;
    }
} g_side_buffer_metadata[NUM_SIDE_BUFFERS] = {};
uint64_t side_buffer_metadata::next_lru_priority = 0;

// The side buffers are also repurposed as source (compressed) and destination
// (decompressed) buffers during the initial pass over the archive file.
#define SIDE_BUFFER_INDEX_COMPRESSED 0
#define SIDE_BUFFER_INDEX_DECOMPRESSED 1
#if NUM_SIDE_BUFFERS <= 1
#error "invalid NUM_SIDE_BUFFERS"
#endif

// ---- Libarchive Error Codes

static bool  //
starts_with(const char* s, const char* prefix) {
    if (!s || !prefix) {
        return false;
    }
    size_t ns = strlen(s);
    size_t np = strlen(prefix);
    return (ns >= np) && (strncmp(s, prefix, np) == 0);
}

// determine_passphrase_exit_code converts libarchive errors to fuse-archive
// exit codes. libarchive doesn't have designated passphrase-related error
// numbers. As for whether a particular archive file's encryption is supported,
// libarchive isn't consistent in archive_read_has_encrypted_entries returning
// ARCHIVE_READ_FORMAT_ENCRYPTION_UNSUPPORTED. Instead, we do a string
// comparison on the various possible error messages.
static int  //
determine_passphrase_exit_code(const char* const e) {
    if (starts_with(e, "Incorrect passphrase")) {
        return EXIT_CODE_PASSPHRASE_INCORRECT;
    }

    if (starts_with(e, "Passphrase required")) {
        return EXIT_CODE_PASSPHRASE_REQUIRED;
    }

    static const char* const not_supported_prefixes[] = {
            "Crypto codec not supported",
            "Decryption is unsupported",
            "Encrypted file is unsupported",
            "Encryption is not supported",
            "RAR encryption support unavailable",
            "The archive header is encrypted, but currently not supported",
            "The file content is encrypted, but currently not supported",
            "Unsupported encryption format",
    };

    for (const char* const prefix : not_supported_prefixes) {
        if (starts_with(e, prefix)) {
            return EXIT_CODE_PASSPHRASE_NOT_SUPPORTED;
        }
    }

    return EXIT_CODE_INVALID_ARCHIVE_CONTENTS;
}

// ---- Logging

// redact replaces s with a placeholder string when the "--redact" command line
// option was given. This may prevent Personally Identifiable Information (PII)
// such as archive filenames or archive entry pathnames from being logged.
static const char*  //
redact(const char* s) {
    return g_options.redact ? "[REDACTED]" : s;
}

// ---- Libarchive Read Callbacks

static uint32_t  //
initialization_progress_out_of_1000000() {
    int64_t m = g_archive_fd_position_hwm.load();
    int64_t n = g_archive_file_size;
    if ((m <= 0) || (n <= 0)) {
        return 0;
    } else if (m >= n) {
        return 1000000;
    }
    double x = ((double)m) / ((double)n);
    return ((uint32_t)(1000000 * x));
}

static void  //
update_g_archive_fd_position_hwm() {
    int64_t h = g_archive_fd_position_hwm.load();
    if (h < g_archive_fd_position_current) {
        g_archive_fd_position_hwm.store(g_archive_fd_position_current);
    }

    const auto period = std::chrono::seconds(1);
    static auto next = std::chrono::steady_clock::now() + period;
    const auto now = std::chrono::steady_clock::now();
    if (!g_options.quiet && (now >= next)) {
        next = now + period;
        const int percent = initialization_progress_out_of_1000000() / 10000;
        if (isatty(STDERR_FILENO)) {
            if (g_displayed_progress) {
                fprintf(stderr, "\e[F\e[K");
            }
            fprintf(stderr, "Loading %d%%\n", percent);
            fflush(stderr);
        } else {
            syslog(LOG_INFO, "Loading %d%%", percent);
        }
        g_displayed_progress = true;
    }
}

// The callbacks below are only used during start-up, for the initial pass
// through the archive to build the node tree, based on the g_archive_fd file
// descriptor that stays open for the lifetime of the process. They are like
// libarchive's built-in "read from a file" callbacks but also update
// g_archive_fd_position_etc. The callback_data arguments are ignored in favor
// of global variables.

static int  //
my_file_close(struct archive* a, void* callback_data) {
    return ARCHIVE_OK;
}

static int  //
my_file_open(struct archive* a, void* callback_data) {
    g_archive_fd_position_current = 0;
    g_archive_fd_position_hwm.store(0);
    g_asyncprogress_complete.store(false);
    return ARCHIVE_OK;
}

static ssize_t  //
my_file_read(struct archive* a, void* callback_data, const void** out_dst_ptr) {
    if (g_options.asyncprogress && g_shutting_down.load()) {
        archive_set_error(a, ECANCELED, "shutting down");
        return ARCHIVE_FATAL;
    } else if (g_archive_fd < 0) {
        archive_set_error(a, EIO, "invalid g_archive_fd");
        return ARCHIVE_FATAL;
    }
    uint8_t* dst_ptr = &g_side_buffer_data[SIDE_BUFFER_INDEX_COMPRESSED][0];
    while (true) {
        ssize_t n = read(g_archive_fd, dst_ptr, SIDE_BUFFER_SIZE);
        if (n >= 0) {
            g_archive_fd_position_current += n;
            update_g_archive_fd_position_hwm();
            *out_dst_ptr = dst_ptr;
            return n;
        } else if (errno == EINTR) {
            continue;
        }
        archive_set_error(a, errno, "could not read archive file: %s",
                          strerror(errno));
        break;
    }
    return ARCHIVE_FATAL;
}

static int64_t  //
my_file_seek(struct archive* a,
             void* callback_data,
             int64_t offset,
             int whence) {
    if (g_options.asyncprogress && g_shutting_down.load()) {
        archive_set_error(a, ECANCELED, "shutting down");
        return ARCHIVE_FATAL;
    } else if (g_archive_fd < 0) {
        archive_set_error(a, EIO, "invalid g_archive_fd");
        return ARCHIVE_FATAL;
    }
    int64_t o = lseek64(g_archive_fd, offset, whence);
    if (o >= 0) {
        g_archive_fd_position_current = o;
        update_g_archive_fd_position_hwm();
        return o;
    }
    archive_set_error(a, errno, "could not seek in archive file: %s",
                      strerror(errno));
    return ARCHIVE_FATAL;
}

static int64_t  //
my_file_skip(struct archive* a, void* callback_data, int64_t delta) {
    if (g_options.asyncprogress && g_shutting_down.load()) {
        archive_set_error(a, ECANCELED, "shutting down");
        return ARCHIVE_FATAL;
    } else if (g_archive_fd < 0) {
        archive_set_error(a, EIO, "invalid g_archive_fd");
        return ARCHIVE_FATAL;
    }
    int64_t o0 = lseek64(g_archive_fd, 0, SEEK_CUR);
    int64_t o1 = lseek64(g_archive_fd, delta, SEEK_CUR);
    if ((o1 >= 0) && (o0 >= 0)) {
        g_archive_fd_position_current = o1;
        update_g_archive_fd_position_hwm();
        return o1 - o0;
    }
    archive_set_error(a, errno, "could not seek in archive file: %s",
                      strerror(errno));
    return ARCHIVE_FATAL;
}

static int  //
my_file_switch(struct archive* a, void* callback_data0, void* callback_data1) {
    return ARCHIVE_OK;
}

static int  //
my_archive_read_open(struct archive* a) {
    TRY(archive_read_set_callback_data(a, nullptr));
    TRY(archive_read_set_close_callback(a, my_file_close));
    TRY(archive_read_set_open_callback(a, my_file_open));
    TRY(archive_read_set_read_callback(a, my_file_read));
    TRY(archive_read_set_seek_callback(a, my_file_seek));
    TRY(archive_read_set_skip_callback(a, my_file_skip));
    TRY(archive_read_set_switch_callback(a, my_file_switch));
    return archive_read_open1(a);
}

// ---- Side Buffer

// acquire_side_buffer returns the index of the least recently used side
// buffer. This indexes g_side_buffer_data and g_side_buffer_metadata.
static int  //
acquire_side_buffer() {
    // The preprocessor already checks "#elif NUM_SIDE_BUFFERS <= 0".
    int oldest_i = 0;
    uint64_t oldest_lru_priority = g_side_buffer_metadata[0].lru_priority;
    for (int i = 1; i < NUM_SIDE_BUFFERS; i++) {
        if (oldest_lru_priority > g_side_buffer_metadata[i].lru_priority) {
            oldest_lru_priority = g_side_buffer_metadata[i].lru_priority;
            oldest_i = i;
        }
    }
    g_side_buffer_metadata[oldest_i].index_within_archive = -1;
    g_side_buffer_metadata[oldest_i].offset_within_entry = -1;
    g_side_buffer_metadata[oldest_i].length = -1;
    g_side_buffer_metadata[oldest_i].lru_priority = UINT64_MAX;
    return oldest_i;
}

static bool  //
read_from_side_buffer(int64_t index_within_archive,
                      char* dst_ptr,
                      size_t dst_len,
                      int64_t offset_within_entry) {
    // Find the longest side buffer that contains (index_within_archive,
    // offset_within_entry, dst_len).
    int best_i = -1;
    int64_t best_length = -1;
    for (int i = 0; i < NUM_SIDE_BUFFERS; i++) {
        struct side_buffer_metadata* meta = &g_side_buffer_metadata[i];
        if ((meta->length > best_length) &&
            meta->contains(index_within_archive, offset_within_entry, dst_len)) {
            best_i = i;
            best_length = meta->length;
        }
    }

    if (best_i >= 0) {
        struct side_buffer_metadata* meta = &g_side_buffer_metadata[best_i];
        meta->lru_priority = ++side_buffer_metadata::next_lru_priority;
        int64_t o = offset_within_entry - meta->offset_within_entry;
        memcpy(dst_ptr, g_side_buffer_data[best_i] + o, dst_len);
        return true;
    }
    return false;
}

// ---- Reader

// reader bundles libarchive concepts (an archive and an archive entry) and
// other state to point to a particular offset (in decompressed space) of a
// particular archive entry (identified by its index) in an archive.
//
// A reader is backed by its own archive_read_open_filename call, managed by
// libarchive, so each can be positioned independently.
struct reader {
    struct archive* archive;
    struct archive_entry* archive_entry;
    int64_t index_within_archive;
    int64_t offset_within_entry;

    reader(struct archive* _archive)
            : archive(_archive),
              archive_entry(nullptr),
              index_within_archive(-1),
              offset_within_entry(0) {}

    ~reader() {
        if (this->archive) {
            archive_read_free(this->archive);
        }
    }

    // advance_index walks forward until positioned at the want'th index. An
    // index identifies an archive entry. If this reader wasn't already
    // positioned at that index, it also resets the reader's offset to zero.
    //
    // It returns success (true) or failure (false).
    bool advance_index(int64_t want) {
        if (!this->archive) {
            return false;
        }
        while (this->index_within_archive < want) {
            int status =
                    archive_read_next_header(this->archive, &this->archive_entry);
            if (status == ARCHIVE_EOF) {
                syslog(LOG_ERR, "inconsistent archive %s", redact(g_archive_filename));
                return false;
            } else if ((status != ARCHIVE_OK) && (status != ARCHIVE_WARN)) {
                syslog(LOG_ERR, "invalid archive %s: %s", redact(g_archive_filename),
                       archive_error_string(this->archive));
                return false;
            }
            this->index_within_archive++;
            this->offset_within_entry = 0;
        }
        return true;
    }

    // advance_offset walks forward until positioned at the want'th offset. An
    // offset identifies a byte position relative to the start of an archive
    // entry's decompressed contents.
    //
    // The pathname is used for log messages.
    //
    // It returns success (true) or failure (false).
    bool advance_offset(int64_t want, const char* pathname) {
        if (!this->archive || !this->archive_entry) {
            return false;
        } else if (want < this->offset_within_entry) {
            // We can't walk backwards.
            return false;
        } else if (want == this->offset_within_entry) {
            // We are exactly where we want to be.
            return true;
        }

        // We are behind where we want to be. Advance (decompressing from the
        // archive entry into a side buffer) until we get there.
        int sb = acquire_side_buffer();
        if ((sb < 0) || (NUM_SIDE_BUFFERS <= sb)) {
            return false;
        }
        uint8_t* dst_ptr = g_side_buffer_data[sb];
        struct side_buffer_metadata* meta = &g_side_buffer_metadata[sb];
        while (want > this->offset_within_entry) {
            int64_t original_owe = this->offset_within_entry;
            int64_t dst_len = want - original_owe;
            // If the amount we need to advance is greater than the SIDE_BUFFER_SIZE,
            // we need multiple this->read calls, but the total advance might not be
            // an exact multiple of SIDE_BUFFER_SIZE. Read that remainder amount
            // first, not last. For example, if advancing 260KiB with a 128KiB
            // SIDE_BUFFER_SIZE then read 4+128+128 instead of 128+128+4. This leaves
            // a full side buffer when we've finished advancing, maximizing later
            // requests' chances of side-buffer-as-cache hits.
            if (dst_len > SIDE_BUFFER_SIZE) {
                dst_len %= SIDE_BUFFER_SIZE;
                if (dst_len == 0) {
                    dst_len = SIDE_BUFFER_SIZE;
                }
            }

            ssize_t n = this->read(dst_ptr, dst_len, pathname);
            if (n < 0) {
                meta->index_within_archive = -1;
                meta->offset_within_entry = -1;
                meta->length = -1;
                meta->lru_priority = 0;
                return false;
            }
            meta->index_within_archive = this->index_within_archive;
            meta->offset_within_entry = original_owe;
            meta->length = n;
            meta->lru_priority = ++side_buffer_metadata::next_lru_priority;
        }
        return true;
    }

    // read copies from the archive entry's decompressed contents to the
    // destination buffer. It also advances the reader's offset_within_entry.
    //
    // The pathname is used for log messages.
    ssize_t read(void* dst_ptr, size_t dst_len, const char* pathname) {
        ssize_t n = archive_read_data(this->archive, dst_ptr, dst_len);
        if (n < 0) {
            syslog(LOG_ERR, "could not serve %s from %s: %s", redact(pathname),
                   redact(g_archive_filename), archive_error_string(this->archive));
            return -EIO;
        } else if (static_cast<size_t>(n) > dst_len) {
            syslog(LOG_ERR, "too much data serving %s from %s", redact(pathname),
                   redact(g_archive_filename));
            // Something has gone wrong, possibly a buffer overflow, so abort.
            abort();
        }
        this->offset_within_entry += n;
        return n;
    }

    // swap swaps fields with another reader.
    void swap(struct reader* that) {
        std::swap(this->archive, that->archive);
        std::swap(this->archive_entry, that->archive_entry);
        std::swap(this->index_within_archive, that->index_within_archive);
        std::swap(this->offset_within_entry, that->offset_within_entry);
    }
};

// compare does a lexicographic comparison of the pairs (i0, o0) and (i1, o1).
static int  //
compare(int64_t index_within_archive0,
        int64_t offset_within_entry0,
        int64_t index_within_archive1,
        int64_t offset_within_entry1) {
    if (index_within_archive0 < index_within_archive1) {
        return -1;
    } else if (index_within_archive0 > index_within_archive1) {
        return +1;
    } else if (offset_within_entry0 < offset_within_entry1) {
        return -1;
    } else if (offset_within_entry0 > offset_within_entry1) {
        return +1;
    }
    return 0;
}

// acquire_reader returns a reader positioned at the start (offset == 0) of the
// given index'th entry of the archive.
static std::unique_ptr<struct reader>  //
acquire_reader(int64_t want_index_within_archive) {
    if (want_index_within_archive < 0) {
        syslog(LOG_ERR, "negative index_within_archive");
        return nullptr;
    }

    int best_i = -1;
    int64_t best_index_within_archive = -1;
    int64_t best_offset_within_entry = -1;
    for (int i = 0; i < NUM_SAVED_READERS; i++) {
        struct reader* sri = g_saved_readers[i].first.get();
        if (sri &&
            (compare(best_index_within_archive, best_offset_within_entry,
                     sri->index_within_archive, sri->offset_within_entry) < 0) &&
            (compare(sri->index_within_archive, sri->offset_within_entry,
                     want_index_within_archive, 0) <= 0)) {
            best_i = i;
            best_index_within_archive = sri->index_within_archive;
            best_offset_within_entry = sri->offset_within_entry;
        }
    }

    std::unique_ptr<struct reader> r;
    if (best_i >= 0) {
        r = std::move(g_saved_readers[best_i].first);
        g_saved_readers[best_i].second = 0;
    } else {
        struct archive* a = archive_read_new();
        if (!a) {
            syslog(LOG_ERR, "out of memory");
            return nullptr;
        }
        if (g_passphrase_length > 0) {
            archive_read_add_passphrase(a, g_passphrase_buffer);
        }
        archive_read_support_filter_all(a);
        archive_read_support_format_all(a);
        archive_read_support_format_raw(a);
        if (archive_read_open_filename(a, g_archive_realpath, BLOCK_SIZE) !=
            ARCHIVE_OK) {
            syslog(LOG_ERR, "could not read %s: %s", redact(g_archive_filename),
                   archive_error_string(a));
            archive_read_free(a);
            return nullptr;
        }
        r = std::make_unique<struct reader>(a);
    }

    if (!r->advance_index(want_index_within_archive)) {
        return nullptr;
    }
    return r;
}

// release_reader returns r to the reader cache.
static void  //
release_reader(std::unique_ptr<struct reader> r) {
    if (NUM_SAVED_READERS <= 0) {
        return;
    }
    int oldest_i = 0;
    uint64_t oldest_lru_priority = g_saved_readers[0].second;
    for (int i = 1; i < NUM_SAVED_READERS; i++) {
        if (oldest_lru_priority > g_saved_readers[i].second) {
            oldest_lru_priority = g_saved_readers[i].second;
            oldest_i = i;
        }
    }
    static uint64_t next_lru_priority = 0;
    g_saved_readers[oldest_i].first = std::move(r);
    g_saved_readers[oldest_i].second = ++next_lru_priority;
}

// ---- In-Memory Directory Tree

struct node {
    std::string rel_name;  // Relative (not absolute) pathname.
    std::string symlink;
    int64_t index_within_archive;
    int64_t size;
    time_t mtime;
    mode_t mode;

    node* last_child;
    node* first_child;
    node* next_sibling;

    node(std::string&& _rel_name,
         std::string&& _symlink,
         int64_t _index_within_archive,
         int64_t _size,
         time_t _mtime,
         mode_t _mode)
            : rel_name(_rel_name),
              symlink(_symlink),
              index_within_archive(_index_within_archive),
              size(_size),
              mtime(_mtime),
              mode(_mode),
              last_child(nullptr),
              first_child(nullptr),
              next_sibling(nullptr) {}

    void add_child(node* n) {
        if (this->last_child == nullptr) {
            this->last_child = n;
            this->first_child = n;
        } else {
            this->last_child->next_sibling = n;
            this->last_child = n;
        }
    }

    void fill_stat(struct stat* z) const {
        if (!z) {
            return;
        }
        memset(z, 0, sizeof(*z));
        z->st_mode = this->mode;
        z->st_nlink = 1;
        z->st_uid = g_uid;
        z->st_gid = g_gid;
        z->st_size = this->size;
        z->st_mtime = this->mtime;
    }
};

// valid_pathname returns whether the C string p is neither "", "./" or "/"
// and, when splitting on '/' into pathname fragments, no fragment is "", "."
// or ".." other than a possibly leading "" or "." fragment when p starts with
// "/" or "./".
//
// If allow_slashes is false then p must not contain "/".
//
// When iterating over fragments, the p pointer does not move but the q and r
// pointers bracket each fragment:
//   "/an/example/pathname"
//    pq-r|      ||       |
//    p   q------r|       |
//    p           q-------r
static bool  //
valid_pathname(const char* const p, bool allow_slashes) {
    if (!p) {
        return false;
    }
    const char* q = p;
    if ((q[0] == '.') && (q[1] == '/')) {
        if (!allow_slashes) {
            return false;
        }
        q += 2;
    } else if (*q == '/') {
        if (!allow_slashes) {
            return false;
        }
        q++;
    }
    if (*q == 0) {
        return false;
    }
    while (true) {
        const char* r = q;
        while (*r != 0) {
            if (*r != '/') {
                r++;
            } else if (!allow_slashes) {
                return false;
            } else {
                break;
            }
        }
        size_t len = r - q;
        if (len == 0) {
            return false;
        } else if ((len == 1) && (q[0] == '.')) {
            return false;
        } else if ((len == 2) && (q[0] == '.') && (q[1] == '.')) {
            return false;
        }
        if (*r == 0) {
            break;
        }
        q = r + 1;
    }
    return true;
}

// normalize_pathname validates and returns e's pathname, prepending a leading
// "/" if it didn't already have one.
static std::string  //
normalize_pathname(struct archive_entry* e) {
    const char* s = archive_entry_pathname_utf8(e);
    if (!s) {
        s = archive_entry_pathname(e);
        if (!s) {
            syslog(LOG_ERR, "archive entry in %s has empty pathname",
                   redact(g_archive_filename));
            return "";
        }
    }
    if (!valid_pathname(s, true)) {
        syslog(LOG_ERR, "archive entry in %s has invalid pathname: %s",
               redact(g_archive_filename), redact(s));
        return "";
    }
    if ((s[0] == '.') && (s[1] == '/')) {
        return std::string(s + 1);
    } else if (*s == '/') {
        return std::string(s);
    }
    return std::string("/") + std::string(s);
}

static int  //
insert_leaf_node(std::string&& pathname,
                 std::string&& symlink,
                 int64_t index_within_archive,
                 int64_t size,
                 time_t mtime,
                 mode_t mode) {
    if (index_within_archive < 0) {
        syslog(LOG_ERR, "negative index_within_archive in %s: %s",
               redact(g_archive_filename), redact(pathname.c_str()));
        return -EIO;
    } else if (g_nodes_by_name.find(pathname) != g_nodes_by_name.end()) {
        syslog(LOG_ERR, "duplicate pathname in %s: %s", redact(g_archive_filename),
               redact(pathname.c_str()));
        return -EIO;
    }
    node* parent = g_root_node;

    mode_t rx_bits = mode & 0555;
    mode_t r_bits = rx_bits & 0444;
    mode_t branch_mode = rx_bits | (r_bits >> 2) | S_IFDIR;
    mode_t leaf_mode = rx_bits;
    if (symlink.empty()) {
        leaf_mode |= S_IFREG;
    } else {
        leaf_mode |= S_IFLNK;
    }

    // p, q and r point to pathname fragments per the valid_pathname comment.
    const char* p = pathname.c_str();
    if ((*p == 0) || (*p != '/')) {
        return 0;
    }
    const char* q = p + 1;
    while (true) {
        // A directory's mtime is the oldest of its leaves' mtimes.
        if (parent->mtime < mtime) {
            parent->mtime = mtime;
        }
        parent->mode |= branch_mode;

        const char* r = q;
        while ((*r != 0) && (*r != '/')) {
            r++;
        }
        std::string abs_pathname(p, r - p);
        std::string rel_pathname(q, r - q);
        if (*r == 0) {
            // Insert an explicit leaf node (a regular file).
            node* n = new node(std::move(rel_pathname), std::move(symlink),
                               index_within_archive, size, mtime, leaf_mode);
            parent->add_child(n);
            // Add to g_nodes_by_name.
            g_nodes_by_name.insert({std::move(abs_pathname), n});
            // Add to g_nodes_by_index.
            while (g_nodes_by_index.size() <
                   static_cast<uint64_t>(index_within_archive)) {
                g_nodes_by_index.push_back(nullptr);
            }
            if (g_nodes_by_index.size() >
                static_cast<uint64_t>(index_within_archive)) {
                syslog(LOG_ERR, "index_within_archive out of order in %s: %s",
                       redact(g_archive_filename), redact(pathname.c_str()));
                return -EIO;
            }
            g_nodes_by_index.push_back(n);
            break;
        }
        q = r + 1;

        // Insert an implicit branch node (a directory).
        auto iter = g_nodes_by_name.find(abs_pathname);
        if (iter == g_nodes_by_name.end()) {
            node* n = new node(std::move(rel_pathname), std::move(symlink), -1, 0,
                               mtime, branch_mode);
            parent->add_child(n);
            g_nodes_by_name.insert(iter, {std::move(abs_pathname), n});
            parent = n;
        } else if (!S_ISDIR(iter->second->mode)) {
            syslog(LOG_ERR, "simultaneous directory and regular file in %s: %s",
                   redact(g_archive_filename), redact(abs_pathname.c_str()));
            return -EIO;
        } else {
            parent = iter->second;
        }
    }
    return 0;
}

static int  //
insert_leaf(struct archive* a,
            struct archive_entry* e,
            int64_t index_within_archive) {
    std::string pathname = normalize_pathname(e);
    if (pathname.empty()) {
        // normalize_pathname already printed a log message.
        return 0;
    }

    std::string symlink;
    mode_t mode = archive_entry_mode(e);
    if (S_ISLNK(mode)) {
        const char* s = archive_entry_symlink_utf8(e);
        if (!s) {
            s = archive_entry_symlink(e);
        }
        if (s) {
            symlink = std::string(s);
        }
        if (symlink.empty()) {
            syslog(LOG_ERR, "empty link in %s: %s", redact(g_archive_filename),
                   redact(pathname.c_str()));
            return 0;
        }
    } else if (!S_ISREG(mode)) {
        syslog(LOG_ERR, "irregular non-link file in %s: %s",
               redact(g_archive_filename), redact(pathname.c_str()));
        return 0;
    }

    int64_t size = archive_entry_size(e);
    if (g_archive_is_raw && (size == 0)) {
        // 'Raw' archives don't always explicitly record the decompressed size.
        // We'll have to decompress it to find out.
        while (true) {
            ssize_t n = archive_read_data(
                    a, g_side_buffer_data[SIDE_BUFFER_INDEX_DECOMPRESSED],
                    SIDE_BUFFER_SIZE);
            if (n == 0) {
                break;
            } else if (n < 0) {
                syslog(LOG_ERR, "could not decompress %s: %s",
                       redact(g_archive_filename), archive_error_string(a));
                return -EIO;
            } else if (n > SIDE_BUFFER_SIZE) {
                syslog(LOG_ERR, "too much data decompressing %s",
                       redact(g_archive_filename));
                // Something has gone wrong, possibly a buffer overflow, so abort.
                abort();
            }
            size += n;
        }
        g_raw_decompressed_size = size;
    }

    return insert_leaf_node(std::move(pathname), std::move(symlink),
                            index_within_archive, size, archive_entry_mtime(e),
                            mode);
}

static int  //
build_tree() {
    if (g_initialize_index_within_archive < 0) {
        return -EIO;
    }
    bool first = true;
    while (true) {
        if (g_options.asyncprogress && g_shutting_down.load()) {
            return -ECANCELED;
        }

        if (first) {
            // The entry was already read by pre_initialize.
            first = false;

        } else {
            int status = archive_read_next_header(g_initialize_archive,
                                                  &g_initialize_archive_entry);
            g_initialize_index_within_archive++;
            if (status == ARCHIVE_EOF) {
                break;
            } else if (status == ARCHIVE_WARN) {
                syslog(LOG_ERR, "libarchive warning for %s: %s",
                       redact(g_archive_filename),
                       archive_error_string(g_initialize_archive));
            } else if (status != ARCHIVE_OK) {
                syslog(LOG_ERR, "invalid archive %s: %s", redact(g_archive_filename),
                       archive_error_string(g_initialize_archive));
                return -EIO;
            }
        }

        if (S_ISDIR(archive_entry_mode(g_initialize_archive_entry))) {
            continue;
        }

        TRY(insert_leaf(g_initialize_archive, g_initialize_archive_entry,
                        g_initialize_index_within_archive));
    }
    return 0;
}

// ---- Lazy Initialization

// This section (pre_initialize and post_initialize_etc) are the "two parts"
// described in the "Building is split into two parts" comment above.

static int  //
read_passphrase_from_stdin() {
    static constexpr int stdin_fd = 0;
    while (g_passphrase_length < PASSPHRASE_BUFFER_LENGTH) {
        ssize_t n = read(stdin_fd, &g_passphrase_buffer[g_passphrase_length],
                         PASSPHRASE_BUFFER_LENGTH - g_passphrase_length);
        if (n < 0) {
            if (errno == EINTR) {
                continue;
            }
            syslog(LOG_ERR, "could not read passphrase from stdin: %m");
            return -errno;
        } else if (n == 0) {
            g_passphrase_buffer[g_passphrase_length] = '\x00';
            return 0;
        }

        int j = g_passphrase_length + n;
        for (int i = g_passphrase_length; i < j; i++) {
            if ((g_passphrase_buffer[i] == '\n') ||
                (g_passphrase_buffer[i] == '\x00')) {
                g_passphrase_buffer[i] = '\x00';
                g_passphrase_length = i;
                return 0;
            }
        }
        g_passphrase_length = j;
    }
    syslog(LOG_ERR, "passphrase was too long");
    return -EIO;
}

static void  //
insert_root_node() {
    static constexpr int64_t index_within_archive = -1;
    g_root_node = new node("", "", index_within_archive, 0, 0, S_IFDIR);
    g_nodes_by_name["/"] = g_root_node;
}

static int  //
pre_initialize() {
    if (!g_archive_filename) {
        syslog(LOG_ERR, "missing archive_filename argument");
        return EXIT_CODE_GENERIC_FAILURE;
    }

    g_archive_realpath = realpath(g_archive_filename, NULL);
    if (!g_archive_realpath) {
        syslog(LOG_ERR, "could not get absolute path of %s: %m",
               redact(g_archive_filename));
        return EXIT_CODE_CANNOT_OPEN_ARCHIVE;
    }

    g_archive_fd = open(g_archive_realpath, O_RDONLY);
    if (g_archive_fd < 0) {
        syslog(LOG_ERR, "could not open %s: %m", redact(g_archive_filename));
        return EXIT_CODE_CANNOT_OPEN_ARCHIVE;
    }

    struct stat z;
    if (fstat(g_archive_fd, &z) != 0) {
        syslog(LOG_ERR, "could not stat %s", redact(g_archive_filename));
        return EXIT_CODE_GENERIC_FAILURE;
    }
    g_archive_file_size = z.st_size;

    if (g_options.passphrase) {
        TRY(read_passphrase_from_stdin());
    }

    g_initialize_archive = archive_read_new();
    if (!g_initialize_archive) {
        syslog(LOG_ERR, "out of memory");
        return EXIT_CODE_GENERIC_FAILURE;
    }
    if (g_passphrase_length > 0) {
        archive_read_add_passphrase(g_initialize_archive, g_passphrase_buffer);
    }
    archive_read_support_filter_all(g_initialize_archive);
    archive_read_support_format_all(g_initialize_archive);
    archive_read_support_format_raw(g_initialize_archive);
    if (my_archive_read_open(g_initialize_archive) != ARCHIVE_OK) {
        syslog(LOG_ERR, "could not open %s: %s", redact(g_archive_filename),
               archive_error_string(g_initialize_archive));
        archive_read_free(g_initialize_archive);
        g_initialize_archive = nullptr;
        g_initialize_archive_entry = nullptr;
        g_initialize_index_within_archive = -1;
        return EXIT_CODE_GENERIC_FAILURE;
    }

    while (true) {
        int status = archive_read_next_header(g_initialize_archive,
                                              &g_initialize_archive_entry);
        g_initialize_index_within_archive++;
        if (status == ARCHIVE_WARN) {
            syslog(LOG_ERR, "libarchive warning for %s: %s",
                   redact(g_archive_filename),
                   archive_error_string(g_initialize_archive));
        } else if (status != ARCHIVE_OK) {
            if (status != ARCHIVE_EOF) {
                syslog(LOG_ERR, "invalid archive %s: %s", redact(g_archive_filename),
                       archive_error_string(g_initialize_archive));
            }
            archive_read_free(g_initialize_archive);
            g_initialize_archive = nullptr;
            g_initialize_archive_entry = nullptr;
            g_initialize_index_within_archive = -1;
            if (status != ARCHIVE_EOF) {
                return EXIT_CODE_INVALID_ARCHIVE_HEADER;
            }
            // Building the tree for an empty archive is trivial.
            insert_root_node();
            return 0;
        }

        if (S_ISDIR(archive_entry_mode(g_initialize_archive_entry))) {
            continue;
        }
        break;
    }

    // For 'raw' archives, check that at least one of the compression filters
    // (e.g. bzip2, gzip) actually triggered. We don't want to mount arbitrary
    // data (e.g. foo.jpeg).
    if (archive_format(g_initialize_archive) == ARCHIVE_FORMAT_RAW) {
        g_archive_is_raw = true;
        int n = archive_filter_count(g_initialize_archive);
        for (int i = 0; true; i++) {
            if (i == n) {
                archive_read_free(g_initialize_archive);
                g_initialize_archive = nullptr;
                g_initialize_archive_entry = nullptr;
                g_initialize_index_within_archive = -1;
                syslog(LOG_ERR, "invalid raw archive: %s", redact(g_archive_filename));
                return EXIT_CODE_INVALID_RAW_ARCHIVE;
            } else if (archive_filter_code(g_initialize_archive, i) !=
                       ARCHIVE_FILTER_NONE) {
                break;
            }
        }

    } else {
        // Otherwise, reading the first byte of the first non-directory entry will
        // reveal whether we also need a passphrase.
        ssize_t n = archive_read_data(
                g_initialize_archive,
                g_side_buffer_data[SIDE_BUFFER_INDEX_DECOMPRESSED], 1);
        if (n < 0) {
            const char* const e = archive_error_string(g_initialize_archive);
            syslog(LOG_ERR, "%s: %s", redact(g_archive_filename), e);
            const int ret = determine_passphrase_exit_code(e);
            archive_read_free(g_initialize_archive);
            g_initialize_archive = nullptr;
            g_initialize_archive_entry = nullptr;
            g_initialize_index_within_archive = -1;
            return ret;
        }
    }

    return 0;
}

static int  //
post_initialize_sync() {
    if (g_initialize_status_code) {
        return g_initialize_status_code;
    }

    if (g_root_node != nullptr) {
        return 0;
    }

    insert_root_node();
    g_initialize_status_code = build_tree();
    archive_read_free(g_initialize_archive);
    g_initialize_archive = nullptr;
    g_initialize_archive_entry = nullptr;
    g_initialize_index_within_archive = -1;
    if (g_archive_fd >= 0) {
        close(g_archive_fd);
        g_archive_fd = -1;
    }
    if (g_displayed_progress && (g_initialize_status_code == 0)) {
        if (isatty(STDERR_FILENO)) {
            fprintf(stderr, "\e[F\e[K");
            fflush(stderr);
        } else {
            syslog(LOG_INFO, "Loaded 100%%");
        }
    }
    return g_initialize_status_code;
}

static void  //
post_initialize_async() {
    post_initialize_sync();
    g_asyncprogress_complete.store(true);
}

// ---- FUSE Callbacks

static int  //
my_getattr(const char* pathname, struct stat* z, fuse_file_info *fi) {
    if (!g_options.asyncprogress) {
        TRY(post_initialize_sync());
    } else if (!g_asyncprogress_complete.load()) {
        if (*pathname == '/') {
            if (pathname[1] == '\x00') {
                memset(z, 0, sizeof(*z));
                z->st_mode = 0555 | S_IFDIR;
                z->st_nlink = 1;
                z->st_uid = g_uid;
                z->st_gid = g_gid;
                z->st_size = 0;
                z->st_mtime = 0;
                return 0;
            } else if (strcmp(pathname + 1, g_options.asyncprogress) == 0) {
                memset(z, 0, sizeof(*z));
                z->st_mode = 0000 | S_IFREG;
                z->st_nlink = 1;
                z->st_uid = g_uid;
                z->st_gid = g_gid;
                z->st_size = 0;
                z->st_mtime = initialization_progress_out_of_1000000();
                return 0;
            }
        }
        return -ENOENT;
    } else if (g_initialize_status_code) {
        return g_initialize_status_code;
    }

    auto iter = g_nodes_by_name.find(pathname);
    if (iter == g_nodes_by_name.end()) {
        return -ENOENT;
    }
    iter->second->fill_stat(z);
    return 0;
}

static int  //
my_readlink(const char* pathname, char* dst_ptr, size_t dst_len) {
    if (!g_options.asyncprogress) {
        TRY(post_initialize_sync());
    } else if (!g_asyncprogress_complete.load()) {
        if (*pathname == '/') {
            if ((pathname[1] == '\x00') ||
                (strcmp(pathname + 1, g_options.asyncprogress) == 0)) {
                return -ENOLINK;
            }
        }
        return -ENOENT;
    } else if (g_initialize_status_code) {
        return g_initialize_status_code;
    }

    auto iter = g_nodes_by_name.find(pathname);
    if (iter == g_nodes_by_name.end()) {
        return -ENOENT;
    }
    node* n = iter->second;
    if (n->symlink.empty() || (dst_len == 0)) {
        return -ENOLINK;
    }
    snprintf(dst_ptr, dst_len, "%s", n->symlink.c_str());
    return 0;
}

static int  //
my_open(const char* pathname, struct fuse_file_info* ffi) {
    if (!g_options.asyncprogress) {
        TRY(post_initialize_sync());
    } else if (!g_asyncprogress_complete.load()) {
        if (*pathname == '/') {
            if (pathname[1] == '\x00') {
                return -EISDIR;
            } else if (strcmp(pathname + 1, g_options.asyncprogress) == 0) {
                return -EACCES;
            }
        }
        return -ENOENT;
    } else if (g_initialize_status_code) {
        return g_initialize_status_code;
    }

    auto iter = g_nodes_by_name.find(pathname);
    if (iter == g_nodes_by_name.end()) {
        return -ENOENT;
    } else if (S_ISDIR(iter->second->mode)) {
        return -EISDIR;
    } else if ((iter->second->index_within_archive < 0) || !ffi) {
        return -EIO;
    } else if ((ffi->flags & O_ACCMODE) != O_RDONLY) {
        return -EACCES;
    }
    std::unique_ptr<struct reader> ur =
            acquire_reader(iter->second->index_within_archive);
    if (!ur) {
        return -EIO;
    }
    ffi->keep_cache = 1;
    ffi->fh = reinterpret_cast<uintptr_t>(ur.release());
    return 0;
}

static int  //
my_read(const char* pathname,
        char* dst_ptr,
        size_t dst_len,
        off_t offset,
        struct fuse_file_info* ffi) {
    if (!g_options.asyncprogress) {
        TRY(post_initialize_sync());
    } else if (!g_asyncprogress_complete.load()) {
        return -EIO;
    } else if (g_initialize_status_code) {
        return g_initialize_status_code;
    }

    if ((offset < 0) || (dst_len > INT_MAX)) {
        return -EINVAL;
    }
    struct reader* r = reinterpret_cast<struct reader*>(ffi->fh);
    if (!r || !r->archive || !r->archive_entry) {
        return -EIO;
    }

    if (r->index_within_archive >= 0) {
        uint64_t i = static_cast<uint64_t>(r->index_within_archive);
        if (i < g_nodes_by_index.size()) {
            struct node* n = g_nodes_by_index[i];
            if (n) {
                if (n->size <= offset) {
                    return 0;
                }
                uint64_t remaining = static_cast<uint64_t>(n->size - offset);
                if (dst_len > remaining) {
                    dst_len = remaining;
                }
            }
        }
    }

    if (dst_len == 0) {
        return 0;
    } else if (read_from_side_buffer(r->index_within_archive, dst_ptr, dst_len,
                                     offset)) {
        return dst_len;
    }

    // libarchive is designed for streaming access, not random access. If we
    // need to seek backwards, there's more work to do.
    if (offset < r->offset_within_entry) {
        // Acquire a new reader, swap it with r and release the new reader. We
        // swap (modify r in-place) instead of updating ffi->fh to point to the
        // new reader, because libfuse ignores any changes to the ffi->fh value
        // after this function returns (this function is not an 'open' callback).
        std::unique_ptr<struct reader> ur = acquire_reader(r->index_within_archive);
        if (!ur || !ur->archive || !ur->archive_entry) {
            return -EIO;
        }
        r->swap(ur.get());
        release_reader(std::move(ur));
    }

    int64_t size = g_archive_is_raw ? g_raw_decompressed_size
                                    : archive_entry_size(r->archive_entry);
    if (size < 0) {
        return -EIO;
    } else if ((size <= offset) || (dst_len == 0)) {
        return 0;
    } else if (!r->advance_offset(offset, pathname)) {
        return -EIO;
    } else if (dst_len > static_cast<uint64_t>(size - offset)) {
        dst_len = static_cast<uint64_t>(size - offset);
    }
    return r->read(dst_ptr, dst_len, pathname);
}

static int  //
my_release(const char* pathname, struct fuse_file_info* ffi) {
    if (!g_options.asyncprogress) {
        TRY(post_initialize_sync());
    } else if (!g_asyncprogress_complete.load()) {
        return -EIO;
    } else if (g_initialize_status_code) {
        return g_initialize_status_code;
    }

    struct reader* r = reinterpret_cast<struct reader*>(ffi->fh);
    if (!r) {
        return -EIO;
    }
    release_reader(std::unique_ptr<struct reader>(r));
    return 0;
}

static int  //
my_readdir(const char* pathname,
           void* buf,
           fuse_fill_dir_t filler,
           off_t offset,
           struct fuse_file_info* ffi, fuse_readdir_flags flag) {
    if (!g_options.asyncprogress) {
        TRY(post_initialize_sync());
    } else if (!g_asyncprogress_complete.load()) {
        if (*pathname == '/') {
            if (pathname[1] == '\x00') {
                if (filler(buf, ".", nullptr, 0, FUSE_FILL_DIR_PLUS) || filler(buf, "..", nullptr, 0, FUSE_FILL_DIR_PLUS) ||
                    filler(buf, g_options.asyncprogress, nullptr, 0, FUSE_FILL_DIR_PLUS)) {
                    return -ENOMEM;
                }
                return 0;
            } else if (strcmp(pathname + 1, g_options.asyncprogress) == 0) {
                return -ENOTDIR;
            }
        }
        return -ENOENT;
    } else if (g_initialize_status_code) {
        return g_initialize_status_code;
    }

    auto iter = g_nodes_by_name.find(pathname);
    if (iter == g_nodes_by_name.end()) {
        return -ENOENT;
    }
    node* n = iter->second;
    if (!S_ISDIR(n->mode)) {
        return -ENOTDIR;
    } else if (filler(buf, ".", NULL, 0, FUSE_FILL_DIR_PLUS) || filler(buf, "..", nullptr, 0, FUSE_FILL_DIR_PLUS)) {
        return -ENOMEM;
    }
    struct stat z;
    for (n = n->first_child; n; n = n->next_sibling) {
        n->fill_stat(&z);
        if (filler(buf, n->rel_name.c_str(), &z, 0, FUSE_FILL_DIR_PLUS)) {
            return -ENOMEM;
        }
    }
    return 0;
}

static void*  //
my_init(struct fuse_conn_info* conn, struct fuse_config* cfg) {
    if (g_options.asyncprogress) {
        // Finish the initialization asynchronously, in a separate thread. This is
        // kicked off here, in my_init, instead of the main function, because of
        // the fuse_main call in between. fuse_main (which calls fuse_daemonize,
        // inside libfuse) can fork-and-kill the current process (unless the -f
        // command line flag was passed for foreground operation), which can also
        // stop any threads started by the main function.
        g_shutting_down.store(false);
        return new std::thread(post_initialize_async);
    }
    return nullptr;
}

static void  //
my_destroy(void* arg) {
    if (arg) {
        g_shutting_down.store(true);
        std::thread* t = static_cast<std::thread*>(arg);
        t->join();
        delete t;
    }
}

static struct fuse_operations my_operations = {
        .getattr = my_getattr,
        .readlink = my_readlink,
        .open = my_open,
        .read = my_read,
        .release = my_release,
        .readdir = my_readdir,
        .init = my_init,
        .destroy = my_destroy,
};

// ---- Main

static int  //
my_opt_proc(void* private_data,
            const char* arg,
            int key,
            struct fuse_args* out_args) {
    static constexpr int error = -1;
    static constexpr int discard = 0;
    static constexpr int keep = 1;

    switch (key) {
        case FUSE_OPT_KEY_NONOPT:
            if (!g_archive_filename) {
                g_archive_filename = arg;
                return discard;
            }
            break;
        case MY_KEY_HELP:
            g_options.help = true;
            return discard;
        case MY_KEY_VERSION:
            g_options.version = true;
            return keep;
        case MY_KEY_ASYNCPROGRESS:
            if (!arg) {
                return error;
            }
            for (; (*arg != '\x00') && (*arg != '='); arg++) {
            }
            if (*arg == '=') {
                arg++;
            }
            if (*arg == '/') {
                arg++;
            }
            if (!valid_pathname(arg, false) || (*arg == '\x00')) {
                syslog(LOG_ERR, "invalid --asyncprogress=etc pathname");
                return error;
            }
            g_options.asyncprogress = strdup(arg);
            if (!g_options.asyncprogress) {
                syslog(LOG_ERR, "out of memory");
                return error;
            }
            return discard;
        case MY_KEY_PASSPHRASE:
            g_options.passphrase = true;
            return discard;
        case MY_KEY_QUIET:
            setlogmask(LOG_UPTO(LOG_ERR));
            g_options.quiet = true;
            return discard;
        case MY_KEY_REDACT:
            g_options.redact = true;
            return discard;
    }
    return keep;
}

static int  //
ensure_utf_8_encoding() {
    // libarchive (especially for reading 7z) has locale-dependent behavior.
    // Non-ASCII pathnames can trigger "Pathname cannot be converted from
    // UTF-16LE to current locale" warnings from archive_read_next_header and
    // archive_entry_pathname_utf8 subsequently returning nullptr.
    //
    // Calling setlocale to enforce a UTF-8 encoding can avoid that. Try various
    // arguments and pick the first one that is supported and produces UTF-8.
    static const char* const locales[] = {
            // As of 2021, many systems (including Debian) support "C.UTF-8".
            "C.UTF-8",
            // However, "C.UTF-8" is not a POSIX standard and glibc 2.34 (released
            // 2021-08-01) does not support it. It may come to glibc 2.35 (see the
            // sourceware.org commit link below), but until then and on older
            // systems, try the popular "en_US.UTF-8".
            //
            // https://sourceware.org/git/?p=glibc.git;a=commit;h=466f2be6c08070e9113ae2fdc7acd5d8828cba50
            "en_US.UTF-8",
            // As a final fallback, an empty string means to use the relevant
            // environment variables (LANG, LC_ALL, etc).
            "",
    };

    for (const char* const locale : locales) {
        if (setlocale(LC_ALL, locale) &&
            (strcmp("UTF-8", nl_langinfo(CODESET)) == 0)) {
            return 0;
        }
    }

    syslog(LOG_ERR, "could not ensure UTF-8 encoding");
    return EXIT_CODE_GENERIC_FAILURE;
}
