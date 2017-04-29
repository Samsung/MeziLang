package mezic.test.code;

class Parent {

}

class Child extends Parent {

}

public class InstanceOfTest {

  public static void main(String[] args) {

    System.out.println(new Child() instanceof Parent);
    System.out.println(new Child() instanceof Child);

  }

}
