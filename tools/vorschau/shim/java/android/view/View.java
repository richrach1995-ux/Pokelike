package android.view;
import android.content.Context;
public class View {
    private final Context ctx;
    private boolean focusable;
    public View(Context c){ ctx=c; }
    public Context getContext(){ return ctx; }
    public void setFocusable(boolean b){ focusable=b; }
    public boolean isFocusable(){ return focusable; }
    public boolean postDelayed(Runnable r,long ms){ return true; }
    public boolean onTouchEvent(MotionEvent e){ return false; }
}
