package android.graphics;
import java.awt.Font;
public class Paint {
    public enum Style { FILL, STROKE, FILL_AND_STROKE }
    public static final int ANTI_ALIAS_FLAG = 1;
    private int color=0xFF000000;
    private Style style=Style.FILL;
    private float strokeWidth=1f;
    private float textSize=12f;
    private Typeface typeface=Typeface.MONOSPACE;
    private boolean antiAlias=true, filterBitmap=true;
    public Paint(){}
    public Paint(int flags){ antiAlias=(flags&ANTI_ALIAS_FLAG)!=0; }
    public int getColor(){return color;} public void setColor(int c){color=c;}
    public Style getStyle(){return style;} public void setStyle(Style s){style=s;}
    public float getStrokeWidth(){return strokeWidth;} public void setStrokeWidth(float w){strokeWidth=w;}
    public float getTextSize(){return textSize;} public void setTextSize(float s){textSize=s;}
    public Typeface getTypeface(){return typeface;} public void setTypeface(Typeface t){typeface=t;}
    public boolean isAntiAlias(){return antiAlias;} public void setAntiAlias(boolean v){antiAlias=v;}
    public boolean isFilterBitmap(){return filterBitmap;} public void setFilterBitmap(boolean v){filterBitmap=v;}
    public int getAlpha(){return (color>>>24)&255;}
    public void setAlpha(int a){ color=(color&0x00FFFFFF)|((a&255)<<24); }
    public Font awtFont(){
        int st=(typeface!=null && typeface.style==Typeface.BOLD)?Font.BOLD:Font.PLAIN;
        return new Font(typeface==null?"Monospaced":typeface.family, st, Math.round(textSize));
    }
    public float measureText(String s){
        java.awt.image.BufferedImage img=new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g=img.createGraphics();
        g.setFont(awtFont());
        float w=g.getFontMetrics().stringWidth(s);
        g.dispose();
        return w;
    }
}
