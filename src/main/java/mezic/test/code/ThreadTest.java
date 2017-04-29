package mezic.test.code;

public class ThreadTest {

  public static void main(String[] args) {

    RunThread th = new RunThread();
    th.start();

    try {
      th.join();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
