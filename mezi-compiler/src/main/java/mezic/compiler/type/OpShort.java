package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

public class OpShort extends OpObject {

  private static final long serialVersionUID = -321738517768153025L;

  public OpShort(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "ShortOp";
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

      Debug.println_dbg("SASTORE");
      opinfo.mv.visitInsn(Opcodes.SASTORE);
    } else {
      throw new CompileException("Invalid lval form(" + lval + ")");
    }

    // ISSUE : is this correct ?
    return lval.getOpStackClone("anonymous");
  }

  /*
   * int does not support map operation - map_access - map_create - plus - minus
   * - multiply - division - rest
   */

  @Override
  public Container smaller(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Label greater_equal_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitJumpInsn(Opcodes.IF_ICMPGE, greater_equal_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(greater_equal_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_bool.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL));
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  @Override
  public Container bigger(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Label less_eq_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitJumpInsn(Opcodes.IF_ICMPLE, less_eq_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(less_eq_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_bool.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL));
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  @Override
  public Container smaller_equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Label greater_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitJumpInsn(Opcodes.IF_ICMPGT, greater_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(greater_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_bool.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL));
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  @Override
  public Container bigger_equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Label less_eq_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitJumpInsn(Opcodes.IF_ICMPLT, less_eq_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(less_eq_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_bool.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL));
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  @Override
  public Container equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Label not_eq_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitJumpInsn(Opcodes.IF_ICMPNE, not_eq_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(not_eq_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_bool.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL));
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  @Override
  public Container not_equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Label not_eq_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitJumpInsn(Opcodes.IF_ICMPNE, not_eq_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(not_eq_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_bool.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL));
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  /*
   * int does not support map operation - and - exc_or - inc_or - logical_and -
   * logical_or - shfit_left - shfit_right - unary_negative - not - inverse
   */

  /*
   * Java supports following operation, but, this lang. does not support
   * following operations.... - multiply_assign - division_assign - rest_assign
   * - plus_assign - minus_assign - shift_left_assign - shift_right_assign -
   * and_assign_assign - exclusive_or_assign - inclusive_or_assign -
   * unary_plusplus - unary_minusminus - postfix_plusplus - postfix_minusminus
   */

  @Override
  public void load_constant(Container lval, OpInfo opinfo) throws CompileException {

    if (!lval.isConstant()) {
      throw new CompileException("Lvalue(" + lval + ") int not constant");
    }

    int int_val = (Short) lval.getContainerObject();

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
        //do nothing
      }
      Debug.println_info("ICONST_" + int_val);
    } else if ((int_val >= 6 && int_val <= 127) || (int_val >= -128 && int_val <= -2)) {
      opinfo.mv.visitIntInsn(Opcodes.BIPUSH, int_val);
      Debug.println_info("BIPUSH_" + int_val);
    } else if ((int_val >= 128 && int_val <= 32767) || (int_val >= -32768 && int_val <= -129)) {
      opinfo.mv.visitIntInsn(Opcodes.SIPUSH, int_val);
      Debug.println_info("SIPUSH_" + int_val);
    } else {
      throw new CompileException("Out of Integer boundary(" + int_val + ")");
    }
  }

  @Override
  public void store(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.ISTORE, index);
    Debug.println_info("ISTORE_" + index);
  }

  @Override
  public void store_map_element(OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.CASTORE);
    Debug.println_info("CASTORE");
  }

  @Override
  public void load_funcstack_variable(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.ILOAD, index);
    Debug.println_info("ILOAD_" + index);
  }

  @Override
  public void return_variable(Container lval, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.IRETURN);
    Debug.println_info("IRETURN");
  }

  @Override
  public void return_dummy_variable(OpInfo opinfo) throws CompileException {

    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    Debug.println_info("ICONST_1 for " + this.getOpString());

    opinfo.mv.visitInsn(Opcodes.IRETURN);
    Debug.println_info("IRETURN" + " for Boolean");
  }

  @Override
  public AbsType get_return_type_of_single_op(int opcode, AbsType lval_type, TContext top_context)
      throws CompileException {
    return lval_type;
  }

  @Override
  public AbsType get_return_type_of_nonassign_dualchild_op(int opcode, AbsType lval_type, AbsType rval_type,
      TContext top_context) throws CompileException {
    AbsType ret_type = null;
    if (Operation.is_comparable(opcode)) {
      ret_type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL);
    } else {
      ret_type = lval_type;
    }
    return ret_type;
  }

  @Override
  public boolean is_compatible_type(AbsType tgttype) throws CompileException {
    if (tgttype.isName(TPrimitiveClass.NAME_INT)) {
      return true;
    }

    return false;
  }

  @Override
  public AbsType get_converting_type(AbsType tgttype) throws CompileException {
    if (tgttype.isName("java/lang/Short") || tgttype.isName("java/lang/Object")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Short");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName("java/lang/Short") || tgttype.isName("java/lang/Object")) {
      Debug.println_info("CHANGE S->java/lang/Short");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Short");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      Debug.println_info("CHANGE S->java/lang/String");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "toString", "(S)Ljava/lang/String;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else {
      throw new CompileException("Not Supported target type");
    }
  }

  @Override
  public void explicit_casting(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
    opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);

    Debug.println_info("CAST to S");
  }

}
