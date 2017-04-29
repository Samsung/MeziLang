package mezic.test.code;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ExceptionTest {

  public static void main(String[] args) {
    // TODO Auto-generated method stub

  }

  public void testa(boolean a) throws Exception {
    if (a) {
      throw new Exception();
    }
  }

  @SuppressWarnings({ "unused", "hiding" })
  public void test() {
    int a = 0;
    FileInputStream fis = null;

    try {
      int b = 0;

      fis = new FileInputStream("./path");
      a = 1;
    } catch (FileNotFoundException e) {
      // e.getMessage()
    } catch (IOException e) {
      a = 2;
    } catch (Exception e) {

    } finally {
      a = 3;
      System.out.println("Finally");
    }

  }

}
