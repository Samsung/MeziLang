package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import java.util.Objects;

import org.objectweb.asm.Opcodes;

//public class OpVoid implements OpLvalue  {
public class OpVoid extends OpObject {

  private static final long serialVersionUID = -6926163662376121398L;

  /*
   * transient protected CompilerLoader cpLoader = null; public
   * OpVoid(CompilerLoader cpLoader) { this.cpLoader = cpLoader; }
   */
  public OpVoid(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "VoidOp";
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
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public void assign_rvalue_dup(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container map_access(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container map_create(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container plus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'add' lvalue op is not supported for " + toString());
  }

  @Override
  public Container minus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    throw new CompileException("'minus' lvalue op is not supported for " + toString());
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

  /*
   * @Override public Container equal(Container lval, Container rvalue, OpInfo
   * opinfo) throws InterpreterException { throw new InterpreterException(
   * "'equal' lvalue op is not supported for " +toString()); }
   *
   * @Override public Container not_equal(Container lval, Container rvalue,
   * OpInfo opinfo) throws InterpreterException { throw new
   * InterpreterException("'not_equal' lvalue op is not supported for "
   * +toString()); }
   */

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
  public Container logical_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
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

  @Override
  public Container not(Container lval, OpInfo opinfo) throws CompileException {
    throw new CompileException("'not' lvalue op is not supported for " + toString());
  }

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

  @Override
  public Container unary_plusplus(Container lval, OpInfo opinfo) throws CompileException {
    throw new CompileException("'unary_plusplus' lvalue op is not supported for " + toString());
  }

  @Override
  public Container unary_minusminus(Container lval, OpInfo opinfo) throws CompileException {
    throw new CompileException("'unary_plusplus' lvalue op is not supported for " + toString());
  }

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

    if (!lval.isConstant()) {
      throw new CompileException("Lvalue(" + lval + ") int not constant");
    }

    if (lval.getContainerObject() instanceof ObjNull) {
      Debug.println_info("ACONST_NULL");
      opinfo.mv.visitInsn(Opcodes.ACONST_NULL);
    } else {
      throw new CompileException("'load constant' lvalue op is not supported for " + toString());
    }

  }

  @Override
  public void store(Container lval, OpInfo opinfo, int index) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public void store_map_element(OpInfo opinfo) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public void load_funcstack_variable(Container lval, OpInfo opinfo, int index) throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public Container load_variable(Container var_cont, Container owner_cont, OpInfo opinfo) throws CompileException {
    Container loaded_obj = null;

    Debug.assertion(var_cont.isTypeInitialized(), "Constant Container should be type initialized");

    switch (var_cont.getForm()) {
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

  /*
   * @Override public void dup(Container lval, OpInfo opinfo) throws
   * InterpreterException{ throw new InterpreterException(
   * "not supported operation for " +toString()); }
   *
   * @Override public void dup_x1(Container lval, OpInfo opinfo) throws
   * InterpreterException{ throw new InterpreterException(
   * "not supported operation for " +toString()); }
   *
   * @Override public void dup_x2(Container lval, OpInfo opinfo) throws
   * InterpreterException{ throw new InterpreterException(
   * "not supported operation for " +toString()); }
   *
   * @Override public void pop(Container lval, OpInfo opinfo) throws
   * InterpreterException{ throw new InterpreterException(
   * "not supported operation for " +toString()); }
   */

  @Override
  public void return_variable(Container lval, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.RETURN);
    Debug.println_info("RETURN" + "(no return value) for Void");
  }

  @Override
  public void return_dummy_variable(OpInfo opinfo) throws CompileException {

    // do nothing
  }

  @Override
  public AbsType get_return_type_of_single_op(int opcode, AbsType lval_type, TContext top_context)
      throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public AbsType get_return_type_of_nonassign_dualchild_op(int opcode, AbsType lval_type, AbsType rval_type,
      TContext top_context) throws CompileException {
    AbsType ret_type = null;
    if (Operation.is_comparable(opcode)) {
      ret_type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL);

      return ret_type;
    }

    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public AbsType get_lval_coverting_type_of_nonassign_dual_op() throws CompileException {
    return null;
  }

  @Override
  public AbsType get_boolreduce_coverting_type() throws CompileException {
    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public AbsType get_converting_type(AbsType tgttype) throws CompileException {
    if (tgttype.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else if (!(tgttype instanceof TPrimitiveClass)) {
      return tgttype;
    } else {
      return null;
    }
  }

  @Override
  public AbsType type_convert(Container lval, AbsType type, OpInfo opinfo) throws CompileException {
    if (type.isName("java/lang/String")) {
      lval.getType().op().pop(lval, opinfo);
      opinfo.mv.visitLdcInsn("null");

      return (AbsType) cpLoader.findClassFull("java/lang/String");
    } else if (!(type instanceof TPrimitiveClass)) {
      // do nothing
      return type;
    }

    throw new CompileException("not supported operation for " + toString());
  }

  @Override
  public void explicit_casting(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    throw new CompileException("not supported operation for " + toString());
  }

}
