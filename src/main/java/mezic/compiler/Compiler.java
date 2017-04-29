package mezic.compiler;

import java.io.PrintStream;
import java.util.LinkedList;

import mezic.parser.ParseException;

import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compiler extends ClassLoader {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(Compiler.class);

  public final static int java_version = Opcodes.V1_8;
  private Debug debug;
  private PrintStream print_out;

  String source_base = null;
  String target_base = null;
  String[] class_pathes = null;
  String[] dflt_import_pkgs = null;

  @SuppressWarnings("unused")
  private Object exitObj = null;

  private int next_thread_id = 0;

  private LinkedList<CompileThread> vthread_list;

  public Compiler(PrintStream print_out, String source_base, String target_base, String[] class_pathes,
      String[] dflt_import_pkgs) {
    debug = new Debug();

    this.print_out = print_out;
    this.source_base = source_base;
    this.target_base = target_base;
    this.class_pathes = class_pathes;
    this.dflt_import_pkgs = dflt_import_pkgs;
    vthread_list = new LinkedList<CompileThread>();
  }

  public Object compile(String tu_name) throws CompileException, ParseException {
    compile(new String[] { tu_name });
    return null;
  }

  public Object compile(String[] tu_names) throws CompileException, ParseException {
    exitObj = null;
    next_thread_id = 0;

    CompileThread.allocCthread(this,
        new CompilerLoader(source_base, target_base, class_pathes, dflt_import_pkgs), tu_names);

    return null;
  }

  public void exit(Object exitObj, CompileThread caller_thread) {
    interruptChildVthread(caller_thread.getThreadId());

    this.exitObj = exitObj;

  }

  public int registerVthread(CompileThread vthread) throws CompileException {
    if (vthread == null) {
      throw new CompileException("registering invalid thread");
    }

    vthread_list.add(vthread);

    return (next_thread_id++);
  }

  public void interruptChildVthread() {
    interruptChildVthread(-1);
  }

  public void interruptChildVthread(int caller_thread_id) {
    int size = vthread_list.size();

    CompileThread vthread;

    for (int i = 0; i < size; i++) {
      vthread = vthread_list.get(i);

      if (vthread != null && vthread.getThreadId() != caller_thread_id) {
        // System.out.println("thread-"+i+" was interrupted");
        vthread.interrupt();
      }
    }

  }

  public void joinChildVthreads(int caller_thread_id) {
    int size = vthread_list.size();

    int tmp_size = size;

    CompileThread vthread;

    for (int i = 0; i < size; i++) {
      vthread = vthread_list.get(i);

      if (vthread != null && vthread.getThreadId() != caller_thread_id) {

        try {
          vthread.join();
          System.out.println("\nVthread(" + i + ") was joined");

          if (vthread.getInterpreterException() != null) {
            vthread.getInterpreterException().printStackTrace();
          }

          // While joining, another thread can be added, so re-count(Is this a
          // correct method ..? )
          tmp_size = vthread_list.size();

          if (size != tmp_size) {
            size = tmp_size;
          }

        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      }

    }
  }

  public Debug getDebug() {
    return this.debug;
  }

  public PrintStream getPrintOut() {
    return this.print_out;
  }

  public static class Builder {

    private PrintStream printout = System.out;
    private String sourcebase = "./";
    private String targetbase = "./";
    private String[] classpathes = new String[] { "./" };
    private String[] dfltImportpkgs = new String[] { "java/lang" };

    public Builder setPrintout(PrintStream printout) {
      this.printout = printout;
      return this;
    }

    public Builder setSourcebase(String sourcebase) {
      this.sourcebase = sourcebase;
      return this;
    }

    public Builder setTargetbase(String targetbase) {
      this.targetbase = targetbase;
      return this;
    }

    public Builder setClasspathes(String[] classpathes) {
      this.classpathes = classpathes;
      return this;
    }

    public Builder setDfltImportpkgs(String[] dfltImportpkgs) {
      this.dfltImportpkgs = dfltImportpkgs;
      return this;
    }

    public Compiler build() {
      return new Compiler(printout, sourcebase, targetbase, classpathes, dfltImportpkgs);
    }
  }

}
