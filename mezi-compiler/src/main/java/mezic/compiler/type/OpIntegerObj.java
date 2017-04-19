package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Opcodes;

public class OpIntegerObj extends OpObject {

  private static final long serialVersionUID = 2282327555345327145L;

  public OpIntegerObj(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "IntegerObjOp";

  }

  @Override
  public AbsType get_lval_coverting_type_of_nonassign_dual_op() throws CompileException {
    AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_INT);
    return type;
  }

  @Override
  public AbsType get_converting_type(AbsType tgt_type) throws CompileException {
    if (tgt_type.isName(TPrimitiveClass.NAME_INT)) {
      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_INT);
      return type;
    } else if (tgt_type.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName(TPrimitiveClass.NAME_INT)) {
      Debug.println_info("CHANGE java/lang/Integer->I");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);

      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_INT);
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      Debug.println_info("java/lang/Long->java/lang/String");

      return string_type_convert(lval, opinfo);
    } else {
      throw new CompileException("Not Supported target type(" + tgttype + ")");
    }
  }

}
