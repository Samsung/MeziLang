package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Opcodes;

public class OpByteObj extends OpObject {

  private static final long serialVersionUID = 8762915333320854224L;

  public OpByteObj(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "ByteObjOp";

  }

  @Override
  public AbsType get_lval_coverting_type_of_nonassign_dual_op() throws CompileException {
    AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BYTE);
    return type;
  }

  @Override
  public AbsType get_boolreduce_coverting_type() throws CompileException {
    AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BYTE);
    return type;
  }

  @Override
  public AbsType get_converting_type(AbsType tgt_type) throws CompileException {
    if (tgt_type.isName(TPrimitiveClass.NAME_BYTE)) {
      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BYTE);
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName(TPrimitiveClass.NAME_BYTE)) {
      Debug.println_info("CHANGE java/lang/Byte -> B");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);

      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BYTE);
      return type;
    } else {
      throw new CompileException("Not Supported target type(" + tgttype + ")");
    }
  }

}
