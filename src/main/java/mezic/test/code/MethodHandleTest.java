package mezic.test.code;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

//import unit_test.grammar.part.PartTest_test_FuncIfImpl_0;

public class MethodHandleTest {

  public static void main(String[] args) throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    // mt is (char,char)String
    MethodType mt = MethodType.methodType(void.class, Object.class);
    MethodHandle mh = lookup.findVirtual(MethodHandleTest.class, "print", mt);
    mh = mh.bindTo(new MethodHandleTest());

    mh.invoke("Hello World");

    /*
     * Consumer cs = new PartTest_test_FuncIfImpl_0(mh);
     *
     * ArrayList list = new ArrayList(); list.add("Hello1"); list.add("Hello2");
     *
     * list.stream().forEach(cs);
     */

  }

  public void print(Object obj) {
    System.out.println(obj);
  }

}
