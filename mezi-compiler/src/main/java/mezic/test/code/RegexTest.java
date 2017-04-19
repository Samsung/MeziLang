package mezic.test.code;

public class RegexTest {

  public static void main(String[] args) {

    /* http://www.vogella.com/tutorials/JavaRegularExpressions/article.html */

    /* white space splitting */
    // split_test("Hello World\tGood after\nnoon", "\\s");
    // // or
    // split_test("Hello World\tGood after\nnoon", "[ \\t\\n\\x0b\\r\\f]");
    //
    // /* specific character splitting */
    // split_test("Hello World\tGood after\nnoon", "ft");
    //
    // /* new line splitting */
    // split_test("Hello World\nGood after\nnoon", "\n");
    //
    // /* new line splitting(\r\n) */
    // split_test("Hello World\r\nGood after\nnoon", "\r\n");
    //
    // /* new line splitting(\r or \n) */
    // split_test("Hello World\r\nGood after\nnoon", "[\\r\\n]");
    //
    // /* back slash character splitting('\') */
    // split_test("Hello World\\Good after\nnoon", "\\\\");
    //
    // /* special character splitting('\','n') */
    // split_test("Hello World\\nGood after\nnoon", "\\\\n");
    //
    // /* any digit splitting('\','n') */
    // split_test("Hello Wo9rld\\nG1ood after\nnoon", "[0-9]");
    //
    // /* non word character splitting('^' '%') */
    // split_test("Hello Wo9rld\\nG1o^od af&ter\nn#oon", "[^\\w]");

    // /* quantifier splitting */
    // split_test("Hello Wo9rld\\nG1o^od af&ter\nn#oon", "l+");
    // // or
    // split_test("Hello Wo9rld\\nG1o^od af&ter\nn#oon", "l{1,}");
    //
    // /* split with "1+" */
    split_test("Hello Wo9rl+d\\nG1o^od	af&ter\nn#oon", "l\\+");

  }

  public static void split_test(String str, String regex) {
    String[] tokens = str.split(regex);

    System.out.println("\nOriginal Text---------------------------");
    System.out.println(str);
    System.out.println("Regular Expression......................");
    System.out.println(regex);
    System.out.println("----------------------------------------\n");

    for (int i = 0; i < tokens.length; i++) {
      System.out.println("(" + i + ") [" + tokens[i] + "]");

    }
    System.out.println("");
  }

}
