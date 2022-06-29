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

package com.hippo.glview.view;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Gravity {

    public static final int NO_GRAVITY = 0x0000;
    public static final int LEFT = 0x0001;
    public static final int TOP = 0x0002;
    public static final int RIGHT = 0x0004;
    public static final int BOTTOM = 0x0008;
    public static final int CENTER_HORIZONTAL = LEFT | RIGHT;
    public static final int HORIZONTAL_MASK = CENTER_HORIZONTAL;
    public static final int CENTER_VERTICAL = TOP | BOTTOM;
    public static final int CENTER = CENTER_VERTICAL | CENTER_HORIZONTAL;
    public static final int VERTICAL_MASK = CENTER_VERTICAL;
    public static final int POSITION_BEGIN = 0;
    public static final int POSITION_CENTER = 1;
    public static final int POSITION_FINISH = 2;
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public static boolean centerHorizontal(int gravity) {
        return (gravity & HORIZONTAL_MASK) == CENTER_HORIZONTAL;
    }

    public static boolean right(int gravity) {
        return (gravity & HORIZONTAL_MASK) == RIGHT;
    }

    public static boolean left(int gravity) {
        return (gravity & HORIZONTAL_MASK) == LEFT;
    }

    public static boolean centerVertical(int gravity) {
        return (gravity & VERTICAL_MASK) == CENTER_VERTICAL;
    }

    public static boolean top(int gravity) {
        return (gravity & VERTICAL_MASK) == TOP;
    }

    public static boolean bottom(int gravity) {
        return (gravity & VERTICAL_MASK) == BOTTOM;
    }

    public static @PositionMode
    int getPosition(int gravity, @DirectionMode int direction) {
        if (direction == HORIZONTAL) {
            if (right(gravity)) {
                return POSITION_FINISH;
            } else if (centerHorizontal(gravity)) {
                return POSITION_CENTER;
            } else {
                return POSITION_BEGIN;
            }
        } else {
            if (bottom(gravity)) {
                return POSITION_FINISH;
            } else if (centerVertical(gravity)) {
                return POSITION_CENTER;
            } else {
                return POSITION_BEGIN;
            }
        }
    }

    @IntDef({POSITION_BEGIN, POSITION_CENTER, POSITION_FINISH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PositionMode {
    }

    @IntDef({HORIZONTAL, VERTICAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DirectionMode {
    }
}
