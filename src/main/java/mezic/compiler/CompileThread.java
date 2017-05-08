package mezic.compiler;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mezic.parser.ASTTranslationUnit;
import mezic.parser.ParseException;

public class CompileThread extends Thread {
	
  private static final Logger LOG = LoggerFactory.getLogger(CompileThread.class);

  private int thread_id = -1;
  private ASTTraverseVisitor visitor;

  private Container retContainer;
  private CompileException interpreteExcp = null;
  @SuppressWarnings("unused")
  private boolean deserialized_thread;

  public CompileThread(ASTTraverseVisitor visitor, boolean deserialized_thread) {
    this.visitor = visitor;
    this.deserialized_thread = deserialized_thread;
  }

  public int getThreadId() {
    return thread_id;
  }

  public void setThreadId(int thread_id) {
    this.thread_id = thread_id;
  }

  public Container getReturnContainer() {
    return this.retContainer;
  }

  public CompileException getInterpreterException() {
    return this.interpreteExcp;
  }

  public ASTTraverseVisitor getVisitor() {
    return this.visitor;
  }

  public static CompileThread allocCthread(Compiler runner, CompilerLoader loader, String[] tu_names)
      throws CompileException, ParseException {

    if (runner == null) {
      throw new CompileException("Invalid Runner");
    }

    if (loader == null) {
      throw new CompileException("Invalid ContainerLoader");
    }

    CompileThread vthread = null;
    ASTTraverseVisitor visitor = null;

    ASTTranslationUnit[] translation_units = new ASTTranslationUnit[tu_names.length];

    // Debug.enable_print_dbg = true;
    // Debug.enable_print_info = true;

    for (int i = 0; i < tu_names.length; i++) {
      try {
        translation_units[i] = loader.constAst(tu_names[i]);

        System.out.println("# Parsing(" + tu_names[i] + ") is completed");

        ASTpreprocessVisitor preprocess = new ASTpreprocessVisitor();
        preprocess.recurs_pre_order_tree_traverse(translation_units[i], 0);
        if (Debug.enable_print_dbg) {
          translation_units[i].dump_tree(".", 0, 0);
        }
        // TreeViewer viewer = new TreeViewer("Tree Viewer", translation_unit);
        // viewer.setVisible(true);
      } catch (CompileException e) {
        e.printStackTrace();
        e.set_tu_name(tu_names[i]);
        // translation_unit.dump_tree(".", 0, 0);
        throw e;
      } catch (ParseException e) {
        // e.printStackTrace();
        // throw new ParseException(e.getMessage() );
        throw e; // it simply throws ParseException
      }
    }

    for (int i = 0; i < tu_names.length; i++) {
      try {
        // create symbol table
        LOG.debug("\n#\n# ContextBuild");
        LOG.debug("...........................................................\n");
        visitor = new ASTContextBuildVisitor(loader, tu_names[i]);
        visitor.traverse(translation_units[i]);
      } catch (CompileException e) {
        e.printStackTrace();
        e.set_tu_name(tu_names[i]);
        // translation_unit.dump_tree(".", 0, 0);
        e.setStackTraceStr(visitor.dump_context_stack());
        throw e;
      }
    }

    for (int i = 0; i < tu_names.length; i++) {
      try {

        // resolving symbol
        LOG.debug("\n#\n# SymbolResolving");
        LOG.debug("...........................................................\n");
        visitor = new ASTSymbolResolvingVisitor(loader);
        visitor.traverse(translation_units[i]);
        visitor.check_reduction_stack_empty();
        // translation_unit.dump_tree(".", 0, 0);
      } catch (CompileException e) {
        e.printStackTrace();
        e.set_tu_name(tu_names[i]);
        // translation_unit.dump_tree(".", 0, 0);
        e.setStackTraceStr(visitor.dump_context_stack());
        throw e;
      }
    }

    for (int i = 0; i < tu_names.length; i++) {
      try {

        // resolving sub symbo
        LOG.debug("\n#\n# SubSymbolResolving");
        LOG.debug("...........................................................\n");
        visitor = new ASTSubSymbolResolvingVisitor(loader);
        ((ASTSubSymbolResolvingVisitor) visitor).dependency_traverse(translation_units[i]);
        // visitor.traverse(translation_unit);
        visitor.check_reduction_stack_empty();

        // translation_unit.dump_tree(".", 0, 0);
      } catch (CompileException e) {
        e.printStackTrace();
        e.set_tu_name(tu_names[i]);
        e.setStackTraceStr(visitor.dump_context_stack());
        throw e;
      }
    }

    for (int i = 0; i < tu_names.length; i++) {
      try {
        // compile
        LOG.debug("\n#\n# Compiling");
        LOG.debug("...........................................................\n");
        visitor = new ASTCompileVisitor(loader);
        visitor.traverse(translation_units[i]);
        visitor.check_reduction_stack_empty();
      } catch (CompileException e) {
        e.printStackTrace();
        e.set_tu_name(tu_names[i]);
        e.setStackTraceStr(visitor.dump_context_stack());
        throw e;
      }
    }

    System.out.println("\n#\n# Compilation is completed");

    return vthread;
  }

  public static CompileThread deserializeVthread(Compiler runner, CompilerLoader loader, InputStream in)
      throws CompileException, ParseException {

    return null;
  }

}
