package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpInt extends OpObject {

  private static final long serialVersionUID = -8947329888184841126L;
  
  private static final Logger LOG = LoggerFactory.getLogger(OpInt.class);

  public OpInt(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "intOp";

  }

  @Override
  public Container assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    // type checking should be completed in SubSymbol Resolving
    // if( ! lval.isEqualType( rvalue) )
    // throw new InterpreterException("Type mismatch(dst: "+lval+", src: "
    // +rvalue+")");

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

      LOG.debug("IASTORE");
      opinfo.mv.visitInsn(Opcodes.IASTORE);
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
    opinfo.mv.visitInsn(Opcodes.IADD);
    LOG.info("IADD");
    // added result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container minus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    opinfo.mv.visitInsn(Opcodes.ISUB);
    LOG.info("ISUB");
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container multiply(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("IMUL");
    opinfo.mv.visitInsn(Opcodes.IMUL);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container division(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("IDIV");
    opinfo.mv.visitInsn(Opcodes.IDIV);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container rest(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("IREM");
    opinfo.mv.visitInsn(Opcodes.IREM);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

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

  @Override
  public Container and(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("IAND");
    opinfo.mv.visitInsn(Opcodes.IAND);
    // subed result will be on the operand stack
    //// End
    return anonyInt;
  }

  @Override
  public Container exc_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("IXOR");
    opinfo.mv.visitInsn(Opcodes.IXOR);
    // subed result will be on the operand stack
    //// End
    return anonyInt;
  }

  @Override
  public Container inc_or(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("IOR");
    opinfo.mv.visitInsn(Opcodes.IOR);
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
    LOG.info("ISHL");
    opinfo.mv.visitInsn(Opcodes.ISHL);
    // subed result will be on the operand stack
    //// End
    return anonyInt;
  }

  @Override
  public Container shfit_right(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("ISHR");
    opinfo.mv.visitInsn(Opcodes.ISHR);
    // subed result will be on the operand stack
    //// End
    return anonyInt;
  }

  @Override
  /* used for (-1) (-a) */
  public Container unary_negative(Container lval, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("ICONST_M1");
    opinfo.mv.visitInsn(Opcodes.ICONST_M1);

    LOG.info("IMUL");
    opinfo.mv.visitInsn(Opcodes.IMUL);
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
    LOG.info("ICONST_M1");
    opinfo.mv.visitInsn(Opcodes.ICONST_M1);

    LOG.info("IXOR");
    opinfo.mv.visitInsn(Opcodes.IXOR);
    // subed result will be on the operand stack
    //// End
    return anonyInt;
  }

  @Override
  public Container multiply_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("IMUL");
    opinfo.mv.visitInsn(Opcodes.IMUL);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container division_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("IDIV");
    opinfo.mv.visitInsn(Opcodes.IDIV);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container rest_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("IREM");
    opinfo.mv.visitInsn(Opcodes.IREM);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container plus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("IADD");
    opinfo.mv.visitInsn(Opcodes.IADD);

    do_assign_common(lval, anonyInt, opinfo);
    //// End
    return anonyInt;
  }

  @Override
  public Container minus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("ISUB");
    opinfo.mv.visitInsn(Opcodes.ISUB);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container shift_left_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("ISHL");
    opinfo.mv.visitInsn(Opcodes.ISHL);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container shift_right_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("ISHR");
    opinfo.mv.visitInsn(Opcodes.ISHR);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container and_assign_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("IAND");
    opinfo.mv.visitInsn(Opcodes.IAND);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container exclusive_or_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("IXOR");
    opinfo.mv.visitInsn(Opcodes.IXOR);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container inclusive_or_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("IOR");
    opinfo.mv.visitInsn(Opcodes.IOR);

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
          LOG.info("ICONST_1");
          opinfo.mv.visitInsn(Opcodes.ICONST_1);
          LOG.info("IADD");
          opinfo.mv.visitInsn(Opcodes.IADD);
          LOG.info("DUP");
          opinfo.mv.visitInsn(Opcodes.DUP);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("ICONST_1");
          opinfo.mv.visitInsn(Opcodes.ICONST_1);
          LOG.info("IADD");
          opinfo.mv.visitInsn(Opcodes.IADD);
          LOG.info("DUP_X1");
          opinfo.mv.visitInsn(Opcodes.DUP_X1);

          LOG.info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }

      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");

        //// Compiled Instruction
        LOG.info("POP for ++a");
        opinfo.mv.visitInsn(Opcodes.POP);
        LOG.info("IINC" + org_cont.getContextVarIdx() + " with 1  for ++a");
        opinfo.mv.visitIincInsn(org_cont.getContextVarIdx(), 1);
        LOG.info("ILOAD for ++a");
        opinfo.mv.visitVarInsn(Opcodes.ILOAD, org_cont.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2"); // Map ref & Index..
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("IALOAD");
      opinfo.mv.visitInsn(Opcodes.IALOAD);

      LOG.info("ICONST_1");
      opinfo.mv.visitInsn(Opcodes.ICONST_1);
      LOG.info("IADD");
      opinfo.mv.visitInsn(Opcodes.IADD);

      dup_x2(lval, opinfo);

      LOG.debug("IASTORE");
      opinfo.mv.visitInsn(Opcodes.IASTORE);

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
          LOG.info("ICONST_1");
          opinfo.mv.visitInsn(Opcodes.ICONST_1);

          LOG.info("ISUB");
          opinfo.mv.visitInsn(Opcodes.ISUB);

          LOG.info("DUP");
          opinfo.mv.visitInsn(Opcodes.DUP);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("ICONST_1");
          opinfo.mv.visitInsn(Opcodes.ICONST_1);

          LOG.info("ISUB");
          opinfo.mv.visitInsn(Opcodes.ISUB);

          LOG.info("DUP_X1");
          opinfo.mv.visitInsn(Opcodes.DUP_X1);

          LOG.info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }

      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");

        //// Compiled Instruction
        LOG.info("POP for --a");
        opinfo.mv.visitInsn(Opcodes.POP);

        LOG.info("IINC" + org_cont.getContextVarIdx() + " with -1  for --a");
        opinfo.mv.visitIincInsn(org_cont.getContextVarIdx(), -1);

        LOG.info("ILOAD for --a");
        opinfo.mv.visitVarInsn(Opcodes.ILOAD, org_cont.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("IALOAD");
      opinfo.mv.visitInsn(Opcodes.IALOAD);

      LOG.info("ICONST_1");
      opinfo.mv.visitInsn(Opcodes.ICONST_1);
      LOG.info("ISUB");
      opinfo.mv.visitInsn(Opcodes.ISUB);

      dup_x2(lval, opinfo);

      LOG.debug("IASTORE");
      opinfo.mv.visitInsn(Opcodes.IASTORE);

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
          LOG.info("ICONST_1");
          opinfo.mv.visitInsn(Opcodes.ICONST_1);
          LOG.info("IADD");
          opinfo.mv.visitInsn(Opcodes.IADD);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("ICONST_1");
          opinfo.mv.visitInsn(Opcodes.ICONST_1);
          LOG.info("IADD");
          opinfo.mv.visitInsn(Opcodes.IADD);

          LOG.info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }
      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");
        //// Compiled Instruction
        LOG.info("IINC" + org_cont.getContextVarIdx() + " with 1");
        opinfo.mv.visitIincInsn(org_cont.getContextVarIdx(), 1);
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("IALOAD");
      opinfo.mv.visitInsn(Opcodes.IALOAD);

      dup_x2(lval, opinfo);

      LOG.info("ICONST_1");
      opinfo.mv.visitInsn(Opcodes.ICONST_1);
      LOG.info("IADD");
      opinfo.mv.visitInsn(Opcodes.IADD);
      LOG.debug("IASTORE");
      opinfo.mv.visitInsn(Opcodes.IASTORE);

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
          LOG.info("ICONST_1");
          opinfo.mv.visitInsn(Opcodes.ICONST_1);
          LOG.info("ISUB");
          opinfo.mv.visitInsn(Opcodes.ISUB);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("ICONST_1");
          opinfo.mv.visitInsn(Opcodes.ICONST_1);
          LOG.info("ISUB");
          opinfo.mv.visitInsn(Opcodes.ISUB);

          LOG.info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }
      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");
        //// Compiled Instruction
        LOG.info("IINC" + org_cont.getContextVarIdx() + " with -1");
        opinfo.mv.visitIincInsn(org_cont.getContextVarIdx(), -1);
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("IALOAD");
      opinfo.mv.visitInsn(Opcodes.IALOAD);

      dup_x2(lval, opinfo);

      LOG.info("ICONST_1");
      opinfo.mv.visitInsn(Opcodes.ICONST_1);
      LOG.info("ISUB");
      opinfo.mv.visitInsn(Opcodes.ISUB);
      LOG.debug("IASTORE");
      opinfo.mv.visitInsn(Opcodes.IASTORE);

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

    int int_val = (Integer) lval.getContainerObject();

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
    } else if ((int_val >= 128 && int_val <= 32767) || (int_val >= -32768 && int_val <= -129)) {
      opinfo.mv.visitIntInsn(Opcodes.SIPUSH, int_val);
      LOG.info("SIPUSH_" + int_val);
    } else if ((int_val >= 32768 && int_val <= 2147483647) || (int_val >= -2147483648 && int_val <= -32769)) {
      opinfo.mv.visitLdcInsn(int_val);
      LOG.info("LDC_" + int_val);
    } else {
      throw new CompileException("Out of Integer boundary(" + int_val + ")");
    }
  }

  @Override
  public void store(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.ISTORE, index);
    LOG.info("ISTORE_" + index);
  }

  @Override
  public void store_map_element(OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.IASTORE);
    LOG.info("IASTORE");
  }

  @Override
  public void load_funcstack_variable(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.ILOAD, index);
    LOG.info("ILOAD_" + index);
  }

  @Override
  public void return_variable(Container lval, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.IRETURN);
    LOG.info("IRETURN");
  }

  @Override
  public void return_dummy_variable(OpInfo opinfo) throws CompileException {

    opinfo.mv.visitInsn(Opcodes.ICONST_1);
    LOG.info("ICONST_1 for Int");

    opinfo.mv.visitInsn(Opcodes.IRETURN);
    LOG.info("IRETURN" + " for Int");
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
    if (tgttype.isName("java/lang/Integer") || tgttype.isName("java/lang/Object")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Integer");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_BYTE)) {
      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BYTE);
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_CHAR)) {
      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_CHAR);
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_SHORT)) {
      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_SHORT);
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_LONG)) {
      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_LONG);
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_FLOAT)) {
      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_FLOAT);
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_DOUBLE)) {
      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_DOUBLE);
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName("java/lang/Integer") || tgttype.isName("java/lang/Object")) {
      LOG.info("CHANGE I->java/lang/Integer");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Integer");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      LOG.info("CHANGE I->java/lang/String");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_BYTE)) {
      LOG.info("CHANGE I->B");
      opinfo.mv.visitInsn(Opcodes.I2B);

      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BYTE);
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_CHAR)) {
      LOG.info("CHANGE I->C");
      opinfo.mv.visitInsn(Opcodes.I2C);

      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_CHAR);
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_SHORT)) {
      LOG.info("CHANGE I->S");
      opinfo.mv.visitInsn(Opcodes.I2S);

      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_SHORT);
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_LONG)) {
      LOG.info("CHANGE I->L");
      opinfo.mv.visitInsn(Opcodes.I2L);

      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_LONG);
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_FLOAT)) {
      LOG.info("CHANGE I->F");
      opinfo.mv.visitInsn(Opcodes.I2F);

      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_FLOAT);
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_DOUBLE)) {
      LOG.info("CHANGE I->D");
      opinfo.mv.visitInsn(Opcodes.I2D);

      AbsType type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_DOUBLE);
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
      LOG.info("CAST to I");
      opinfo.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
    } else if (src_type.isName(TPrimitiveClass.NAME_LONG)) {
      LOG.info("CAST to I");
      opinfo.mv.visitInsn(Opcodes.L2I);
    } else if (src_type.isName(TPrimitiveClass.NAME_DOUBLE)) {
      LOG.info("CAST to I");
      opinfo.mv.visitInsn(Opcodes.D2I);
    } else {
      throw new CompileException("Not Supported cast src type(" + src_type + ")");
    }
  }

}
