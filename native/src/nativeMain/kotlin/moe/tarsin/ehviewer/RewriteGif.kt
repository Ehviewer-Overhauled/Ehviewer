package moe.tarsin.ehviewer

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import platform.android.JNIEnvVar
import platform.android.jclass
import platform.android.jobject

@CName("Java_com_hippo_Native_rewriteGif")
fun rewriteGif(env: CPointer<JNIEnvVar>, thiz: jclass, buffer: jobject) {
    val ptr = env.pointed.pointed!!.GetDirectBufferAddress!!.invoke(env, buffer)
}
