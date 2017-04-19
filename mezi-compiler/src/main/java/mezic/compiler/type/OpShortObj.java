package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Opcodes;

public class OpShortObj extends OpObject {

  private static final long serialVersionUID = -9059456676899719574L;

  public OpShortObj(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "ShortObjOp";

  }

  @Override
  public AbsType get_lval_coverting_type_of_nonassign_dual_op() throws CompileException {
    AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_SHORT);
    return type;
  }

  @Override
  public AbsType get_boolreduce_coverting_type() throws CompileException {
    AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_SHORT);
    return type;
  }

  @Override
  public AbsType get_converting_type(AbsType tgt_type) throws CompileException {
    if (tgt_type.isName(TPrimitiveClass.NAME_SHORT)) {
      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_SHORT);
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName(TPrimitiveClass.NAME_SHORT)) {
      Debug.println_info("CHANGE java/lang/Short -> S");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);

      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_SHORT);
      return type;
    } else {
      throw new CompileException("Not Supported target type(" + tgttype + ")");
    }
  }

}
