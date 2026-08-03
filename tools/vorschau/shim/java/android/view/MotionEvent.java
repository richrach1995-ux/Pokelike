package android.view;
import java.util.ArrayList;
import java.util.List;
public class MotionEvent {
    public static final int ACTION_DOWN=0, ACTION_UP=1, ACTION_MOVE=2, ACTION_CANCEL=3,
                            ACTION_POINTER_DOWN=5, ACTION_POINTER_UP=6;
    private int action; private int actionIndex;
    private final List<float[]> points=new ArrayList<>();
    public static MotionEvent obtain(int action,float x,float y){
        MotionEvent e=new MotionEvent(); e.action=action; e.points.add(new float[]{x,y}); return e;
    }
    public int getActionMasked(){ return action; }
    public int getActionIndex(){ return actionIndex; }
    public int getPointerCount(){ return points.size(); }
    public float getX(int i){ return points.get(i)[0]; }
    public float getY(int i){ return points.get(i)[1]; }
}
