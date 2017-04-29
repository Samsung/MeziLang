package mezic.test;

import mezic.compiler.CompileException;
import mezic.compiler.Compiler;
import mezic.compiler.Debug;
import mezic.parser.ParseException;
import mezic.util.PathUtil;

public class CompilerTest {
  public static void main(String[] args) {

	String rtPath = PathUtil.getJavaRtPath();
	if (rtPath == null) {
		System.out.println("It cannot find path of rt.jar");
		return;
	}
	  
    Compiler runner = new Compiler.Builder()
    		.setPrintout(System.out)
    		.setSourcebase("code_base/")
    		.setTargetbase("path/")
            .setClasspathes(new String[] {
            		"path/",
            		rtPath,
            		"target/classes/",
            		})
            .setDfltImportpkgs(new String[] {
            		"java/lang",
            		}).build();

    try {
      Debug.enable(false);
      PathUtil.clearFilesOnPath("path/compilertest/grammar");
      PathUtil.clearFilesOnPath("path/compilertest/grammar/tmp");

      runner.compile(new String[] {
    		  "compilertest.grammar.BasicGrammarTest",
    		  "compilertest.grammar.StreamGrammarTest",
              "compilertest.grammar.tmp.TemporalTest",
              });

      PathUtil.executeMain(new String[] {
    		  "compilertest.grammar.BasicGrammarTest",
    		  "compilertest.grammar.StreamGrammarTest",
              "compilertest.grammar.tmp.TemporalTest",
              });

    } catch (CompileException | ParseException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }



}
