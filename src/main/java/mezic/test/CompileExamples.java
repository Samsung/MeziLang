package mezic.test;

import mezic.compiler.CompileException;
import mezic.compiler.Compiler;
import mezic.compiler.Debug;
import mezic.parser.ParseException;
import mezic.util.PathUtil;

public class CompileExamples {
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
      PathUtil.clearFilesOnPath("path/example");

      runner.compile(new String[] {
    		  "example.e000_helloworld",
    		  "example.e001_plus_function",
          "example.e0011_cum_function",
          "example.e0012_function_name",
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

      ///*
      PathUtil.executeMain(new String[] {
    		  "example.Hello",
    		  "example.PlusFunction",
          "example.CummulateFunction",
          "example.FunctionName",
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
     //*/
     //PathUtil.executeMain(new String[] { "example.FunctionName" });

      
    } catch (CompileException | ParseException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
