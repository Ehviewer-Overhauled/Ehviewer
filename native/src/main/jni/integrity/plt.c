//
// Created by Thom on 2019/2/16.
//

#include <stdint.h>
#include <string.h>
#include <stdbool.h>
#include <stdlib.h>
#include <dlfcn.h>
#include "plt.h"
#include "common.h"
#include "path.h"

/*
 * reference: https://android.googlesource.com/platform/bionic/+/master/linker/linker_soinfo.cpp
 */
static uint32_t gnu_hash(const uint8_t *name) {
    uint32_t h = 5381;

    while (*name) {
        h += (h << 5) + *name++;
    }
    return h;
}

static uint32_t elf_hash(const uint8_t *name) {
    uint32_t h = 0, g;

    while (*name) {
        h = (h << 4) + *name++;
        g = h & 0xf0000000;
        h ^= g;
        h ^= g >> 24;
    }

    return h;
}

static ElfW(Dyn) *find_dyn_by_tag(ElfW(Dyn) *dyn, ElfW(Sxword) tag) {
    while (dyn->d_tag != DT_NULL) {
        if (dyn->d_tag == tag) {
            return dyn;
        }
        ++dyn;
    }
    return NULL;
}

static inline bool is_global(ElfW(Sym) *sym) {
    unsigned char stb = ELF_ST_BIND(sym->st_info);
    if (stb == STB_GLOBAL || stb == STB_WEAK) {
        return sym->st_shndx != SHN_UNDEF;
    } else {
        return false;
    }
}

static ElfW(Addr) *
find_symbol(struct dl_phdr_info *info, ElfW(Dyn) *base_addr, const char *symbol) {
    ElfW(Dyn) *dyn;

    dyn = find_dyn_by_tag(base_addr, DT_SYMTAB);
    ElfW(Sym) *dynsym = (ElfW(Sym) *) (info->dlpi_addr + dyn->d_un.d_ptr);

    dyn = find_dyn_by_tag(base_addr, DT_STRTAB);
    char *dynstr = (char *) (info->dlpi_addr + dyn->d_un.d_ptr);

    dyn = find_dyn_by_tag(base_addr, DT_GNU_HASH);
    if (dyn != NULL) {
        ElfW(Word) *dt_gnu_hash = (ElfW(Word) *) (info->dlpi_addr + dyn->d_un.d_ptr);
        size_t gnu_nbucket_ = dt_gnu_hash[0];
        uint32_t gnu_maskwords_ = dt_gnu_hash[2];
        uint32_t gnu_shift2_ = dt_gnu_hash[3];
        ElfW(Addr) *gnu_bloom_filter_ = (ElfW(Addr) *) (dt_gnu_hash + 4);
        uint32_t *gnu_bucket_ = (uint32_t *) (gnu_bloom_filter_ + gnu_maskwords_);
        uint32_t *gnu_chain_ = gnu_bucket_ + gnu_nbucket_ - dt_gnu_hash[1];

        --gnu_maskwords_;

        uint32_t hash = gnu_hash((uint8_t *) symbol);
        uint32_t h2 = hash >> gnu_shift2_;

        uint32_t bloom_mask_bits = sizeof(ElfW(Addr)) * 8;
        uint32_t word_num = (hash / bloom_mask_bits) & gnu_maskwords_;
        ElfW(Addr) bloom_word = gnu_bloom_filter_[word_num];

        if ((1 & (bloom_word >> (hash % bloom_mask_bits)) &
             (bloom_word >> (h2 % bloom_mask_bits))) == 0) {
            return NULL;
        }

        uint32_t n = gnu_bucket_[hash % gnu_nbucket_];

        if (n == 0) {
            return NULL;
        }

        do {
            ElfW(Sym) *sym = dynsym + n;
            if (((gnu_chain_[n] ^ hash) >> 1) == 0
                && is_global(sym)
                && strcmp(dynstr + sym->st_name, symbol) == 0) {
                ElfW(Addr) *symbol_sym = (ElfW(Addr) *) (info->dlpi_addr + sym->st_value);
#ifdef DEBUG_PLT
                LOGI("found %s(gnu+%u) in %s, %p", symbol, n, info->dlpi_name, symbol_sym);
#endif
                return symbol_sym;
            }
        } while ((gnu_chain_[n++] & 1) == 0);

        return NULL;
    }

    dyn = find_dyn_by_tag(base_addr, DT_HASH);
    if (dyn != NULL) {
        ElfW(Word) *dt_hash = (ElfW(Word) *) (info->dlpi_addr + dyn->d_un.d_ptr);
        size_t nbucket_ = dt_hash[0];
        uint32_t *bucket_ = dt_hash + 2;
        uint32_t *chain_ = bucket_ + nbucket_;

        uint32_t hash = elf_hash((uint8_t *) (symbol));
        for (uint32_t n = bucket_[hash % nbucket_]; n != 0; n = chain_[n]) {
            ElfW(Sym) *sym = dynsym + n;
            if (is_global(sym) &&
                strcmp(dynstr + sym->st_name, symbol) == 0) {
                ElfW(Addr) *symbol_sym = (ElfW(Addr) *) (info->dlpi_addr + sym->st_value);
#ifdef DEBUG_PLT
                LOGI("found %s(elf+%u) in %s, %p", symbol, n, info->dlpi_name, symbol_sym);
#endif
                return symbol_sym;
            }
        }

        return NULL;
    }

    return NULL;
}

#if defined(__LP64__)
#define Elf_Rela ElfW(Rela)
#define ELF_R_SYM ELF64_R_SYM
#else
#define Elf_Rela ElfW(Rel)
#define ELF_R_SYM ELF32_R_SYM
#endif

#ifdef DEBUG_PLT
#if defined(__x86_64__)
#define R_JUMP_SLOT R_X86_64_JUMP_SLOT
#define ELF_R_TYPE  ELF64_R_TYPE
#elif defined(__i386__)
#define R_JUMP_SLOT R_386_JMP_SLOT
#define ELF_R_TYPE  ELF32_R_TYPE
#elif defined(__arm__)
#define R_JUMP_SLOT R_ARM_JUMP_SLOT
#define ELF_R_TYPE  ELF32_R_TYPE
#elif defined(__aarch64__)
#define R_JUMP_SLOT R_AARCH64_JUMP_SLOT
#define ELF_R_TYPE  ELF64_R_TYPE
#else
#error unsupported OS
#endif
#endif

static ElfW(Addr) *find_plt(struct dl_phdr_info *info, ElfW(Dyn) *base_addr, const char *symbol) {
    ElfW(Dyn) *dyn = find_dyn_by_tag(base_addr, DT_JMPREL);
    if (dyn == NULL) {
        return NULL;
    }
    Elf_Rela *dynplt = (Elf_Rela *) (info->dlpi_addr + dyn->d_un.d_ptr);

    dyn = find_dyn_by_tag(base_addr, DT_SYMTAB);
    ElfW(Sym) *dynsym = (ElfW(Sym) *) (info->dlpi_addr + dyn->d_un.d_ptr);

    dyn = find_dyn_by_tag(base_addr, DT_STRTAB);
    char *dynstr = (char *) (info->dlpi_addr + dyn->d_un.d_ptr);

    dyn = find_dyn_by_tag(base_addr, DT_PLTRELSZ);
    if (dyn == NULL) {
        return NULL;
    }
    size_t count = dyn->d_un.d_val / sizeof(Elf_Rela);

    for (size_t i = 0; i < count; ++i) {
        Elf_Rela *plt = dynplt + i;
#ifdef DEBUG_PLT
        if (ELF_R_TYPE(plt->r_info) != R_JUMP_SLOT) {
            LOGW("invalid type for plt+%zu in %s", i, info->dlpi_name);
            continue;
        }
#endif
        size_t idx = ELF_R_SYM(plt->r_info);
        idx = dynsym[idx].st_name;
        if (strcmp(dynstr + idx, symbol) == 0) {
            ElfW(Addr) *symbol_plt = (ElfW(Addr) *) (info->dlpi_addr + plt->r_offset);
#ifdef DEBUG_PLT
            ElfW(Addr) *symbol_plt_value = (ElfW(Addr) *) *symbol_plt;
            LOGI("found %s(plt+%zu) in %s, %p -> %p", symbol, i, info->dlpi_name, symbol_plt,
                 symbol_plt_value);
#endif
            return symbol_plt;
        }
    }

    return NULL;
}

static inline bool isso(const char *str) {
    if (str == NULL) {
        return false;
    }
    const char *dot = strrchr(str, '.');
    return dot != NULL
           && *++dot == 's'
           && *++dot == 'o'
           && (*++dot == '\0' || *dot == '\r' || *dot == '\n');
}

static inline bool should_check_plt(Symbol *symbol, struct dl_phdr_info *info) {
    const char *path = info->dlpi_name;
    if (symbol->check & PLT_CHECK_PLT_ALL) {
        return true;
    } else if (symbol->check & PLT_CHECK_PLT_APP) {
        return *path != '/' || isThirdParty(path);
    } else {
        return false;
    }
}

static int callback(struct dl_phdr_info *info, __unused size_t size, void *data) {
    if (!isso(info->dlpi_name)) {
#ifdef DEBUG_PLT
        LOGW("ignore non-so: %s", info->dlpi_name);
#endif
        return 0;
    }
    Symbol *symbol = (Symbol *) data;
#if 0
    LOGI("Name: \"%s\" (%d segments)", info->dlpi_name, info->dlpi_phnum);
#endif
    ++symbol->total;
    for (ElfW(Half) phdr_idx = 0; phdr_idx < info->dlpi_phnum; ++phdr_idx) {
        ElfW(Phdr) phdr = info->dlpi_phdr[phdr_idx];
        if (phdr.p_type != PT_DYNAMIC) {
            continue;
        }
        ElfW(Dyn) *base_addr = (ElfW(Dyn) *) (info->dlpi_addr + phdr.p_vaddr);
        ElfW(Addr) *addr;
        addr = should_check_plt(symbol, info) ? find_plt(info, base_addr, symbol->symbol_name) : NULL;
        if (addr != NULL) {
            if (symbol->symbol_plt != NULL) {
                ElfW(Addr) *addr_value = (ElfW(Addr) *) *addr;
                ElfW(Addr) *symbol_plt_value = (ElfW(Addr) *) *symbol->symbol_plt;
                if (addr_value != symbol_plt_value) {
#ifdef DEBUG_PLT
                    LOGW("%s, plt %p -> %p != %p", symbol->symbol_name, addr, addr_value,
                         symbol_plt_value);
#endif
                    return 1;
                }
            }
            symbol->symbol_plt = addr;
            if (symbol->check & PLT_CHECK_NAME) {
                if (symbol->size == 0) {
                    symbol->size = 1;
                    symbol->names = calloc(1, sizeof(char *));
                } else {
                    ++symbol->size;
                    symbol->names = realloc(symbol->names, symbol->size * sizeof(char *));
                }
#ifdef DEBUG_PLT
                LOGI("[%d]: %s", symbol->size - 1, info->dlpi_name);
#endif
                symbol->names[symbol->size - 1] = strdup(info->dlpi_name);
            }
        }
        addr = find_symbol(info, base_addr, symbol->symbol_name);
        if (addr != NULL) {
            symbol->symbol_sym = addr;
            if (symbol->check == PLT_CHECK_SYM_ONE) {
                return PLT_CHECK_SYM_ONE;
            }
        }
        if (symbol->symbol_plt != NULL && symbol->symbol_sym != NULL) {
            ElfW(Addr) *symbol_plt_value = (ElfW(Addr) *) *symbol->symbol_plt;
            // stop if unmatch
            if (symbol_plt_value != symbol->symbol_sym) {
#ifdef DEBUG_PLT
                LOGW("%s, plt: %p -> %p != %p", symbol->symbol_name, symbol->symbol_plt,
                     symbol_plt_value, symbol->symbol_sym);
#endif
                return 1;
            }
        }
    }
    return 0;
}

void *plt_dlsym(const char *name, size_t *total) {
    Symbol symbol;
    memset(&symbol, 0, sizeof(Symbol));
    if (total == NULL) {
        symbol.check = PLT_CHECK_SYM_ONE;
    }
    symbol.symbol_name = name;
    dl_iterate_phdr_symbol(&symbol);
    if (total != NULL) {
        *total = symbol.total;
    }
    return symbol.symbol_sym;
}

bool isPltHooked(const char *name, bool all) {
    Symbol symbol;
    memset(&symbol, 0, sizeof(Symbol));
    symbol.check = all ? PLT_CHECK_PLT_ALL : PLT_CHECK_PLT_APP;
    symbol.symbol_name = name;
    return dl_iterate_phdr_symbol(&symbol) ? true : false;
}

/**
 * symbol->check PLT_CHECK_PLT | PLT_CHECK_NAME
 * @param symbol
 * @return
 */
int dl_iterate_phdr_symbol(Symbol *symbol) {
    int result;
#ifdef DEBUG_PLT
    LOGI("start dl_iterate_phdr: %s", symbol->symbol_name);
#endif
#if __ANDROID_API__ >= 21 || !defined(__arm__)
    result = dl_iterate_phdr(callback, symbol);
#else
    int (*dl_iterate_phdr)(int (*)(struct dl_phdr_info *, size_t, void *), void *);
    dl_iterate_phdr = dlsym(RTLD_NEXT, "dl_iterate_phdr");
    if (dl_iterate_phdr != NULL) {
        result = dl_iterate_phdr(callback, symbol);
    } else {
        result = 0;
        void *handle = dlopen("libdl.so", RTLD_NOW);
        dl_iterate_phdr = dlsym(handle, "dl_iterate_phdr");
        if (dl_iterate_phdr != NULL) {
            result = dl_iterate_phdr(callback, symbol);
        } else {
            LOGW("cannot dlsym dl_iterate_phdr");
        }
        dlclose(handle);
    }
#endif
#ifdef DEBUG_PLT
    LOGI("complete dl_iterate_phdr: %s", symbol->symbol_name);
#endif
    return result;
}
