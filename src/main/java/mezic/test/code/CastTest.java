package mezic.test.code;

abstract class Sum {

  int sum(int a, int b) {
    return (a > b) ? 0 : (oper(a) + sum(a + 1, b));
  }

  abstract int oper(int a);
}

class Ints extends Sum {
  int oper(int a) {
    return a;
  }
}

class Squares extends Sum {
  int oper(int a) {
    return a * a;
  }
}

public class CastTest {

  @SuppressWarnings("unused")
  public static void main(String[] args) {

    System.out.println(new Ints().sum(1, 10));
    System.out.println(new Squares().sum(1, 10));

    Integer[] Ia = new Integer[] { 1, 2, 3, 4 };

  }

}
