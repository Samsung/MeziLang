package mezic.test;

import mezic.compiler.CompileException;
import mezic.compiler.Compiler;
import mezic.compiler.Debug;
import mezic.parser.ParseException;
import mezic.util.PathUtil;

public class CompileExamples {
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
    		  "example.e001_plus_function",
    		  "example.e002_closure_function",
    		  "example.e003_comparation_expr",
    		  "example.e004_array",
    		  "example.e005_list",
    		  "example.e006_loop",
    		  "example.e007_member_function",
    		  "example.e008_apply_function",
    		  "example.e009_currying",
    		  "example.e010_operator_overload",
              });

      PathUtil.executeMain(new String[] {
    		  "example.Hello",
    		  "example.PlusFunction",
    		  "example.Closure",
    		  "example.Compare",
    		  "example.Array",
    		  "example.List",
    		  "example.Loop",
    		  "example.MemberFunction",
    		  "example.ApplyFunc",
    		  "example.ExampleCurrying",
    		  "example.ExOperatorOverload",
              });

    } catch (CompileException | ParseException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
