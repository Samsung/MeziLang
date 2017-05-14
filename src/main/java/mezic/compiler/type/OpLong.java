package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpLong extends OpObject {

  private static final long serialVersionUID = -6960343608612920119L;
  
  private static final Logger LOG = LoggerFactory.getLogger(OpLong.class);


  public OpLong(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "longOp";

  }

  @Override
  public int getCategory() {
    return 2;
  }

  @Override
  public Container assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    if (!lval.isEqualType(rvalue)) {
      throw new CompileException("Type mismatch(dst: " + lval + ", src: " + rvalue + ")");
    }

    if (lval.getForm() == Container.FORM_FUNSTACK_VAR) {
      //// Compiled Instruction
      opinfo.mv.visitVarInsn(Opcodes.LSTORE, lval.getContextVarIdx());
      LOG.info("LSTORE" + lval.getContextVarIdx());
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

      opinfo.mv.visitInsn(Opcodes.LASTORE);
    } else {
      throw new CompileException("Invalid lval form(" + lval + ")");
    }

    // ISSUE : is this correct ?
    return lval.getOpStackClone("anonymous");
  }

  /*
   * int does not support map operation - map_access - map_create
   */

  @Override
  public Container plus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("LADD");
    opinfo.mv.visitInsn(Opcodes.LADD);
    // added result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container minus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("LSUB");
    opinfo.mv.visitInsn(Opcodes.LSUB);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container multiply(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("LMUL");
    opinfo.mv.visitInsn(Opcodes.LMUL);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container division(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("LDIV");
    opinfo.mv.visitInsn(Opcodes.LDIV);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container rest(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("LREM");
    opinfo.mv.visitInsn(Opcodes.LREM);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  // <
  @Override
  public Container smaller(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Label not_smaller_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitInsn(Opcodes.LCMP);
    opinfo.mv.visitJumpInsn(Opcodes.IFGE, not_smaller_label);

    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(not_smaller_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_bool.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL));
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  @Override
  public Container bigger(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Label not_bigger_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitInsn(Opcodes.LCMP);
    opinfo.mv.visitJumpInsn(Opcodes.IFLE, not_bigger_label);

    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(not_bigger_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_bool.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL));
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  @Override
  public Container smaller_equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Label not_smaller_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitInsn(Opcodes.LCMP);
    opinfo.mv.visitJumpInsn(Opcodes.IFGT, not_smaller_label);

    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(not_smaller_label);
    opinfo.mv.visitInsn(Opcodes.ICONST_0);
    opinfo.mv.visitLabel(end_label);

    Container anony_bool = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    anony_bool.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL));
    anony_bool.setAssigned(true);

    return anony_bool;
  }

  @Override
  public Container bigger_equal(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Label not_bigger_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitInsn(Opcodes.LCMP);
    opinfo.mv.visitJumpInsn(Opcodes.IFLT, not_bigger_label);

    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, end_label);
    opinfo.mv.visitLabel(not_bigger_label);
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

    opinfo.mv.visitInsn(Opcodes.LCMP);
    opinfo.mv.visitJumpInsn(Opcodes.IFNE, not_eq_label);

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

    opinfo.mv.visitInsn(Opcodes.LCMP);
    opinfo.mv.visitJumpInsn(Opcodes.IFNE, not_eq_label);

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
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LAND");
    opinfo.mv.visitInsn(Opcodes.LAND);
    // subed result will be on the operand stack
    //// End
    return anonyInt;
  }

  @Override
  public Container exc_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LXOR");
    opinfo.mv.visitInsn(Opcodes.LXOR);
    // subed result will be on the operand stack
    //// End
    return anonyInt;
  }

  @Override
  public Container inc_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LOR");
    opinfo.mv.visitInsn(Opcodes.LOR);
    // subed result will be on the operand stack
    //// End
    return anonyInt;
  }

  /*
   * int does not support
   *
   * @Override public Container logical_and(Container lval, Container rvalue,
   * OpInfo opinfo) throws InterpreterException { throw new
   * InterpreterException("'logical_and' lvalue op is not supported for "
   * +toString()); }
   *
   * @Override public Container logical_or(Container lval, Container rvalue,
   * OpInfo opinfo) throws InterpreterException { throw new
   * InterpreterException("'logical_or' lvalue op is not supported for "
   * +toString()); }
   */

  @Override
  public Container shfit_left(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LSHL");
    opinfo.mv.visitInsn(Opcodes.LSHL);
    // subed result will be on the operand stack
    //// End
    return anonyInt;
  }

  @Override
  public Container shfit_right(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LSHR");
    opinfo.mv.visitInsn(Opcodes.LSHR);
    // subed result will be on the operand stack
    //// End
    return anonyInt;
  }

  @Override
  /* used for (-1) (-a) */
  public Container unary_negative(Container lval, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    long long_val = -1;
    LOG.info("LDC_" + long_val);
    opinfo.mv.visitLdcInsn(long_val);

    LOG.info("LMUL");
    opinfo.mv.visitInsn(Opcodes.LMUL);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  /*
   * int does not support 'not' operation
   *
   * @Override public Container not(Container lval, OpInfo opinfo) throws
   * InterpreterException{ throw new InterpreterException(
   * "'not' lvalue op is not supported for " +toString()); }
   */

  // ~
  @Override
  public Container inverse(Container lval, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    long long_val = -1;
    LOG.info("LDC_" + long_val);
    opinfo.mv.visitLdcInsn(long_val);

    LOG.info("LXOR");
    opinfo.mv.visitInsn(Opcodes.LXOR);
    // subed result will be on the operand stack
    //// End
    return anonyInt;
  }

  @Override
  public Container multiply_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LMUL");
    opinfo.mv.visitInsn(Opcodes.LMUL);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container division_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LDIV");
    opinfo.mv.visitInsn(Opcodes.LDIV);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container rest_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LREM");
    opinfo.mv.visitInsn(Opcodes.LREM);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container plus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("LADD");
    opinfo.mv.visitInsn(Opcodes.LADD);

    do_assign_common(lval, anonyInt, opinfo);
    //// End
    return anonyInt;
  }

  @Override
  public Container minus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LSUB");
    opinfo.mv.visitInsn(Opcodes.LSUB);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container shift_left_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LSHL");
    opinfo.mv.visitInsn(Opcodes.LSHL);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container shift_right_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LSHR");
    opinfo.mv.visitInsn(Opcodes.LSHR);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container and_assign_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LAND");
    opinfo.mv.visitInsn(Opcodes.LAND);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container exclusive_or_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LXOR");
    opinfo.mv.visitInsn(Opcodes.LXOR);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container inclusive_or_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("LOR");
    opinfo.mv.visitInsn(Opcodes.LOR);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container unary_plusplus(Container lval, OpInfo opinfo) throws CompileException {

    if (lval.isForm(Container.FORM_OPSTACK_VAR)) {

      Container org_cont = lval.getOwnerContainer();
      Debug.assertion(org_cont != null, "org_cont should be valid");
      Debug.assertion(!org_cont.isForm(Container.FORM_OPSTACK_VAR), "posfix ++ does not support opstack variable");

      if (org_cont.getForm() == Container.FORM_OBJMEMBER_VAR) {
        // 'isAssigned()' cannot be guaranteed for object member variable
        // Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned
        // status");

        Container src_cont = org_cont.getOwnerContainer();
        if (src_cont == null) {
          throw new CompileException("Invalid Src Container");
        }

        AbsType src_type = src_cont.getType();
        if (src_type == null) {
          throw new CompileException("Invalid Src Container Type");
        }

        AbsType sub_ref_type = org_cont.getType();
        if (sub_ref_type == null) {
          throw new CompileException("Invalid Sub Ref Container Type");
        }

        if (org_cont.isSingleton()) {
          LOG.info("LCONST_1");
          opinfo.mv.visitInsn(Opcodes.LCONST_1);

          LOG.info("LADD");
          opinfo.mv.visitInsn(Opcodes.LADD);

          LOG.info("DUP2");
          opinfo.mv.visitInsn(Opcodes.DUP2);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("LCONST_1");
          opinfo.mv.visitInsn(Opcodes.LCONST_1);

          LOG.info("LADD");
          opinfo.mv.visitInsn(Opcodes.LADD);

          LOG.info("DUP2_X1");
          opinfo.mv.visitInsn(Opcodes.DUP2_X1);

          LOG.info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }

      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");

        //// Compiled Instruction
        LOG.info("LCONST_1");
        opinfo.mv.visitInsn(Opcodes.LCONST_1);

        LOG.info("LADD");
        opinfo.mv.visitInsn(Opcodes.LADD);

        LOG.info("LSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.LSTORE, lval.getContextVarIdx());

        LOG.info("LLOAD_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.LLOAD, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("LALOAD");
      opinfo.mv.visitInsn(Opcodes.LALOAD);

      LOG.info("LCONST_1");
      opinfo.mv.visitInsn(Opcodes.LCONST_1);
      LOG.info("LADD");
      opinfo.mv.visitInsn(Opcodes.LADD);

      dup_x2(lval, opinfo);

      LOG.debug("LASTORE");
      opinfo.mv.visitInsn(Opcodes.LASTORE);

      return lval.getOpStackClone("anonymous");
    } else {
      throw new CompileException("Invalid lval form (" + lval + ")");
    }
  }

  @Override
  public Container unary_minusminus(Container lval, OpInfo opinfo) throws CompileException {

    if (lval.isForm(Container.FORM_OPSTACK_VAR)) {

      Container org_cont = lval.getOwnerContainer();
      Debug.assertion(org_cont != null, "org_cont should be valid");
      Debug.assertion(!org_cont.isForm(Container.FORM_OPSTACK_VAR), "posfix ++ does not support opstack variable");

      if (org_cont.getForm() == Container.FORM_OBJMEMBER_VAR) {
        // 'isAssigned()' cannot be guaranteed for object member variable
        // Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned
        // status");

        Container src_cont = org_cont.getOwnerContainer();
        if (src_cont == null) {
          throw new CompileException("Invalid Src Container");
        }

        AbsType src_type = src_cont.getType();
        if (src_type == null) {
          throw new CompileException("Invalid Src Container Type");
        }

        AbsType sub_ref_type = org_cont.getType();
        if (sub_ref_type == null) {
          throw new CompileException("Invalid Sub Ref Container Type");
        }

        if (org_cont.isSingleton()) {
          LOG.info("LCONST_1");
          opinfo.mv.visitInsn(Opcodes.LCONST_1);

          LOG.info("LSUB");
          opinfo.mv.visitInsn(Opcodes.LSUB);

          LOG.info("DUP2");
          opinfo.mv.visitInsn(Opcodes.DUP2);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("LCONST_1");
          opinfo.mv.visitInsn(Opcodes.LCONST_1);

          LOG.info("LSUB");
          opinfo.mv.visitInsn(Opcodes.LSUB);

          LOG.info("DUP2_X1");
          opinfo.mv.visitInsn(Opcodes.DUP2_X1);

          LOG.info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }

      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");

        //// Compiled Instruction
        LOG.info("LCONST_1");
        opinfo.mv.visitInsn(Opcodes.LCONST_1);

        LOG.info("LSUB");
        opinfo.mv.visitInsn(Opcodes.LSUB);

        LOG.info("LSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.LSTORE, lval.getContextVarIdx());

        LOG.info("LLOAD_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.LLOAD, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("LALOAD");
      opinfo.mv.visitInsn(Opcodes.LALOAD);

      LOG.info("LCONST_1");
      opinfo.mv.visitInsn(Opcodes.LCONST_1);
      LOG.info("LSUB");
      opinfo.mv.visitInsn(Opcodes.LSUB);

      dup_x2(lval, opinfo);

      LOG.debug("LASTORE");
      opinfo.mv.visitInsn(Opcodes.LASTORE);

      return lval.getOpStackClone("anonymous");
    } else {
      throw new CompileException("Invalid lval form (" + lval + ")");
    }
  }

  @Override
  public Container postfix_plusplus(Container lval, OpInfo opinfo) throws CompileException {

    if (lval.isForm(Container.FORM_OPSTACK_VAR)) {
      Container org_cont = lval.getOwnerContainer();
      Debug.assertion(org_cont != null, "org_cont should be valid");
      Debug.assertion(!org_cont.isForm(Container.FORM_OPSTACK_VAR), "posfix ++ does not support opstack variable");

      if (org_cont.getForm() == Container.FORM_OBJMEMBER_VAR) {
        // 'isAssigned()' cannot be guaranteed for object member variable
        // Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned
        // status");

        Container src_cont = org_cont.getOwnerContainer();
        if (src_cont == null) {
          throw new CompileException("Invalid Src Container");
        }

        AbsType src_type = src_cont.getType();
        if (src_type == null) {
          throw new CompileException("Invalid Src Container Type");
        }

        AbsType sub_ref_type = org_cont.getType();
        if (sub_ref_type == null) {
          throw new CompileException("Invalid Sub Ref Container Type");
        }

        if (org_cont.isSingleton()) {
          LOG.info("LCONST_1");
          opinfo.mv.visitInsn(Opcodes.LCONST_1);

          LOG.info("LADD");
          opinfo.mv.visitInsn(Opcodes.LADD);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("LCONST_1");
          opinfo.mv.visitInsn(Opcodes.LCONST_1);

          LOG.info("LADD");
          opinfo.mv.visitInsn(Opcodes.LADD);

          LOG.info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }

      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");

        //// Compiled Instruction
        LOG.info("DUP2");
        opinfo.mv.visitInsn(Opcodes.DUP2);

        LOG.info("LCONST_1");
        opinfo.mv.visitInsn(Opcodes.LCONST_1);

        LOG.info("LADD");
        opinfo.mv.visitInsn(Opcodes.LADD);

        LOG.info("LSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.LSTORE, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("LALOAD");
      opinfo.mv.visitInsn(Opcodes.LALOAD);

      dup_x2(lval, opinfo);

      LOG.info("LCONST_1");
      opinfo.mv.visitInsn(Opcodes.LCONST_1);
      LOG.info("LADD");
      opinfo.mv.visitInsn(Opcodes.LADD);
      LOG.debug("LASTORE");
      opinfo.mv.visitInsn(Opcodes.LASTORE);

      return lval.getOpStackClone("anonymous");
    } else {
      throw new CompileException("Invalid lval form (" + lval + ")");
    }
  }

  @Override
  public Container postfix_minusminus(Container lval, OpInfo opinfo) throws CompileException {

    if (lval.isForm(Container.FORM_OPSTACK_VAR)) {
      Container org_cont = lval.getOwnerContainer();
      Debug.assertion(org_cont != null, "org_cont should be valid");
      Debug.assertion(!org_cont.isForm(Container.FORM_OPSTACK_VAR), "posfix ++ does not support opstack variable");

      if (org_cont.getForm() == Container.FORM_OBJMEMBER_VAR) {
        // 'isAssigned()' cannot be guaranteed for object member variable
        // Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned
        // status");

        Container src_cont = org_cont.getOwnerContainer();
        if (src_cont == null) {
          throw new CompileException("Invalid Src Container");
        }

        AbsType src_type = src_cont.getType();
        if (src_type == null) {
          throw new CompileException("Invalid Src Container Type");
        }

        AbsType sub_ref_type = org_cont.getType();
        if (sub_ref_type == null) {
          throw new CompileException("Invalid Sub Ref Container Type");
        }

        if (org_cont.isSingleton()) {
          LOG.info("LCONST_1");
          opinfo.mv.visitInsn(Opcodes.LCONST_1);

          LOG.info("LSUB");
          opinfo.mv.visitInsn(Opcodes.LSUB);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("LCONST_1");
          opinfo.mv.visitInsn(Opcodes.LCONST_1);

          LOG.info("LSUB");
          opinfo.mv.visitInsn(Opcodes.LSUB);

          LOG.info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }

      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");

        //// Compiled Instruction
        LOG.info("DUP2");
        opinfo.mv.visitInsn(Opcodes.DUP2);

        LOG.info("LCONST_1");
        opinfo.mv.visitInsn(Opcodes.LCONST_1);

        LOG.info("LSUB");
        opinfo.mv.visitInsn(Opcodes.LSUB);

        LOG.info("LSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.LSTORE, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("LALOAD");
      opinfo.mv.visitInsn(Opcodes.LALOAD);

      dup_x2(lval, opinfo);

      LOG.info("LCONST_1");
      opinfo.mv.visitInsn(Opcodes.LCONST_1);
      LOG.info("LSUB");
      opinfo.mv.visitInsn(Opcodes.LSUB);
      LOG.debug("LASTORE");
      opinfo.mv.visitInsn(Opcodes.LASTORE);

      return lval.getOpStackClone("anonymous");
    } else {
      throw new CompileException("Invalid lval form (" + lval + ")");
    }
  }

  @Override
  public void load_constant(Container lval, OpInfo opinfo) throws CompileException {

    if (!lval.isConstant()) {
      throw new CompileException("Lvalue(" + lval + ") int not constant");
    }

    long long_val = (Long) lval.getContainerObject();

    if (long_val == 0) {
      opinfo.mv.visitInsn(Opcodes.LCONST_0);
      LOG.info("LCONST_" + long_val);
    } else if (long_val == 1) {
      opinfo.mv.visitInsn(Opcodes.LCONST_1);
      LOG.info("LCONST_" + long_val);
    } else if ((long_val >= 2 && long_val <= 9223372036854775807L)
        || (long_val >= -9223372036854775808L && long_val <= -1)) {
      opinfo.mv.visitLdcInsn(long_val);
      LOG.info("LDC_" + long_val);
    } else {
      throw new CompileException("Out of Integer boundary(" + long_val + ")");
    }
  }

  @Override
  public void store(Container lval, OpInfo opinfo, int index) throws CompileException {
    LOG.info("LSTORE_" + index);
    opinfo.mv.visitVarInsn(Opcodes.LSTORE, index);
  }

  @Override
  public void store_map_element(OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.LASTORE);
    LOG.info("LASTORE");
  }

  @Override
  public void load_funcstack_variable(Container lval, OpInfo opinfo, int index) throws CompileException {
    LOG.info("LLOAD_" + index);
    opinfo.mv.visitVarInsn(Opcodes.LLOAD, index);
  }

  @Override
  public void return_variable(Container lval, OpInfo opinfo) throws CompileException {
    LOG.info("LRETURN");
    opinfo.mv.visitInsn(Opcodes.LRETURN);
  }

  @Override
  public void return_dummy_variable(OpInfo opinfo) throws CompileException {

    opinfo.mv.visitInsn(Opcodes.LCONST_1);
    LOG.info("LCONST_1 for Long");

    opinfo.mv.visitInsn(Opcodes.LRETURN);
    LOG.info("LRETURN" + " for Long");
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
  public AbsType get_converting_type(AbsType tgttype) throws CompileException {
    if (tgttype.isName("java/lang/Long") || tgttype.isName("java/lang/Object")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Long");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName("java/lang/Long") || tgttype.isName("java/lang/Object")) {
      LOG.info("CHANGE J->java/lang/Long");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Long");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      LOG.info("CHANGE J->java/lang/String");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "toString", "(J)Ljava/lang/String;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else {
      throw new CompileException("Not Supported target type");
    }
  }

  @Override
  public void explicit_casting(Container casttype_cont, Container src_cont, OpInfo opinfo) throws CompileException {
    Debug.assertion(casttype_cont.isTypeInitialized(), "casttype_cont should be type initialized");
    //AbsType cast_type = casttype_cont.getType();

    Debug.assertion(src_cont.isTypeInitialized(), "src_cont should be type initialized");
    AbsType src_type = src_cont.getType();

    if (src_type.isName("java/lang/Object")) {
      LOG.info("CAST to J");
      opinfo.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
    } else if (src_type.isName(TPrimitiveClass.NAME_INT)) {
      LOG.info("CAST to J");
      opinfo.mv.visitInsn(Opcodes.I2L);
    } else {
      throw new CompileException("Not Supported cast src type(" + src_type + ")");
    }

  }
}
