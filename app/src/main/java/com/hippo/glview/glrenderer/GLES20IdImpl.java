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

package com.hippo.glview.glrenderer;

import android.opengl.GLES20;

import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

public class GLES20IdImpl implements GLId {
    private final int[] mTempIntArray = new int[1];

    @Override
    public int generateTexture() {
        GLES20.glGenTextures(1, mTempIntArray, 0);
        GLES20Canvas.checkError();
        return mTempIntArray[0];
    }

    @Override
    public void glGenBuffers(int n, int[] buffers, int offset) {
        GLES20.glGenBuffers(n, buffers, offset);
        GLES20Canvas.checkError();
    }

    @Override
    public void glDeleteTextures(GL11 gl, int n, int[] textures, int offset) {
        GLES20.glDeleteTextures(n, textures, offset);
        GLES20Canvas.checkError();
    }


    @Override
    public void glDeleteBuffers(GL11 gl, int n, int[] buffers, int offset) {
        GLES20.glDeleteBuffers(n, buffers, offset);
        GLES20Canvas.checkError();
    }

    @Override
    public void glDeleteFramebuffers(GL11ExtensionPack gl11ep, int n, int[] buffers, int offset) {
        GLES20.glDeleteFramebuffers(n, buffers, offset);
        GLES20Canvas.checkError();
    }
}
