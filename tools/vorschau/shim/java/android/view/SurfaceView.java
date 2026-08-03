package android.view;
import android.content.Context;
public class SurfaceView extends View {
    private final SurfaceHolder holder=new SurfaceHolder();
    public SurfaceView(Context c){ super(c); }
    public SurfaceHolder getHolder(){ return holder; }
}
