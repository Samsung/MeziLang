package mezic.compiler;

/* Used to Control execution-flow */
public class ExecutionControl extends Reduction {

  /*
   * public final static int NEXT = 1; public final static int BREAK = 2; public
   * final static int CONTINUE = 3; public final static int RETURN = 4;
   *
   * private int cmd = 0;
   *
   * public ExecutionControl(int cmd) throws InterpreterException { setCmd(cmd);
   * }
   *
   *
   * public String toString() { String cmd; switch(getCmd()) { case NEXT: cmd =
   * "NEXT"; break; case BREAK: cmd = "BREAK"; break; case CONTINUE: cmd =
   * "CONTINUE"; break; case RETURN: cmd = "RETURN"; break; default: cmd =
   * "invalid"; } return "Control("+cmd+")"; }
   *
   *
   * public void setCmd(int cmd) throws InterpreterException { switch(cmd) {
   * case NEXT: case BREAK: case CONTINUE: case RETURN: break; default: throw
   * new InterpreterException("Setting invalid control("+cmd+")"); }
   *
   * this.cmd = cmd; }
   *
   *
   * public int getCmd() { return this.cmd; }
   *
   *
   * public boolean isCmd(int cmd) { if( this.cmd == cmd ) return true; else
   * return false; }
   *
   */

  private static final long serialVersionUID = 256147162825044402L;

  public int reduceType() {
    // return Reduction.EXECUTION_CONTROL;
    return -1;
  }
}
