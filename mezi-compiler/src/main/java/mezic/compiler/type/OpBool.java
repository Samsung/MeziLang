package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;
import mezic.parser.LangUnitNode;
import mezic.parser.ParserTreeConstants;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

public class OpBool extends OpObject {

  private static final long serialVersionUID = 7769559067341011833L;

  public OpBool(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "BooleanOp";
  }

  @Override
  public Container assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    if (!lval.isEqualType(rvalue)) {
      throw new CompileException("Type mismatch(dst: " + lval + ", src: " + rvalue + ")");
    }

    if (lval.getForm() == Container.FORM_FUNSTACK_VAR) {
      //// Compiled Instruction
      opinfo.mv.visitVarInsn(Opcodes.ISTORE, lval.getContextVarIdx());
      Debug.println_info("ISTORE" + lval.getContextVarIdx() + " for Boolean");
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

      /*
       * Debug.println_info("PUTFIELD "+ src_type.getName()+ ":" +
       * lval.getName()+"("+sub_ref_type.getMthdDscStr()+")"); //// Compiled
       * Instruction opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD,
       * src_type.getName(), lval.getName(), sub_ref_type.getMthdDscStr()); ////
       * End
       */

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

      opinfo.mv.visitInsn(Opcodes.BASTORE);
    } else {
      throw new CompileException("Invalid lval form(" + lval + ")");
    }

    return lval.getOpStackClone("anonymous");
  }

  /*
   * boolean does not support the following operations - map_access - map_create
   * - plus - minus - multiply - division - rest - smaller - bigger -
   * smaller_equal - bigger_equal
   */
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

    opinfo.mv.visitJumpInsn(Opcodes.IF_ICMPEQ, not_eq_label);
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

  /*
   * boolean does not support the following operations - and - exc_or - inc_or
   */
  @Override
  public void prepare_logical_and(OpInfo opinfo) throws CompileException {

    // Label zero_label = new Label();
    LangUnitNode node = opinfo.op_node;
    Debug.assertion(node != null, "node should be valid");

    LangUnitNode logicaland_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(logicaland_node != null, "logicaland_node should be valid");
    Debug.assertion(logicaland_node.isNodeId(ParserTreeConstants.JJTLOGICALAND), "logicaland_node should be valid");

    Label logicalop_br_label = logicaland_node.getLogicalOpBrLabel();
    Debug.assertion(logicalop_br_label != null, "zero_label should be valid");

    opinfo.mv.visitJumpInsn(Opcodes.IFEQ, logicalop_br_label);
  }

  @Override
  public Container logical_and(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    AbsType booltype = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL);

    AbsType lval_type = lval.getType();
    AbsType rval_type = lval.getType();

    if (!lval_type.equals(booltype)) {
      throw new CompileException("lvalue should be boolean type, but " + lval);
    }
    if (!rval_type.equals(booltype)) {
      throw new CompileException("rvalue should be boolean type, but " + rvalue);
    }

    // Label zero_label = new Label();
    LangUnitNode node = opinfo.op_node;
    Debug.assertion(node != null, "node should be valid");

    Label logicalop_br_label = node.getLogicalOpBrLabel();
    Debug.assertion(logicalop_br_label != null, "logicalop_br_label should be valid");

    Label end_label = new Label();

    // see 'prepareLogicalAnd in CompilerVisitor' & prepare_logical_and
    // opinfo.mv.visitInsn(Opcodes.IAND);
    // opinfo.mv.visitJumpInsn(Opcodes.IFEQ, zero_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_1); // true
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(logicalop_br_label); // false
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_bool.initializeType(booltype);
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  @Override
  public void prepare_logical_or(OpInfo opinfo) throws CompileException {

    // Label zero_label = new Label();
    LangUnitNode node = opinfo.op_node;
    Debug.assertion(node != null, "node should be valid");

    LangUnitNode logicalor_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(logicalor_node != null, "logicalor_node should be valid");
    Debug.assertion(logicalor_node.isNodeId(ParserTreeConstants.JJTLOGICALOR), "logicaland_node should be valid");

    Label logicalop_br_label = logicalor_node.getLogicalOpBrLabel();
    Debug.assertion(logicalop_br_label != null, "logicalop_br_label should be valid");

    opinfo.mv.visitJumpInsn(Opcodes.IFNE, logicalop_br_label);
  }

  @Override
  public Container logical_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    AbsType booltype = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL);

    AbsType lval_type = lval.getType();
    AbsType rval_type = lval.getType();

    if (!lval_type.equals(booltype)) {
      throw new CompileException("lvalue should be boolean type, but " + lval);
    }
    if (!rval_type.equals(booltype)) {
      throw new CompileException("rvalue should be boolean type, but " + rvalue);
    }

    LangUnitNode node = opinfo.op_node;
    Debug.assertion(node != null, "node should be valid");

    Label logicalop_br_label = node.getLogicalOpBrLabel();
    Debug.assertion(logicalop_br_label != null, "logicalop_br_label should be valid");
    Label end_label = new Label();

    /*
     * opinfo.mv.visitInsn(Opcodes.IOR); opinfo.mv.visitJumpInsn(Opcodes.IFEQ,
     * zero_label); opinfo.mv.visitInsn(Opcodes.ICONST_1);
     * opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
     * opinfo.mv.visitLabel(zero_label); opinfo.mv.visitInsn(Opcodes.ICONST_0);
     * opinfo.mv.visitLabel(end_label);
     */
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(logicalop_br_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_bool.initializeType(booltype);
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  /*
   * boolean does not support the following operations - shfit_left -
   * shfit_right - unary_negative
   */

  @Override
  public Container not(Container lval, OpInfo opinfo) throws CompileException {
    AbsType lval_type = lval.getType();

    if (!lval_type.isName(TPrimitiveClass.NAME_BOOL)) {
      throw new CompileException("lvalue should be boolean type, but " + lval);
    }

    Label zero_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitJumpInsn(Opcodes.IFEQ, zero_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(zero_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    AbsType booltype = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL);
    anony_bool.initializeType(booltype);
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  /*
   * boolean does not support the following operations - inverse -
   * multiply_assign - division_assign - rest_assign - plus_assign -
   * minus_assign - shift_left_assign - shift_right_assign - and_assign_assign -
   * exclusive_or_assign - inclusive_or_assign - unary_plusplus -
   * unary_minusminus - postfix_plusplus - postfix_minusminus
   */

  @Override
  public void load_constant(Container lval, OpInfo opinfo) throws CompileException {

    if (!lval.isConstant()) {
      throw new CompileException("Lvalue(" + lval + ") int not constant");
    }

    boolean bool_val = (Boolean) lval.getContainerObject();

    if (bool_val) {
      opinfo.mv.visitInsn(Opcodes.ICONST_1);
      Debug.println_info("ICONST_1 for boolean");
    } else {
      opinfo.mv.visitInsn(Opcodes.ICONST_0);
      Debug.println_info("ICONST_0 for boolean");
    }

  }

  @Override
  public void store(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.ISTORE, index);
    Debug.println_info("ISTORE_" + index + " for Boolean");
  }

  @Override
  public void load_funcstack_variable(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.ILOAD, index);
    Debug.println_info("ILOAD_" + index + " for Boolean");
  }

  @Override
  public void return_variable(Container lval, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.IRETURN);
    Debug.println_info("IRETURN" + " for Boolean");
  }

  @Override
  public void return_dummy_variable(OpInfo opinfo) throws CompileException {

    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    Debug.println_info("ICONST_1 for boolean");

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
  public AbsType get_converting_type(AbsType tgt_type) throws CompileException {
    if (tgt_type.isName("java/lang/Boolean") || tgt_type.isName("java/lang/Object")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Boolean");
      return type;
    } else if (tgt_type.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgt_type, OpInfo opinfo) throws CompileException {
    if (tgt_type.isName("java/lang/Boolean") || tgt_type.isName("java/lang/Object")) {
      Debug.println_info("CONVERT Z->java/lang/Boolean");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Boolean");
      return type;
    } else if (tgt_type.isName("java/lang/String")) {
      Debug.println_info("CHANGE Z->java/lang/String");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "toString", "(Z)Ljava/lang/String;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else {
      throw new CompileException("Not Supported target type");
    }

  }

  @Override
  public void explicit_casting(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
    opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);

    Debug.println_info("CAST to I");
  }

}
