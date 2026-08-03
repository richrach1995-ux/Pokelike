package android.graphics;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import javax.imageio.ImageIO;
public class Bitmap {
    public enum Config { ARGB_8888, RGB_565 }
    public enum CompressFormat { PNG, JPEG }
    public final BufferedImage img;
    private Bitmap(int w,int h){ img=new BufferedImage(Math.max(1,w),Math.max(1,h),BufferedImage.TYPE_INT_ARGB); }
    public static Bitmap createBitmap(int w,int h,Config c){ return new Bitmap(w,h); }
    public int getWidth(){return img.getWidth();}
    public int getHeight(){return img.getHeight();}
    public void setPixel(int x,int y,int argb){ img.setRGB(x,y,argb); }
    public int getPixel(int x,int y){ return img.getRGB(x,y); }
    public boolean compress(CompressFormat f,int q,OutputStream out){
        try { return ImageIO.write(img, f==CompressFormat.PNG?"png":"jpg", out); }
        catch(Exception e){ return false; }
    }
}
