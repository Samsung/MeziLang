package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Opcodes;

public class OpChar extends OpShort {

  private static final long serialVersionUID = 5052711291118269650L;

  public OpChar(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "CharOp";
  }

  @Override
  public Container assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    if (!lval.isEqualType(rvalue)) {
      throw new CompileException("Type mismatch(dst: " + lval + ", src: " + rvalue + ")");
    }

    if (lval.getForm() == Container.FORM_FUNSTACK_VAR) {
      //// Compiled Instruction
      opinfo.mv.visitVarInsn(Opcodes.ISTORE, lval.getContextVarIdx());
      Debug.println_info("ISTORE" + lval.getContextVarIdx());
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
        Debug.println_info(
            "PUTSTATIC " + src_type.getName() + ":" + lval.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
        opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), lval.getName(), sub_ref_type.getMthdDscStr());
      } else {
        Debug.println_info(
            "PUTFIELD " + src_type.getName() + ":" + lval.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
        opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), lval.getName(), sub_ref_type.getMthdDscStr());
      }
      lval.setAssigned(true);
    } else if (lval.getForm() == Container.FORM_MAPELEMENT_VAR) {
      Container src_cont = lval.getOwnerContainer();
      Debug.assertion(src_cont != null, "lval_owner should be valid");
      Debug.assertion(src_cont.isTypeInitialized(), "lval_owner should be type initialized");
      Debug.assertion(src_cont.getType() instanceof TMapType, "lval_owner type should be TMapType");

      Debug.println_dbg("CASTORE");
      opinfo.mv.visitInsn(Opcodes.CASTORE);
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

    char char_val = (Character) lval.getContainerObject();

    // java char is 2byte(for unicode)...

    if (char_val >= 0 && char_val <= 5) {
      switch (char_val) {
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
      Debug.println_info("ICONST_" + char_val);
    } else if (char_val >= 6 && char_val <= 127) {
      opinfo.mv.visitIntInsn(Opcodes.BIPUSH, char_val);
      Debug.println_info("BIPUSH_" + char_val);
    } else if (char_val >= 128 && char_val <= 32767) {
      opinfo.mv.visitIntInsn(Opcodes.SIPUSH, char_val);
      Debug.println_info("SIPUSH_" + char_val);
    } else if (char_val >= 32768 && char_val <= 65535) {
      opinfo.mv.visitLdcInsn(char_val);
      Debug.println_info("LDC_" + char_val);
    } else {
      throw new CompileException("Out of Character boundary(" + char_val + ")");
    }
  }

  @Override
  public AbsType get_converting_type(AbsType tgttype) throws CompileException {
    if (tgttype.isName("java/lang/Character") || tgttype.isName("java/lang/Object")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Character");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName("java/lang/Character") || tgttype.isName("java/lang/Object")) {
      Debug.println_info("CHANGE C->java/lang/Character");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",
          false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Character");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      Debug.println_info("CHANGE C->java/lang/String");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "toString", "(C)Ljava/lang/String;",
          false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else {
      throw new CompileException("Not Supported target type");
    }

  }

  @Override
  public void explicit_casting(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
    opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);

    Debug.println_info("CAST to C");
  }

}
