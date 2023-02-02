package moe.tarsin.ehviewer

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.value
import platform.android.JNIEnvVar
import platform.android.jclass
import platform.android.jint
import platform.android.jlong
import platform.android.jobject
import platform.posix.MAP_PRIVATE
import platform.posix.PROT_READ
import platform.posix.PROT_WRITE
import platform.posix.mmap

@OptIn(UnsafeNumber::class)
@Suppress("UNUSED_PARAMETER")
@CName("Java_com_hippo_Native_mapFd")
fun mapFd(env: CPointer<JNIEnvVar>, clazz: jclass, fd: jint, capability: jlong): jobject {
    val result = mmap(null, capability.tosize_t(), PROT_READ or PROT_WRITE, MAP_PRIVATE, fd, 0)
    return env.newDirectByteBuffer(result!!, capability)
}
