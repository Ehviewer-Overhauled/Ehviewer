package moe.tarsin.ehviewer

import kotlinx.cinterop.CPointer
import platform.android.JNIEnvVar
import platform.android.jclass
import platform.android.jobject

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_hippo_Native_rewriteGif")
fun rewriteGif(env: CPointer<JNIEnvVar>, clazz: jclass, buffer: jobject) {
    val ptr = env.getDirectBufferAddress(buffer)
    val size = env.getDirectBufferCapacity(buffer)
}
