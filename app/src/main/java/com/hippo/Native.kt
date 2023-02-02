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

import java.io.FileDescriptor
import java.nio.ByteBuffer

object Native {
    fun initialize() {
        System.loadLibrary("ehviewer")
        System.loadLibrary("ehviewer_kotlin_native")
    }

    @JvmStatic
    external fun getFd(fd: FileDescriptor?): Int

    @JvmStatic
    external fun mapFd(fd: Int, capability: Long): ByteBuffer?

    @JvmStatic
    external fun unmapDirectByteBuffer(buffer: ByteBuffer)
}
