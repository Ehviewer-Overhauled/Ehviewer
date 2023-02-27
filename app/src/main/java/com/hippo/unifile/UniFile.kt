package com.hippo.unifile

import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.os.ParcelFileDescriptor.AutoCloseOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

fun UniFile.openInputStream(): FileInputStream {
    return AutoCloseInputStream(openFileDescriptor("r"))
}

fun UniFile.openOutputStream(): FileOutputStream {
    return AutoCloseOutputStream(openFileDescriptor("w"))
}
