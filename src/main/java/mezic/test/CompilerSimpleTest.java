package mezic.test;

import mezic.compiler.CompileException;
import mezic.compiler.Compiler;
import mezic.compiler.Debug;
import mezic.parser.ParseException;
import mezic.util.PathUtil;

public class CompilerSimpleTest {
  public static void main(String[] args) {

	String[] runtimeClassPaths = PathUtil.getJavaRuntimeClassPaths();
	if (runtimeClassPaths.length == 0) {
		System.out.println("It cannot find Java runtime class path");
		return;
	}

    String[] compilerClassPaths = new String[runtimeClassPaths.length + 2];
    compilerClassPaths[0] = "path/";
    System.arraycopy(runtimeClassPaths, 0, compilerClassPaths, 1, runtimeClassPaths.length);
    compilerClassPaths[compilerClassPaths.length - 1] = "target/classes/";

    Compiler runner = new Compiler.Builder()
    		.setPrintout(System.out)
    		.setSourcebase("code_base/")
    		.setTargetbase("path/")
            .setClasspathes(compilerClassPaths)
            .setDfltImportpkgs(new String[] {
            		"java/lang",
            		}).build();

    try {
      Debug.enable(false);
      PathUtil.clearFilesOnPath("path/compilertest/grammar/tmp");

      runner.compile(new String[] {
              "compilertest.grammar.tmp.TemporalTest",
              });

      PathUtil.executeMain(new String[] {
              "compilertest.grammar.tmp.TemporalTest",
              });

    } catch (CompileException | ParseException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }



}
