package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpByte extends OpShort {

  private static final long serialVersionUID = 609500681055432094L;
  
  private static final Logger LOG = LoggerFactory.getLogger(OpByte.class);

  public OpByte(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "ByteOp";
  }

  @Override
  public Container assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    if (!lval.isEqualType(rvalue)) {
      throw new CompileException("Type mismatch(dst: " + lval + ", src: " + rvalue + ")");
    }

    if (lval.getForm() == Container.FORM_FUNSTACK_VAR) {
      //// Compiled Instruction
      opinfo.mv.visitVarInsn(Opcodes.ISTORE, lval.getContextVarIdx());
      LOG.info("ISTORE" + lval.getContextVarIdx());
      //// End
      lval.setAssigned(true);
    } else if (lval.getForm() == Container.FORM_OBJMEMBER_VAR) {
      Container src_cont = lval.getOwnerContainer();
      if (src_cont == null) {
        throw new CompileException("Invalid Src Container");
      }

      AbsType src_type = src_cont.getType();
      if (src_type == null) {
        throw new CompileException("Invalid Src Container Type");
      }

      AbsType sub_ref_type = lval.getType();
      if (sub_ref_type == null) {
        throw new CompileException("Invalid Sub Ref Container Type");
      }

      if (lval.isSingleton()) {
        LOG.info(
            "PUTSTATIC " + src_type.getName() + ":" + lval.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
        opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), lval.getName(), sub_ref_type.getMthdDscStr());
      } else {
        LOG.info(
            "PUTFIELD " + src_type.getName() + ":" + lval.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
        opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), lval.getName(), sub_ref_type.getMthdDscStr());
      }
      lval.setAssigned(true);
    } else if (lval.getForm() == Container.FORM_MAPELEMENT_VAR) {
      Container src_cont = lval.getOwnerContainer();
      Debug.assertion(src_cont != null, "lval_owner should be valid");
      Debug.assertion(src_cont.isTypeInitialized(), "lval_owner should be type initialized");
      Debug.assertion(src_cont.getType() instanceof TMapType, "lval_owner type should be TMapType");

      LOG.debug("BASTORE");
      opinfo.mv.visitInsn(Opcodes.BASTORE);
    } else {
      throw new CompileException("Invalid lval form(" + lval + ")");
    }

    // ISSUE : is this correct ?
    return lval.getOpStackClone("anonymous");
  }

  @Override
  public void load_constant(Container lval, OpInfo opinfo) throws CompileException {

    if (!lval.isConstant()) {
      throw new CompileException("Lvalue(" + lval + ") int not constant");
    }

    int int_val = (Byte) lval.getContainerObject();

    if (int_val >= -1 && int_val <= 5) {
      switch (int_val) {
      case -1:
        opinfo.mv.visitInsn(Opcodes.ICONST_M1);
        break;
      case 0:
        opinfo.mv.visitInsn(Opcodes.ICONST_0);
        break;
      case 1:
        opinfo.mv.visitInsn(Opcodes.ICONST_1);
        break;
      case 2:
        opinfo.mv.visitInsn(Opcodes.ICONST_2);
        break;
      case 3:
        opinfo.mv.visitInsn(Opcodes.ICONST_3);
        break;
      case 4:
        opinfo.mv.visitInsn(Opcodes.ICONST_4);
        break;
      case 5:
        opinfo.mv.visitInsn(Opcodes.ICONST_5);
        break;
      default:
        // do nothing
      }
      LOG.info("ICONST_" + int_val);
    } else if ((int_val >= 6 && int_val <= 127) || (int_val >= -128 && int_val <= -2)) {
      opinfo.mv.visitIntInsn(Opcodes.BIPUSH, int_val);
      LOG.info("BIPUSH_" + int_val);
    } else {
      throw new CompileException("Out of Integer boundary(" + int_val + ")");
    }
  }

  @Override
  public AbsType get_converting_type(AbsType tgttype) throws CompileException {
    if (tgttype.isName("java/lang/Byte") || tgttype.isName("java/lang/Object")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Byte");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName("java/lang/Byte") || tgttype.isName("java/lang/Object")) {
      LOG.info("CHANGE B->java/lang/Byte");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(C)Ljava/lang/Byte;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Byte");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      LOG.info("CHANGE B->java/lang/String");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "toString", "(B)Ljava/lang/String;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else {
      throw new CompileException("Not Supported target type");
    }
  }

  @Override
  public void explicit_casting(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
    opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);

    LOG.info("CAST to C");
  }

}
