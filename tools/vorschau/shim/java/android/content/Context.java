package android.content;
import java.io.File;
public class Context {
    private final File dir;
    public Context(){ dir=new File(System.getProperty("java.io.tmpdir"),"pokelike-shim"); dir.mkdirs(); }
    public File getFilesDir(){ return dir; }
}
