package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpFloat extends OpObject {

  private static final long serialVersionUID = -7320983276345826191L;
  
  private static final Logger LOG = LoggerFactory.getLogger(OpFloat.class);

  public OpFloat(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "FloatArrayOp";
  }

  @Override
  public Container assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    if (!lval.isEqualType(rvalue)) {
      throw new CompileException("Type mismatch(dst: " + lval + ", src: " + rvalue + ")");
    }

    if (lval.getForm() == Container.FORM_FUNSTACK_VAR) {
      //// Compiled Instruction
      opinfo.mv.visitVarInsn(Opcodes.FSTORE, lval.getContextVarIdx());
      LOG.info("FSTORE" + lval.getContextVarIdx());
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

      LOG.debug("FASTORE");
      opinfo.mv.visitInsn(Opcodes.FASTORE);
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
    opinfo.mv.visitInsn(Opcodes.FADD);
    LOG.info("FADD");
    // added result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container minus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    opinfo.mv.visitInsn(Opcodes.FSUB);
    LOG.info("FSUB");
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container multiply(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("FMUL");
    opinfo.mv.visitInsn(Opcodes.FMUL);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container division(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("FDIV");
    opinfo.mv.visitInsn(Opcodes.FDIV);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container rest(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("FREM");
    opinfo.mv.visitInsn(Opcodes.FREM);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container smaller(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Label not_smaller_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitInsn(Opcodes.FCMPG);
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

    opinfo.mv.visitInsn(Opcodes.FCMPL);
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

    opinfo.mv.visitInsn(Opcodes.FCMPG);
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

    opinfo.mv.visitInsn(Opcodes.FCMPL);
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

    opinfo.mv.visitInsn(Opcodes.FCMPL);
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

    opinfo.mv.visitInsn(Opcodes.FCMPL);
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

  /*
   * float does not support following operation... - and - exc_or - inc_or -
   * logical_and - logical_or - shfit_left - shfit_right - inverse - not
   */

  @Override
  /* used for (-1) (-a) */
  public Container unary_negative(Container lval, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    float float_val = -1.0f;
    LOG.info("LDC_" + float_val);
    opinfo.mv.visitLdcInsn(float_val);

    LOG.info("FMUL");
    opinfo.mv.visitInsn(Opcodes.FMUL);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container multiply_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("FMUL");
    opinfo.mv.visitInsn(Opcodes.FMUL);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container division_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("FDIV");
    opinfo.mv.visitInsn(Opcodes.FDIV);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container rest_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("FREM");
    opinfo.mv.visitInsn(Opcodes.FREM);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container plus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    LOG.info("FADD");
    opinfo.mv.visitInsn(Opcodes.FADD);

    do_assign_common(lval, anonyInt, opinfo);
    //// End
    return anonyInt;
  }

  @Override
  public Container minus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    LOG.info("FSUB");
    opinfo.mv.visitInsn(Opcodes.FSUB);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  /*
   * float does not support following operation... - shift_left_assign -
   * shift_right_assign - and_assign_assign - exclusive_or_assign -
   * inclusive_or_assign
   */

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
          LOG.info("FCONST_1");
          opinfo.mv.visitInsn(Opcodes.FCONST_1);
          LOG.info("FADD");
          opinfo.mv.visitInsn(Opcodes.FADD);
          LOG.info("DUP");
          opinfo.mv.visitInsn(Opcodes.DUP);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("FCONST_1");
          opinfo.mv.visitInsn(Opcodes.FCONST_1);
          LOG.info("FADD");
          opinfo.mv.visitInsn(Opcodes.FADD);
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
        LOG.info("FCONST_1");
        opinfo.mv.visitInsn(Opcodes.FCONST_1);

        LOG.info("FADD");
        opinfo.mv.visitInsn(Opcodes.FADD);

        LOG.info("FSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.FSTORE, lval.getContextVarIdx());

        LOG.info("FLOAD_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.FLOAD, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2"); // Map ref & Index..
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("FALOAD");
      opinfo.mv.visitInsn(Opcodes.FALOAD);

      LOG.info("FCONST_1");
      opinfo.mv.visitInsn(Opcodes.FCONST_1);
      LOG.info("FADD");
      opinfo.mv.visitInsn(Opcodes.FADD);

      dup_x2(lval, opinfo);

      LOG.debug("FASTORE");
      opinfo.mv.visitInsn(Opcodes.FASTORE);

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
          LOG.info("FCONST_1");
          opinfo.mv.visitInsn(Opcodes.FCONST_1);

          LOG.info("FSUB");
          opinfo.mv.visitInsn(Opcodes.FSUB);

          LOG.info("DUP");
          opinfo.mv.visitInsn(Opcodes.DUP);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("FCONST_1");
          opinfo.mv.visitInsn(Opcodes.FCONST_1);

          LOG.info("FSUB");
          opinfo.mv.visitInsn(Opcodes.FSUB);

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
        LOG.info("FCONST_1");
        opinfo.mv.visitInsn(Opcodes.FCONST_1);

        LOG.info("FSUB");
        opinfo.mv.visitInsn(Opcodes.FSUB);

        LOG.info("FSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.FSTORE, lval.getContextVarIdx());

        LOG.info("FLOAD_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.FLOAD, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("FALOAD");
      opinfo.mv.visitInsn(Opcodes.FALOAD);

      LOG.info("FCONST_1");
      opinfo.mv.visitInsn(Opcodes.FCONST_1);
      LOG.info("FSUB");
      opinfo.mv.visitInsn(Opcodes.FSUB);

      dup_x2(lval, opinfo);

      LOG.debug("FASTORE");
      opinfo.mv.visitInsn(Opcodes.FASTORE);

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
          LOG.info("FCONST_1");
          opinfo.mv.visitInsn(Opcodes.FCONST_1);
          LOG.info("FADD");
          opinfo.mv.visitInsn(Opcodes.FADD);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("FCONST_1");
          opinfo.mv.visitInsn(Opcodes.FCONST_1);
          LOG.info("FADD");
          opinfo.mv.visitInsn(Opcodes.FADD);

          LOG.info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }
      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");
        //// Compiled Instruction
        LOG.info("DUP");
        opinfo.mv.visitInsn(Opcodes.DUP);

        LOG.info("FCONST_1");
        opinfo.mv.visitInsn(Opcodes.FCONST_1);

        LOG.info("FADD");
        opinfo.mv.visitInsn(Opcodes.FADD);

        LOG.info("FSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.FSTORE, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("FALOAD");
      opinfo.mv.visitInsn(Opcodes.FALOAD);

      dup_x2(lval, opinfo);

      LOG.info("FCONST_1");
      opinfo.mv.visitInsn(Opcodes.FCONST_1);
      LOG.info("FADD");
      opinfo.mv.visitInsn(Opcodes.FADD);
      LOG.debug("FASTORE");
      opinfo.mv.visitInsn(Opcodes.FASTORE);

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
          LOG.info("FCONST_1");
          opinfo.mv.visitInsn(Opcodes.FCONST_1);
          LOG.info("FSUB");
          opinfo.mv.visitInsn(Opcodes.FSUB);

          LOG.info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          LOG.info("FCONST_1");
          opinfo.mv.visitInsn(Opcodes.FCONST_1);
          LOG.info("FSUB");
          opinfo.mv.visitInsn(Opcodes.FSUB);

          LOG.info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }
      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");
        //// Compiled Instruction
        LOG.info("DUP");
        opinfo.mv.visitInsn(Opcodes.DUP);

        LOG.info("FCONST_1");
        opinfo.mv.visitInsn(Opcodes.FCONST_1);

        LOG.info("FSUB");
        opinfo.mv.visitInsn(Opcodes.FSUB);

        LOG.info("FSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.FSTORE, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      LOG.debug("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      LOG.debug("FALOAD");
      opinfo.mv.visitInsn(Opcodes.FALOAD);

      dup_x2(lval, opinfo);

      LOG.info("FCONST_1");
      opinfo.mv.visitInsn(Opcodes.FCONST_1);
      LOG.info("FSUB");
      opinfo.mv.visitInsn(Opcodes.FSUB);
      LOG.debug("FASTORE");
      opinfo.mv.visitInsn(Opcodes.FASTORE);

      return lval.getOpStackClone("anonymous");
    } else {
      throw new CompileException("Invalid lval form (" + lval + ")");
    }

  }

  @Override
  public void load_constant(Container lval, OpInfo opinfo) throws CompileException {

    if (!lval.isConstant()) {
      throw new CompileException("Lvalue(" + lval + ") float not constant");
      }

    float float_val = (Float) lval.getContainerObject();

    if (float_val == 0.0f) {
      opinfo.mv.visitInsn(Opcodes.FCONST_0);
    } else if (float_val == 1.0f) {
      opinfo.mv.visitInsn(Opcodes.FCONST_1);
    } else if (float_val == 2.0f) {
      opinfo.mv.visitInsn(Opcodes.FCONST_2);
    } else {
      LOG.info("LDC_Float " + float_val);
      opinfo.mv.visitLdcInsn(float_val);
    }

  }

  @Override
  public void store(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.FSTORE, index);
    LOG.info("FSTORE_" + index);
  }

  @Override
  public void store_map_element(OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.FASTORE);
    LOG.info("FASTORE");
  }

  @Override
  public void load_funcstack_variable(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.FLOAD, index);
    LOG.info("FLOAD_" + index);
  }

  @Override
  public void return_variable(Container lval, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.FRETURN);
    LOG.info("FRETURN");
  }

  @Override
  public void return_dummy_variable(OpInfo opinfo) throws CompileException {

    opinfo.mv.visitInsn(Opcodes.FCONST_1);
    LOG.info("FCONST_1 for Float");

    opinfo.mv.visitInsn(Opcodes.FRETURN);
    LOG.info("FRETURN" + " for Float");
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
    if (tgttype.isName("java/lang/Float") || tgttype.isName("java/lang/Object")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Float");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName("java/lang/Float") || tgttype.isName("java/lang/Object")) {
      LOG.info("CHANGE F->java/lang/Float");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Float");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      LOG.info("CHANGE I->java/lang/String");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "toString", "(F)Ljava/lang/String;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else {
      throw new CompileException("Not Supported target type");
    }
  }

  @Override
  public void explicit_casting(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
    opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);

    LOG.info("CAST to F");
  }

}
