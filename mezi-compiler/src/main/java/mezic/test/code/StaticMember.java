package mezic.test.code;

class Part1 {
  static int e;

  public Part1() {
    e = 100;
  }

  static void static_func(int a) {
    System.out.println("Static Function Call");
  }

}

class Part2 {
  Part1 d;

  public Part2() {
    d = new Part1();
  }
}

class Part3 {
  Part2 c;

  public Part3() {
    c = new Part2();
  }
}

public class StaticMember {

  @SuppressWarnings("static-access")
  public static void main(String[] args) {
    Part3 p = new Part3();
    // System.out.println(p.c.d.e);
    p.c.d.static_func(10);
  }

}
