package mezic.test.code;

public class ArrayCpy {

  public static void main(String[] args) {
    String[] a = { "0", "1", "2" };
    dump(a);
    del(a, 2);
    // dump(a);

  }

  public static void del(String[] children, int i) {
    if (children == null) {
      System.out.println("error");
      return;
    }

    if (i < 0 || i >= children.length) {
      System.out.println("error");
      return;
    }

    if (children.length == 1) {
      System.out.println("array is removed");
      return;
    }

    String[] c = new String[children.length - 1];

    System.arraycopy(children, 0, c, 0, i);

    if (children.length - i - 1 > 0) {
      System.arraycopy(children, i + 1, c, i, children.length - i - 1);
    }

    children = c;

    dump(children);

  }

  public static void dump(String[] arr) {
    System.out.println("dump");
    for (int i = 0; i < arr.length; i++) {
      System.out.println(i + ")" + arr[i]);
    }
  }

}
