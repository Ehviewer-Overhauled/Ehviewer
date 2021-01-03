package androidx.recyclerview.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BridgeRecyclerView extends RecyclerView {
    public BridgeRecyclerView(@NonNull Context context) {
        super(context);
    }

    public BridgeRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BridgeRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    void scrollStep(int dx, int dy, @Nullable int[] consumed) {
        try {
            super.scrollStep(dx, dy, consumed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean scrollByInternal(int x, int y, MotionEvent ev, int type) {
        return super.scrollByInternal(x, y, ev, type);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        try {
            super.onLayout(changed, l, t, r, b);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}