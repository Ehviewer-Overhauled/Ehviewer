package androidx.recyclerview.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BridgeRecyclerView extends RecyclerView{
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
    public boolean scrollByInternal(int x, int y, MotionEvent ev, int type) {
        return super.scrollByInternal(x, y, ev, type);
    }

}