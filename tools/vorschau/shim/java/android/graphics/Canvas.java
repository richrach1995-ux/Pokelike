package android.graphics;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayDeque;
import java.util.Deque;
public class Canvas {
    private final Graphics2D g;
    private final Deque<AffineTransform> stack = new ArrayDeque<>();
    private final int w,h;
    public Canvas(Bitmap b){
        g=b.img.createGraphics();
        w=b.getWidth(); h=b.getHeight();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }
    private void apply(Paint p){
        int c=p.getColor();
        g.setColor(new java.awt.Color(Color2.r(c),Color2.g(c),Color2.b(c),Color2.a(c)));
        if(p.getStyle()==Paint.Style.STROKE) g.setStroke(new BasicStroke(Math.max(0.1f,p.getStrokeWidth())));
        else g.setStroke(new BasicStroke(1f));
    }
    private boolean stroke(Paint p){ return p.getStyle()==Paint.Style.STROKE; }
    public int getWidth(){return w;} public int getHeight(){return h;}
    public void drawColor(int color){
        g.setColor(new java.awt.Color(Color2.r(color),Color2.g(color),Color2.b(color),Color2.a(color)));
        g.fill(new Rectangle2D.Float(-100000,-100000,300000,300000));
    }
    public int save(){ stack.push(g.getTransform()); return stack.size(); }
    public void restore(){ if(!stack.isEmpty()) g.setTransform(stack.pop()); }
    public void translate(float x,float y){ g.translate(x,y); }
    public void scale(float sx,float sy){ g.scale(sx,sy); }
    public void drawRect(float l,float t,float r,float b,Paint p){
        apply(p); Shape s=new Rectangle2D.Float(Math.min(l,r),Math.min(t,b),Math.abs(r-l),Math.abs(b-t));
        if(stroke(p)) g.draw(s); else g.fill(s);
    }
    public void drawRoundRect(RectF rc,float rx,float ry,Paint p){
        apply(p); Shape s=new RoundRectangle2D.Float(rc.left,rc.top,rc.right-rc.left,rc.bottom-rc.top,rx*2,ry*2);
        if(stroke(p)) g.draw(s); else g.fill(s);
    }
    public void drawCircle(float cx,float cy,float r,Paint p){
        apply(p); Shape s=new Ellipse2D.Float(cx-r,cy-r,r*2,r*2);
        if(stroke(p)) g.draw(s); else g.fill(s);
    }
    public void drawOval(RectF rc,Paint p){
        apply(p); Shape s=new Ellipse2D.Float(rc.left,rc.top,rc.right-rc.left,rc.bottom-rc.top);
        if(stroke(p)) g.draw(s); else g.fill(s);
    }
    public void drawPath(Path path,Paint p){
        apply(p); if(stroke(p)) g.draw(path.p); else g.fill(path.p);
    }
    public void drawText(String text,float x,float y,Paint p){
        apply(p); g.setFont(p.awtFont()); g.drawString(text,x,y);
    }
    public void drawBitmap(Bitmap b,Rect src,RectF dst,Paint p){
        Composite old=g.getComposite();
        if(p!=null && p.getAlpha()<255) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,p.getAlpha()/255f));
        int sl=src==null?0:src.left, st=src==null?0:src.top;
        int sr=src==null?b.getWidth():src.right, sb=src==null?b.getHeight():src.bottom;
        g.drawImage(b.img, Math.round(dst.left),Math.round(dst.top),Math.round(dst.right),Math.round(dst.bottom),
                    sl,st,sr,sb,null);
        g.setComposite(old);
    }
    static class Color2 {
        static int a(int c){return (c>>>24)&255;} static int r(int c){return (c>>16)&255;}
        static int g(int c){return (c>>8)&255;} static int b(int c){return c&255;}
    }
}
