import com.sun.source.util.JavacTask;
import javax.tools.*;
import java.util.*;
public class ParseOnly {
 public static void main(String[] args) throws Exception {
   JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
   DiagnosticCollector<JavaFileObject> diags=new DiagnosticCollector<>();
   StandardJavaFileManager manager=compiler.getStandardFileManager(diags,null,null);
   Iterable<? extends JavaFileObject> files=manager.getJavaFileObjectsFromStrings(Arrays.asList(args));
   JavacTask t=(JavacTask)compiler.getTask(null,manager,diags,Arrays.asList("-proc:none"),null,files);
   int count=0;for(Object tree:t.parse())count++;
   for(Diagnostic<?> d:diags.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR)throw new Exception(d.toString());
   System.out.println("PARSE OK: "+count+" Java compilation units (syntax only, not Android compilation)");
 }
}
