package moe.tarsin.ehviewer

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.nativeNullPtr
import kotlinx.cinterop.value
import platform.android.JNIEnvVar
import platform.android.jclass
import platform.android.jint
import platform.android.jlong
import platform.android.jobject
import platform.posix.MAP_PRIVATE
import platform.posix.PROT_READ
import platform.posix.PROT_WRITE
import platform.posix.errno
import platform.posix.mmap
import platform.posix.munmap

@OptIn(UnsafeNumber::class)
@Suppress("UNUSED_PARAMETER")
@CName("Java_com_hippo_Native_mapFd")
fun mapFd(env: CPointer<JNIEnvVar>, clazz: jclass, fd: jint, capability: jlong): jobject {
    val result = mmap(null, capability.tosize_t(), PROT_READ or PROT_WRITE, MAP_PRIVATE, fd, 0)
    result ?: Warn("mmap call with fd:${fd} capability:${capability} failed!, errno:${errno}")
    return result?.let { env.newDirectByteBuffer(result, capability) } ?: interpretCPointer(nativeNullPtr)!!
}

@Suppress("UNUSED_PARAMETER")
@OptIn(UnsafeNumber::class)
@CName("Java_com_hippo_Native_unmapDirectByteBuffer")
fun unmapFd(env: CPointer<JNIEnvVar>, clazz: jclass, buffer: jobject) {
    val addr = env.getDirectBufferAddress(buffer)
    val size = env.getDirectBufferCapacity(buffer)
    val result = munmap(addr, size.tosize_t())
    if (result != 0) Warn("mmap call with addr:${addr} size:${size} failed!, errno:${errno}")
}
