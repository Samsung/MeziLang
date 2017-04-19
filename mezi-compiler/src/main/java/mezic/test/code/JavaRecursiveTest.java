package mezic.test.code;

public class JavaRecursiveTest {

  public static int recursive_func(int depth) {
    System.out.println("recursive_func(depth=" + depth + ")");

    if (depth < 1) {
      System.out.println("met the bottom");
      return 0;
    }

    return (recursive_func(--depth) + 1);
  }

  public static void main(String[] args) {
    System.out.println("ret = " + recursive_func(10000));

  }

}
