package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.Container;

public interface OpLvalue extends java.io.Serializable {

  public String getOpString();

  public int getCategory();

  /* Arithmetic */
  // +
  public Container plus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // -
  public Container minus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // *
  public Container multiply(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // /
  public Container division(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // %
  public Container rest(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  /* Relational - Change to boolean */
  // <
  public Container smaller(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // >
  public Container bigger(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // <=
  public Container smaller_equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // >=
  public Container bigger_equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // ==
  public Container equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // !=
  public Container not_equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // special map access
  public Container map_access(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // special map access
  public Container map_create(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  /* Bit Operation */
  // &
  public Container and(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // ^
  public Container exc_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // |
  public Container inc_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  /* Boolean Operation */
  // &&
  public void prepare_logical_and(OpInfo opinfo) throws CompileException;

  public Container logical_and(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // ||
  public void prepare_logical_or(OpInfo opinfo) throws CompileException;

  public Container logical_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  /* Shift */
  // <<
  public Container shfit_left(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // >>
  public Container shfit_right(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  /* creating array */
  // public Container create_array(int length) throws InterpreterException;

  /* Unary Operation */
  /* used for (-1) (-a) */
  public Container unary_negative(Container lval, OpInfo opinfo) throws CompileException;

  public Container not(Container lval, OpInfo opinfo) throws CompileException;

  public Container inverse(Container lval, OpInfo opinfo) throws CompileException;

  /* assignment '=' */
  // =
  public Container assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  public void assign_rvalue_dup(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // *=
  public Container multiply_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // /=
  public Container division_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // %=
  public Container rest_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // +=
  public Container plus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // -=
  public Container minus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // <<=
  public Container shift_left_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // >>=
  public Container shift_right_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // &=
  public Container and_assign_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // ^=
  public Container exclusive_or_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  // |=
  public Container inclusive_or_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

  /* unary */
  public Container unary_plusplus(Container lval, OpInfo opinfo) throws CompileException;

  public Container unary_minusminus(Container lval, OpInfo opinfo) throws CompileException;

  /* postfix */
  public Container postfix_plusplus(Container lval, OpInfo opinfo) throws CompileException;

  public Container postfix_minusminus(Container lval, OpInfo opinfo) throws CompileException;

  public void load_constant(Container lval, OpInfo opinfo) throws CompileException;

  public void store(Container lval, OpInfo opinfo, int index) throws CompileException;

  public void store_map_element(OpInfo opinfo) throws CompileException;

  public void load_funcstack_variable(Container lval, OpInfo opinfo, int index) throws CompileException;

  public Container load_variable(Container var_cont, Container owner_cont, OpInfo opinfo) throws CompileException;

  public void dup(Container lval, OpInfo opinfo) throws CompileException;

  public void dup_x1(Container lval, OpInfo opinfo) throws CompileException;

  public void dup_x2(Container lval, OpInfo opinfo) throws CompileException;

  public void pop(Container lval, OpInfo opinfo) throws CompileException;

  public void return_variable(Container lval, OpInfo opinfo) throws CompileException;

  public void return_dummy_variable(OpInfo opinfo) throws CompileException;

  /* casting & type conversion */
  public AbsType get_return_type_of_single_op(int opcode, AbsType lval_type, TContext top_context)
      throws CompileException;

  public AbsType get_return_type_of_nonassign_dualchild_op(int opcode, AbsType lval_type, AbsType rval_type,
      TContext top_context) throws CompileException;

  public AbsType get_lval_coverting_type_of_nonassign_dual_op() throws CompileException;

  public AbsType get_boolreduce_coverting_type() throws CompileException;

  public boolean is_compatible_type(AbsType tgttype) throws CompileException;

  public AbsType get_converting_type(AbsType tgttype) throws CompileException;

  public AbsType type_convert(Container lval, AbsType tgt_type, OpInfo opinfo) throws CompileException;

  public void explicit_casting(Container lval, Container rvalue, OpInfo opinfo) throws CompileException;

}
