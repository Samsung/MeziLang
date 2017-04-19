package mezic.test.code;

class SubSub {
  int c;

  public SubSub() {
    c = 100;
  }
}

class Sub {
  SubSub b;

  public Sub() {
    b = new SubSub();
  }
}

public class PrimaryTest {

  @SuppressWarnings("unused")
  public static void main(String[] args) {
    int sum = 0;
    for (int i = 0; i < 15; i++) {
      sum = sum + i;
    }

    System.out.println(sum);

    Sub a = new Sub();
    Sub d = null;

    (d) = (a);

    (((a.b.c))) = 10;

    System.out.println((a.b.c));

    sum = 0;

    for (int i = 0; i < 100; i = i + 1) {
      sum = sum + i;
    }

    System.out.println(sum);
  }

}
