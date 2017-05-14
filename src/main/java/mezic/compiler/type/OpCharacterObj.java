package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;

import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpCharacterObj extends OpObject {

  private static final long serialVersionUID = -2997908685943678833L;
  
  private static final Logger LOG = LoggerFactory.getLogger(OpCharacterObj.class);

  public OpCharacterObj(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "CharacterObjOp";

  }

  @Override
  public AbsType get_lval_coverting_type_of_nonassign_dual_op() throws CompileException {
    AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_CHAR);
    return type;
  }

  @Override
  public AbsType get_boolreduce_coverting_type() throws CompileException {
    AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_CHAR);
    return type;
  }

  @Override
  public AbsType get_converting_type(AbsType tgt_type) throws CompileException {
    if (tgt_type.isName(TPrimitiveClass.NAME_CHAR)) {
      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_CHAR);
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName(TPrimitiveClass.NAME_CHAR)) {
      LOG.info("CHANGE java/lang/Character -> C");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);

      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_CHAR);
      return type;
    } else {
      throw new CompileException("Not Supported target type(" + tgttype + ")");
    }
  }

}
