package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.Debug;
import mezic.parser.LangUnitNode;

import org.objectweb.asm.MethodVisitor;

public class OpInfo {

  public TContext top_context = null;
  public MethodVisitor mv = null;
  public LangUnitNode op_node = null;

  public OpInfo(TContext top_context) throws CompileException {

    this.top_context = top_context;

    TContext func_context = top_context.getClosestAncestor(AbsType.FORM_FUNC);
    Debug.assertion(func_context != null, "func_context should not be invalid");
    Debug.assertion(func_context instanceof TContextFunc,
        "Invalid Context From(" + func_context.getFormString(func_context.getForm()) + ")");

    this.mv = ((TContextFunc) func_context).getMethodVisitor();
    Debug.assertion(mv != null, "mv should be valid");

  }

  public OpInfo(MethodVisitor mv) throws CompileException {
    this.mv = mv;
    Debug.assertion(mv != null, "mv should be valid");
  }

}
