package com.hippo.util

import android.os.ParcelFileDescriptor
import android.system.Int64Ref
import android.system.Os
import java.io.FileDescriptor

private fun sendFileTotally(from: FileDescriptor, to: FileDescriptor) {
    Os.sendfile(to, from, Int64Ref(0), Long.MAX_VALUE)
}

infix fun ParcelFileDescriptor.sendTo(fd: FileDescriptor) {
    sendFileTotally(fileDescriptor, fd)
}

infix fun ParcelFileDescriptor.sendTo(fd: ParcelFileDescriptor) {
    sendFileTotally(fileDescriptor, fd.fileDescriptor)
}
