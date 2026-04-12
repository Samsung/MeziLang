package mezic.test;

import mezic.compiler.CompileException;
import mezic.compiler.Compiler;
import mezic.compiler.Debug;
import mezic.parser.ParseException;
import mezic.util.PathUtil;

public class CompileApsExamples {
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
    		  "aps.aps010_bitoperation",
    		  "aps.aps020_selection_sort",
    		  "aps.aps030_factorial",
    		  "aps.aps031_permutation",
    		  "aps.aps032_combination",
    		  "aps.aps033_fibonachi",
    		  "aps.aps034_fibonachi_dp",
    		  "aps.aps035_knapsack",
    		  "aps.aps036_lcs",
    		  "aps.aps039_recursive_pow",
    		  "aps.aps040_mergesort",
    		  "aps.aps041_quicksort",
    		  "aps.aps050_graph_dfs",
    		  "aps.aps051_dijkstra",
              });

      PathUtil.executeMain(new String[] {
    		  "aps.ApsBitPrint",
    		  "aps.ApsSelectionSort",
    		  "aps.ApsFactorial",
    		  "aps.ApsPermutation",
    		  "aps.ApsCombination",
    		  "aps.ApsFibonachi",
    		  "aps.ApsFibonachiDp",
    		  "aps.ApsKnapsack",
    		  "aps.AbsLcs",
    		  "aps.ApsRecursivePower",
    		  "aps.ApsMergeSort",
    		  "aps.ApsQuickSort",
    		  "aps.AbsGraphDfs",
    		  "aps.ApsDijkstra",
              });

    } catch (CompileException | ParseException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
