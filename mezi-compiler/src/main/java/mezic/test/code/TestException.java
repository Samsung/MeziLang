package mezic.test.code;

@SuppressWarnings("serial")
public class TestException extends Exception {

  public TestException() {
    this("dflt");

  }

  public TestException(String msg) {
    super(msg);
  }

  public String getmsg() {

    return super.getMessage();
  }

}
