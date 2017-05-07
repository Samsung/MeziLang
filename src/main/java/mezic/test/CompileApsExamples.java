package mezic.test;

import mezic.compiler.CompileException;
import mezic.compiler.Compiler;
import mezic.compiler.Debug;
import mezic.parser.ParseException;
import mezic.util.PathUtil;

public class CompileApsExamples {
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
