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

#ifndef IMAGE_IMAGE_UTILS_H
#define IMAGE_IMAGE_UTILS_H

#include <stdbool.h>

bool copy_pixels(const void *src, int src_w, int src_h, int src_x, int src_y,
                 void *dst, int dst_w, int dst_h, int dst_x, int dst_y,
                 int width, int height);

#define STRINGIZE(L) #L
#define MAKESTRING(M, L) M(L)

#define MIN(a, b) (((a) < (b)) ? (a) : (b))
#define MAX(a, b) (((a) > (b)) ? (a) : (b))

#endif //IMAGE_IMAGE_UTILS_H
