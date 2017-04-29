package mezic.compiler;

import mezic.parser.LangUnitNode;

public class CompileException extends Exception {

  private static final long serialVersionUID = -4699727666689161996L;

  LangUnitNode node = null;
  Exception targetException = null;
  Object exitRet = null;
  String stackTraceStr = null;
  int thread_id = -1;
  String tu_name = null;

  public CompileException(String msg) {
    super(msg);
    node = null;
    stackTraceStr = null;
  }

  public CompileException(String msg, LangUnitNode node) {
    super(msg);
    this.node = node;
    stackTraceStr = null;
  }

  public boolean node_initialized() {
    return (node != null);
    /*
    if (node == null) {
      return false;
    }
    else {
      return true;
    }
    */
  }

  public void set_node(LangUnitNode node) {
    this.node = node;
  }

  public LangUnitNode get_node() {
    return this.node;
  }

  public void set_tu_name(String tu_name) {
    this.tu_name = tu_name;
  }

  public String get_tu_name() {
    return this.tu_name;
  }

  public boolean targetExceptionInitlialized() {
    return (this.targetException != null);
    /*
    if (this.taretException != null) {
      return true;
    } else {
      return false;
    }
    */
  }

  public Exception getTargetException() {
    return this.targetException;
  }

  public void setTargetException(Exception targetException) {
    this.targetException = targetException;
  }

  public boolean hasExitReturnObj() {
    return (this.exitRet != null);
    /*
    if (this.exitRet != null) {
      return true;
    } else {
      return false;
    }
    */
  }

  public Object getExitReturn() {
    return this.exitRet;
  }

  public void setExitReturn(Object exitRetObject) {
    this.exitRet = exitRetObject;
  }

  public void setThreadId(int id) {
    this.thread_id = id;
  }

  public int getThreadId() {
    return this.thread_id;
  }

  @Override
  public String getMessage() {
    String msg = super.getMessage();

    if (node_initialized()) {
      msg += " in " + node.toString();
    }

    if (tu_name != null) {
      msg += (" in tu(" + tu_name + ")");
    }

    if (thread_id != -1) {
      msg += " in Vthread(" + thread_id + ")";
    }

    if (stackTraceStr != null) {
      msg += ("\n" + stackTraceStr);
    }

    if (targetException != null) {
      msg += targetException;
    }

    return msg;
  }

  public String getRecursiveTargetExcpMessage(int level) {
    String msg = "";

    if (level < 30 && targetExceptionInitlialized()) {
      // System.out.println("TargetException is initialized");
      Exception tgtExcp = getTargetException();

      System.err.println("Target Exception Level(" + level + ")");
      tgtExcp.printStackTrace();

      msg += (tgtExcp.getClass() + ": " + tgtExcp.getMessage() + "\n");

      if (tgtExcp.getClass().equals(CompileException.class)) {
        CompileException re = (CompileException) tgtExcp;

        msg += re.getRecursiveTargetExcpMessage(level + 1);
      }
    }

    return msg;
  }

  public void setStackTraceStr(String stack_trace) {
    this.stackTraceStr = stack_trace;
  }

}
