package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import java.util.Objects;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpObject implements OpLvalue {

  private static final long serialVersionUID = -2536277020146509831L;
  
  private static final Logger LOG = LoggerFactory.getLogger(OpObject.class);


  transient protected CompilerLoader cpLoader = null;

  public OpObject(CompilerLoader cpLoader) {
    this.cpLoader = cpLoader;
  }

  @Override
  public String getOpString() {
    return "ObjectOp";
  }

  @Override
  public int getCategory() {
    return 1;
  }

  @Override
  public boolean equals(Object op) {
    // if( !( op instanceof LvalueOp) ) return false;

    return this.getClass().equals(op.getClass());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getClass());
  }


  public String toString() {
    return getOpString();
  }

  @Override
  public Container assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    if (lval.getForm() == Container.FORM_OBJMEMBER_VAR) {
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
        //// Compiled Instruction
        opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), lval.getName(), sub_ref_type.getMthdDscStr());
        //// End
      } else {
        LOG.info(
            "PUTFIELD " + src_type.getName() + ":" + lval.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
        //// Compiled Instruction
        opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), lval.getName(), sub_ref_type.getMthdDscStr());
        //// End
      }
      lval.setAssigned(true);
    } else if (lval.getForm() == Container.FORM_MAPELEMENT_VAR) {
      Container src_cont = lval.getOwnerContainer();
      Debug.assertion(src_cont != null, "lval_owner should be valid");
      Debug.assertion(src_cont.isTypeInitialized(), "lval_owner should be type initialized");
      Debug.assertion(src_cont.getType() instanceof TMapType, "lval_owner type should be TMapType");

      opinfo.mv.visitInsn(Opcodes.AASTORE);
    } else { // normal object
      lval.initializeType(rvalue.getType());
      lval.setAssigned(true);
      opinfo.mv.visitVarInsn(Opcodes.ASTORE, lval.getContextVarIdx());
      LOG.info("ASTORE" + lval.getContextVarIdx());
    }

    return lval.getOpStackClone("anonymous");
  }

  @Override
  public void assign_rvalue_dup(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    switch (lval.getForm()) {
    case Container.FORM_FUNSTACK_VAR:
      rvalue.getType().op().dup(rvalue, opinfo);
      break;

    case Container.FORM_OBJMEMBER_VAR:
      if (lval.isSingleton()) {
        // duplicate the value on top of the stack (value2, value1 ¡æ value1,
        // value2, value1)
        rvalue.getType().op().dup(rvalue, opinfo);
      } else {
        // duplicate the value on top of the stack (value2, value1 ¡æ value1,
        // value2, value1)
        rvalue.getType().op().dup_x1(rvalue, opinfo);
      }
      break;

    case Container.FORM_MAPELEMENT_VAR:
      rvalue.getType().op().dup_x2(rvalue, opinfo);
      break;

    default:
      throw new CompileException("It cannot be assigned to target(" + rvalue + ")");
    }
  }

  @Override
  public Container map_access(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    return dual_op_overload("[]", lval, rvalue, opinfo);
  }

  @Override
  public Container map_create(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    throw new CompileException("'map_create' lvalue op is not supported for " + toString());
  }

  @Override
  public Container plus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    return dual_op_overload("+", lval, rvalue, opinfo);
  }

  @Override
  public Container minus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    return dual_op_overload("-", lval, rvalue, opinfo);
  }

  @Override
  public Container multiply(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'multiply' lvalue op is not supported for " + toString());
  }

  @Override
  public Container division(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'division' lvalue op is not supported for " + toString());
  }

  @Override
  public Container rest(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'rest' lvalue op is not supported for " + toString());
  }

  @Override
  public Container smaller(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'smaller' lvalue op is not supported for " + toString());
  }

  @Override
  public Container bigger(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'bigger' lvalue op is not supported for " + toString());
  }

  @Override
  public Container smaller_equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'smaller_equal' lvalue op is not supported for " + toString());
  }

  @Override
  public Container bigger_equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'bigger_equal' lvalue op is not supported for " + toString());
  }

  @Override
  public Container equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Label not_eq_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitJumpInsn(Opcodes.IF_ACMPNE, not_eq_label);
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

    opinfo.mv.visitJumpInsn(Opcodes.IF_ACMPNE, not_eq_label);
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

  @Override
  public Container and(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'and' lvalue op is not supported for " + toString());
  }

  @Override
  public Container exc_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'exc_or' lvalue op is not supported for " + toString());
  }

  @Override
  public Container inc_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'inc_or' lvalue op is not supported for " + toString());
  }

  @Override
  public Container logical_and(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'logical_and' lvalue op is not supported for " + toString());
  }

  @Override
  public void prepare_logical_and(OpInfo opinfo) throws CompileException {
    throw new CompileException("'logical_and' lvalue op is not supported for " + toString());
  }

  @Override
  public Container logical_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'logical_or' lvalue op is not supported for " + toString());
  }

  @Override
  public void prepare_logical_or(OpInfo opinfo) throws CompileException {
    throw new CompileException("'logical_or' lvalue op is not supported for " + toString());
  }

  @Override
  public Container shfit_left(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'shfit_left' lvalue op is not supported for " + toString());
  }

  @Override
  public Container shfit_right(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'shfit_right' lvalue op is not supported for " + toString());
  }

  @Override
  /* used for (-1) (-a) */
  public Container unary_negative(Container lval, OpInfo opinfo) throws CompileException {
    throw new CompileException("'unary addtitive' lvalue op is not supported for " + toString());
  }

  // !
  @Override
  public Container not(Container lval, OpInfo opinfo) throws CompileException {
    throw new CompileException("'not' lvalue op is not supported for " + toString());
  }

  // ~
  @Override
  public Container inverse(Container lval, OpInfo opinfo) throws CompileException {
    throw new CompileException("'inverse' lvalue op is not supported for " + toString());
  }

  @Override
  public Container multiply_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container division_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container rest_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container plus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container minus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container shift_left_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container shift_right_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container and_assign_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    if (!lval.isTypeInitialized()) {
      throw new CompileException("lvalue(" + lval + ") is not initialized");
    }
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container exclusive_or_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container inclusive_or_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  protected void do_assign_common(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Debug.assertion(lval.isForm(Container.FORM_OPSTACK_VAR), "lval should be opstack var, but " + lval);

    Container org_cont = lval.getOwnerContainer();
    Debug.assertion(org_cont != null, "org_cont should be valid");

    if (org_cont.isForm(Container.FORM_MAPELEMENT_VAR)) {
      lval.op().dup_x2(lval, opinfo);
      lval.op().store_map_element(opinfo);
    } else if (org_cont.isForm(Container.FORM_OBJMEMBER_VAR)) {
      if (org_cont.isSingleton()) {
        org_cont.op().dup(lval, opinfo);
      } else {
        org_cont.op().dup_x1(lval, opinfo);
      }

      org_cont.op().assign(org_cont, rvalue, opinfo);
    } else if (org_cont.isForm(Container.FORM_FUNSTACK_VAR)) {
      lval.op().dup(lval, opinfo);
      lval.op().assign(org_cont, rvalue, opinfo);
    } else {
      throw new CompileException("Invalid lval form(" + lval + ")");
    }

  }

  public Container dual_op_overload(String op_str, Container lval, Container rvalue, OpInfo opinfo)
      throws CompileException {

    AbsType lval_type = lval.getType();
    AbsType rval_type = rvalue.getType();

    AbsTypeList paratype_list = new AbsTypeList();
    paratype_list.add(rval_type);

    // AbsType func_type = (AbsType)
    // ((AbsClassType)lval_type).getLocalFunction(op_str, paratype_list );
    AbsType func_type = cpLoader.findLocalFunction((AbsClassType) lval_type, op_str, paratype_list);

    if (func_type == null) {
      throw new CompileException(
          "member function " + op_str + "(" + rval_type.getMthdDscStr() + ") is not defined in " + lval_type);
    }
    LOG.debug("func_type:" + func_type);

    cpLoader.checkExceptionHandling(func_type, opinfo.top_context);

    AbsType classType = func_type.getOwnerType();

    opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classType.getName(), func_type.getName(),
        func_type.getMthdDscStr(), false);

    AbsType ret_type = ((AbsFuncType) func_type).getReturnType(cpLoader);

    Container ret_obj = new Container("anonymous", Container.FORM_OPSTACK_VAR, false, false);
    ret_obj.initializeType(ret_type);

    return ret_obj;
  }

  /* unary */
  @Override
  public Container unary_plusplus(Container lval, OpInfo opinfo) throws CompileException {
    throw new CompileException("'unary_plusplus' lvalue op is not supported for " + toString());
  }

  @Override
  public Container unary_minusminus(Container lval, OpInfo opinfo) throws CompileException {
    throw new CompileException("'unary_minusminus' lvalue op is not supported for " + toString());
  }

  /* postfix */
  @Override
  public Container postfix_plusplus(Container lval, OpInfo opinfo) throws CompileException {
    throw new CompileException("'posfix_plusplus' lvalue op is not supported for " + toString());
  }

  @Override
  public Container postfix_minusminus(Container lval, OpInfo opinfo) throws CompileException {
    throw new CompileException("'posfix_plusplus' lvalue op is not supported for " + toString());
  }

  @Override
  public void load_constant(Container lval, OpInfo opinfo) throws CompileException {

    throw new CompileException("'load constant' lvalue op is not supported for " + toString());
  }

  @Override
  public void store(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.ASTORE, index);
    LOG.info("ASTORE_" + index);
  }

  @Override
  public void store_map_element(OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.AASTORE);
    LOG.info("AASTORE");
  }

  @Override
  public void load_funcstack_variable(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.ALOAD, index);
    LOG.info("ALOAD_" + index);
  }

  @Override
  public Container load_variable(Container var_cont, Container owner_cont, OpInfo opinfo) throws CompileException {

    Container loaded_obj = null;

    Debug.assertion(var_cont.isTypeInitialized(), "Constant Container should be type initialized");

    switch (var_cont.getForm()) {
    case Container.FORM_FUNSTACK_VAR:
      Debug.assertion(var_cont.isAssigned(), "loading variable(" + var_cont + ") should be assigned");
      var_cont.op().load_funcstack_variable(var_cont, opinfo, var_cont.getContextVarIdx());
      loaded_obj = var_cont.getOpStackClone(var_cont.getName());
      break;

    case Container.FORM_OBJMEMBER_VAR:
      // we cannot check whether member variable is assigned or not
      // Container owner_cont = var_cont.getOwnerContainer();
      Debug.assertion(owner_cont != null, "Owner Container should not be invalid");
      LOG.debug("Owner Container: " + owner_cont);

      AbsType owner_type = owner_cont.getType();
      AbsType var_type = var_cont.getType();

      if (var_cont.isSingleton()) {
        LOG.debug(owner_cont.toString());
        if (owner_cont.isForm(Container.FORM_OPSTACK_VAR)) {
          /*
           * This is for accessing singleton member variable access
           *
           * ex) a.b.c.d.e ( e is singleton, or d is singleton, or c is
           * singleton ..)
           */
          LOG.info("POP OPSTACK_VAR for GETSTATIC");
          // throw new InterpreterException("Stop");
          owner_cont.getType().op().pop(owner_cont, opinfo);
        }

        LOG.info(
            "GETSTATIC " + owner_type.getName() + ":" + var_cont.getName() + "(" + var_type.getMthdDscStr() + ")");
        opinfo.mv.visitFieldInsn(Opcodes.GETSTATIC, owner_type.getName(), var_cont.getName(), var_type.getMthdDscStr());
      } else {
        LOG.info(
            "GETFIELD " + owner_type.getName() + ":" + var_cont.getName() + "(" + var_type.getMthdDscStr() + ")");
        opinfo.mv.visitFieldInsn(Opcodes.GETFIELD, owner_type.getName(), var_cont.getName(), var_type.getMthdDscStr());
      }

      loaded_obj = var_cont.getOpStackClone(var_cont.getName());
      break;

    case Container.FORM_CONSTANT_VAR:
      var_cont.op().load_constant(var_cont, opinfo);
      loaded_obj = var_cont.getOpStackClone(var_cont.getName());
      break;

    default:
      throw new CompileException(
          "This Container Type(" + var_cont.getFormString() + ") does not support load variable");

    }

    return loaded_obj;

  }

  @Override
  public void dup(Container lval, OpInfo opinfo) throws CompileException {
    switch (getCategory()) {
    case 1:
      LOG.debug("DUP");
      opinfo.mv.visitInsn(Opcodes.DUP);
      break;
    case 2:
      LOG.debug("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      break;
    default:
      throw new CompileException("Invalid Type Category(" + getCategory() + ")");
    }
  }

  @Override
  public void dup_x1(Container lval, OpInfo opinfo) throws CompileException {
    switch (getCategory()) {
    case 1:
      LOG.debug("DUP_X1");
      opinfo.mv.visitInsn(Opcodes.DUP_X1);
      break;
    case 2:
      LOG.debug("DUP2_X1");
      opinfo.mv.visitInsn(Opcodes.DUP2_X1);
      break;
    default:
      throw new CompileException("Invalid Type Category(" + getCategory() + ")");
    }
  }

  @Override
  public void dup_x2(Container lval, OpInfo opinfo) throws CompileException {
    switch (getCategory()) {
    case 1:
      LOG.debug("DUP_X2");
      opinfo.mv.visitInsn(Opcodes.DUP_X2);
      break;
    case 2:
      LOG.debug("DUP2_X2");
      opinfo.mv.visitInsn(Opcodes.DUP2_X2);
      break;
    default:
      throw new CompileException("Invalid Type Category(" + getCategory() + ")");
    }
  }

  @Override
  public void pop(Container lval, OpInfo opinfo) throws CompileException {
    switch (getCategory()) {
    case 1:
      LOG.debug("POP");
      opinfo.mv.visitInsn(Opcodes.POP);
      break;
    case 2:
      LOG.debug("POP2");
      opinfo.mv.visitInsn(Opcodes.POP2);
      break;
    default:
      throw new CompileException("Invalid Type Category(" + getCategory() + ")");
    }
  }

  @Override
  public void return_variable(Container lval, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.ARETURN);
    LOG.info("ARETURN");
  }

  @Override
  public void return_dummy_variable(OpInfo opinfo) throws CompileException {

    LOG.info("ACONST_NULL");
    opinfo.mv.visitInsn(Opcodes.ACONST_NULL);

    opinfo.mv.visitInsn(Opcodes.ARETURN);
    LOG.info("ARETURN");
  }

  public AbsType get_return_type_of_single_op(int opcode, AbsType lval_type, TContext top_context)
      throws CompileException {
    String func_name = Operation.opOverloadingFuncName(opcode);

    AbsTypeList paratype_list = new AbsTypeList();

    AbsType func_type = cpLoader.findLocalFunction((AbsClassType) lval_type, func_name, paratype_list);

    if (func_type == null) {
      throw new CompileException(
          "member function " + func_name + "() is not defined in " + lval_type + "(" + lval_type.op() + ")");
    }

    cpLoader.checkExceptionHandling(func_type, top_context);

    AbsType ret_type = ((AbsFuncType) func_type).getReturnType(cpLoader);
    return ret_type;
  }

  @Override
  public AbsType get_return_type_of_nonassign_dualchild_op(int opcode, AbsType lval_type, AbsType rval_type,
      TContext top_context) throws CompileException {
    AbsType ret_type = null;

    // equality operation is not permitted for operator overload
    if (Operation.is_equality(opcode)) {
      ret_type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL);
      return ret_type;
    }

    String func_name = Operation.opOverloadingFuncName(opcode);

    AbsTypeList paratype_list = new AbsTypeList();
    paratype_list.add(rval_type);

    Debug.assertion(lval_type instanceof AbsClassType, "lval_type should be instanceof AbsClassType");

    AbsType func_type = cpLoader.findLocalFunction((AbsClassType) lval_type, func_name, paratype_list);

    if (func_type == null) {
      throw new CompileException("member function " + func_name + "(" + rval_type.getMthdDscStr()
          + ") is not defined in " + lval_type + "(" + lval_type.op() + ")");
    }

    cpLoader.checkExceptionHandling(func_type, top_context);

    ret_type = ((AbsFuncType) func_type).getReturnType(cpLoader);
    return ret_type;
  }

  @Override
  public AbsType get_lval_coverting_type_of_nonassign_dual_op() throws CompileException {
    return null;
  }

  @Override
  public AbsType get_boolreduce_coverting_type() throws CompileException {
    return null;
  }

  @Override
  public boolean is_compatible_type(AbsType type) throws CompileException {
    return false;
  }

  @Override
  public AbsType get_converting_type(AbsType tgttype) throws CompileException {
    if (tgttype.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_VOID)) { // for null
      return tgttype;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container rval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName("java/lang/String")) {
      LOG.info("java/lang/Object->java/lang/String");

      return string_type_convert(rval, opinfo);
    } else if (tgttype.isName(TPrimitiveClass.NAME_VOID)) { // for null
      return tgttype;
    }

    throw new CompileException("not supported operation for " + toString());
  }

  protected AbsType string_type_convert(Container lval, OpInfo opinfo) throws CompileException {
    Label nonull_label = new Label();
    Label end_label = new Label();
    lval.getType().op().dup(lval, opinfo);

    // null checking..
    opinfo.mv.visitJumpInsn(Opcodes.IFNONNULL, nonull_label);

    lval.getType().op().pop(lval, opinfo);
    // opinfo.mv.visitInsn(Opcodes.ACONST_NULL);
    opinfo.mv.visitLdcInsn("null");
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);

    opinfo.mv.visitLabel(nonull_label);
    opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;", false);
    opinfo.mv.visitLabel(end_label);

    AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
    return type;
  }

  @Override
  public void explicit_casting(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitTypeInsn(Opcodes.CHECKCAST, lval.getType().getName());
    LOG.info("CAST to " + lval.getType().getName());
  }

}
