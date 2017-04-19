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

  public static void print_dbg(Object msg) {
    if (enable_print_dbg) {
      System.out.print(msg);
    }
  }

  public static void println_dbg(Object msg) {
    print_dbg(msg + "\n");
  }

  public static void println_dbg() {
    print_dbg("\n");
  }

  public static void print_dbg(Object caller, Object msg) {
    if (enable_print_dbg) {
      System.out.print(msg);
    }
  }

  public static void println_dbg(Object caller, Object msg) {
    print_dbg(caller, msg + "\n");
  }

  public static void print_info(Object msg) {
    if (enable_print_info) {
      System.out.print(msg);
    }
  }

  public static void println_info(Object msg) {
    print_info(msg + "\n");
  }

  public static void println_info() {
    print_info("\n");
  }

  public static void print_info(Object caller, Object msg) {
    if (enable_print_info) {
      System.out.print(msg);
    }
  }

  public static void println_info(Object caller, Object msg) {
    print_info(caller, msg + "\n");
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
