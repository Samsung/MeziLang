package mezic.compiler;

import mezic.parser.LangUnitNode;

public class Debug {

  public static boolean enable_print_dbg = true;
  public static boolean enable_print_info = true;
  public static boolean enable_print_closure = true;

  public static void enable(boolean is_enable) {
    enable_print_dbg = is_enable;
    enable_print_info = is_enable;
  }

  public static boolean enable_compile_debug_print = false;

  public static void stop() throws CompileException {
    // throw new CompileException("Stop !!");
    stop("");
  }

  public static void stop(Object msg) throws CompileException {
    throw new CompileException("Stop !!" + msg);
  }

  public static void assertion(boolean condition, String statement) throws CompileException {
    if (!condition) {
      throw new CompileException(statement);
    }
  }

  public static void assertion(boolean condition, String statement, LangUnitNode node) throws CompileException {
    if (!condition) {
      throw new CompileException(statement, node);
    }
  }

}
