package moe.tarsin.ehviewer

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import platform.android.JNIEnvVar
import platform.android.JNINativeInterface
import platform.android.jobject

fun CPointer<JNIEnvVar>.getNativeInterface(): JNINativeInterface {
    return pointed.pointed!!
}

fun CPointer<JNIEnvVar>.getDirectBufferAddress(buffer: jobject): COpaquePointer {
    return getNativeInterface().GetDirectBufferAddress!!.invoke(this, buffer)!!
}

fun CPointer<JNIEnvVar>.getDirectBufferCapacity(buffer: jobject): Long {
    return getNativeInterface().GetDirectBufferCapacity!!.invoke(this, buffer)
}

fun CPointer<JNIEnvVar>.newDirectByteBuffer(addr: COpaquePointer, capability: Long): jobject {
    return getNativeInterface().NewDirectByteBuffer!!.invoke(this, addr, capability)!!
}
