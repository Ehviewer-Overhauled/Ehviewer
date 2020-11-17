/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.easyrecyclerview;

import android.content.Context;
import android.graphics.PointF;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.SimpleHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class LayoutManagerUtils {

    private static Method sCsdfp = null;

    static {
        try {
            sCsdfp = StaggeredGridLayoutManager.class.getDeclaredMethod("calculateScrollDirectionForPosition", int.class);
            sCsdfp.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // Ignore
            e.printStackTrace();
        }
    }

    public static void scrollToPositionWithOffset(
            RecyclerView.LayoutManager layoutManager, int position, int offset) {
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(position, offset);
        } else {
            throw new IllegalStateException("Can't do scrollToPositionWithOffset for " +
                    layoutManager.getClass().getName());
        }
    }

    public static void smoothScrollToPosition(
            RecyclerView.LayoutManager layoutManager, Context context, int position) {
        smoothScrollToPosition(layoutManager, context, position, -1);
    }

    public static void smoothScrollToPosition(
            RecyclerView.LayoutManager layoutManager, Context context, int position,
            int millisecondsPerInch) {
        SimpleSmoothScroller smoothScroller;
        if (layoutManager instanceof LinearLayoutManager) {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            smoothScroller = new SimpleSmoothScroller(context, millisecondsPerInch) {
                @Override
                public PointF computeScrollVectorForPosition(int targetPosition) {
                    return linearLayoutManager.computeScrollVectorForPosition(targetPosition);
                }
            };

        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            final StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
            smoothScroller = new SimpleSmoothScroller(context, millisecondsPerInch) {
                @Override
                public PointF computeScrollVectorForPosition(int targetPosition) {
                    int direction = 0;
                    try {
                        direction = (Integer) sCsdfp.invoke(staggeredGridLayoutManager, targetPosition);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    if (direction == 0) {
                        return null;
                    }
                    if (staggeredGridLayoutManager.getOrientation() == StaggeredGridLayoutManager.HORIZONTAL) {
                        return new PointF(direction, 0);
                    } else {
                        return new PointF(0, direction);
                    }
                }
            };

        } else {
            throw new IllegalStateException("Can't do smoothScrollToPosition for " +
                    layoutManager.getClass().getName());
        }
        smoothScroller.setTargetPosition(position);
        layoutManager.startSmoothScroll(smoothScroller);
    }

    public static void scrollToPositionProperly(final RecyclerView.LayoutManager layoutManager,
                                                final Context context, final int position, final OnScrollToPositionListener listener) {
        SimpleHandler.getInstance().postDelayed(new Runnable() {
            @Override
            public void run() {
                int first = getFirstVisibleItemPosition(layoutManager);
                int last = getLastVisibleItemPosition(layoutManager);
                int offset = Math.abs(position - first);
                int max = last - first;
                if (offset < max && max > 0) {
                    smoothScrollToPosition(layoutManager, context, position,
                            MathUtils.lerp(100, 25, (offset / max)));
                } else {
                    scrollToPositionWithOffset(layoutManager, position, 0);
                    if (listener != null) {
                        listener.onScrollToPosition(position);
                    }
                }
            }
        }, 200);
    }

    public static int getFirstVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] positions = ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(null);
            return MathUtils.min(positions);
        } else {
            throw new IllegalStateException("Can't do getFirstVisibleItemPosition for " +
                    layoutManager.getClass().getName());
        }
    }

    public static int getLastVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] positions = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
            return MathUtils.max(positions);
        } else {
            throw new IllegalStateException("Can't do getLastVisibleItemPosition for " +
                    layoutManager.getClass().getName());
        }
    }

    public interface OnScrollToPositionListener {
        void onScrollToPosition(int position);
    }
}
