package mezic.test;

import mezic.compiler.CompileException;
import mezic.compiler.Compiler;
import mezic.compiler.Debug;
import mezic.parser.ParseException;
import mezic.util.PathUtil;

public class CompileHelloworld {
  public static void main(String[] args) {

    Compiler runner = new Compiler.Builder()
    		.setPrintout(System.out)
    		.setSourcebase("code_base/")
    		.setTargetbase("path/")
            .setClasspathes(new String[] {
            		"path/",
            		"C:/Program Files/Java/jdk1.8.0_102/jre/lib/rt.jar",
            		"target/classes/",
            		})
            .setDfltImportpkgs(new String[] {
            		"java/lang",
            		}).build();

    try {
      Debug.enable(false);
      PathUtil.clearFilesOnPath("path/example");

      runner.compile(new String[] {
    		  "example.e000_helloworld",
              });

      PathUtil.executeMain(new String[] {
    		  "example.Hello",
              });

    } catch (CompileException | ParseException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
