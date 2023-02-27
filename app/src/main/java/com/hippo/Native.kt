/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.hippo

import android.os.ParcelFileDescriptor
import android.system.Int64Ref
import android.system.Os
import java.io.FileDescriptor

object Native {
    fun initialize() {
        System.loadLibrary("ehviewer")
    }

    @JvmStatic
    external fun getFd(fd: FileDescriptor?): Int
}

private fun sendFileTotally(from: FileDescriptor, to: FileDescriptor) {
    Os.sendfile(to, from, Int64Ref(0), Long.MAX_VALUE)
}

val FileDescriptor.fd
    get() = Native.getFd(this)

infix fun ParcelFileDescriptor.sendTo(fd: FileDescriptor) {
    sendFileTotally(fileDescriptor, fd)
}

infix fun ParcelFileDescriptor.sendTo(fd: ParcelFileDescriptor) {
    sendFileTotally(fileDescriptor, fd.fileDescriptor)
}
