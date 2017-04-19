package mezic.test.code;

import mezic.util.TxtUtil;

public class EscapeSeqAlternationTest {

  /**
   * @param args
   */
  public static void main(String[] args) {

    alt_test(new char[] { 'n', 'A', 'A', 'B' });
    alt_test(new char[] { '\\', 'A', 'A', 'B' });
    alt_test(new char[] { '\\', 'n', 'A', 'A', 'B' });
    alt_test(new char[] { '\\', '\\', 'n', 'A', 'A', 'B' });
    alt_test(new char[] { '\\', '\\', '\\', 'n', 'A', 'A', 'B' });
    // System.out.println("\\\nAAB");
    alt_test(new char[] { 'A', 'A', '\\', 'n', 'B' });
    alt_test(new char[] { 'A', 'A', '\\', '\\', 'n', 'B' });
    alt_test(new char[] { 'A', 'A', '\\', '\\', '\\', 'n', 'B' });
    alt_test(new char[] { 'A', 'A', '\\', '\\', '\\', 'n', 'B', 'A', 'A', '\\', '\\', '\\', 'n', 'B' });

  }

  public static void alt_test(char[] arr) {
    String str = new String(arr);

    String alt = TxtUtil.alternateEscapeSequence(str, "\\n", '-');

    System.out.println("Orginal[" + str + "] Alternation[" + alt + "]");
  }

}
