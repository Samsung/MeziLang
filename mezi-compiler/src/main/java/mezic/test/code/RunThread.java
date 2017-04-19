package mezic.test.code;

public class RunThread extends Thread {

  public void run() {
    System.out.println("Hello World");

    System.out.println("Thread");

    int sum = 0;

    for (int i = 0; i < 10000; i++) {
      sum += i;
    }

    System.out.println("[" + this.getName() + "]" + " sum=" + sum);

  }

}
