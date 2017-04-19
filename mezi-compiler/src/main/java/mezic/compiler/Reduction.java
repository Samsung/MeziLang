package mezic.compiler;

abstract public class Reduction implements java.io.Serializable {

  private static final long serialVersionUID = -5416006906289794276L;

  public final static int CONTAINER = 0;
  public final static int INVOKE_INFO = 1;
  public final static int OPERATION = 2;
  public final static int IDENTIFIER = 3;
  public final static int TYPE = 4;
  public final static int CONTROL = 5;

  public Reduction() {
  }

  public boolean isContainer() {
    return (reduceType() == CONTAINER);
  }

  public boolean isInvokeInfo() {
    return (reduceType() == INVOKE_INFO);
  }

  public boolean isOperation() {
    return (reduceType() == OPERATION);
  }

  public boolean isIdentifier() {
    return (reduceType() == IDENTIFIER);
  }

  public boolean isType() {
    return (reduceType() == TYPE);
  }

  public boolean isControl() {
    return (reduceType() == CONTROL);
  }

  abstract public int reduceType();

}
