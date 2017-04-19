package mezic.test.code;

public class InnerTest {

  public static void main(String[] args) {
    new A().new B().b = 10;

  }

  static class A {

    int a;

    class B {

      int b;

    }

  }

}
