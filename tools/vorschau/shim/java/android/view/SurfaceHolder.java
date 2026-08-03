package android.view;
public class SurfaceHolder {
    public interface Callback {
        void surfaceCreated(SurfaceHolder holder);
        void surfaceChanged(SurfaceHolder holder,int format,int width,int height);
        void surfaceDestroyed(SurfaceHolder holder);
    }
    private final Surface surface=new Surface();
    public void addCallback(Callback c){}
    public Surface getSurface(){ return surface; }
    public android.graphics.Canvas lockCanvas(){ return null; }
    public void unlockCanvasAndPost(android.graphics.Canvas c){}
}
