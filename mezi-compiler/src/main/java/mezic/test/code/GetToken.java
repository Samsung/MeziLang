package mezic.test.code;

import mezic.util.Util;

public class GetToken {

  /**
   * @param args
   */
  public static void main(String[] args) {
    String a = "excel::SheetA:C2:ScriptC2";

    String[] t = Util.getTokens(a, ":");

    for (int i = 0; i < t.length; i++) {
      System.out.println("(" + i + ") " + t[i]);
    }

  }

}
