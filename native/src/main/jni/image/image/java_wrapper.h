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
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * EhViewer. If not, see <https://www.gnu.org/licenses/>.
 */

//
// Created by Hippo on 12/28/2015.
//

#ifndef IMAGE_JAVA_WRAPPER_H
#define IMAGE_JAVA_WRAPPER_H

#include <jni.h>
#include <stdbool.h>

#define IMAGE_TILE_MAX_SIZE (512 * 512)

bool image_onLoad(JavaVM *vm);

JNIEnv *obtain_env(bool *attach);

void release_env();

#endif // IMAGE_JAVA_WRAPPER_H
