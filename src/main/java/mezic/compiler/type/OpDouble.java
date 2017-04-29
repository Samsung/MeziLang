package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

public class OpDouble extends OpObject {

  private static final long serialVersionUID = 9141961605464582440L;

  public OpDouble(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "DoubleOp";
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
      opinfo.mv.visitVarInsn(Opcodes.DSTORE, lval.getContextVarIdx());
      Debug.println_info("DSTORE" + lval.getContextVarIdx());
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

      Debug.println_dbg("DASTORE");
      opinfo.mv.visitInsn(Opcodes.DASTORE);
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
    opinfo.mv.visitInsn(Opcodes.DADD);
    Debug.println_info("DADD");
    // added result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container minus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    opinfo.mv.visitInsn(Opcodes.DSUB);
    Debug.println_info("DSUB");
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container multiply(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    Debug.println_info("DMUL");
    opinfo.mv.visitInsn(Opcodes.DMUL);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container division(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    Debug.println_info("DDIV");
    opinfo.mv.visitInsn(Opcodes.DDIV);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container rest(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    Debug.println_info("DREM");
    opinfo.mv.visitInsn(Opcodes.DREM);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container smaller(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Label not_smaller_label = new Label();
    Label end_label = new Label();

    opinfo.mv.visitInsn(Opcodes.DCMPG);
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

    opinfo.mv.visitInsn(Opcodes.DCMPL);
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

    opinfo.mv.visitInsn(Opcodes.DCMPG);
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

    opinfo.mv.visitInsn(Opcodes.DCMPL);
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

    opinfo.mv.visitInsn(Opcodes.DCMPL);
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

    opinfo.mv.visitInsn(Opcodes.DCMPL);
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
   * double does not support following operation... - and - exc_or - inc_or -
   * logical_and - logical_or - shfit_left - shfit_right - inverse - not
   */

  @Override
  /* used for (-1) (-a) */
  public Container unary_negative(Container lval, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    double double_val = -1.0f;
    Debug.println_info("LDC_" + double_val);
    opinfo.mv.visitLdcInsn(double_val);

    Debug.println_info("DMUL");
    opinfo.mv.visitInsn(Opcodes.DMUL);
    // subed result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container multiply_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    Debug.println_info("DMUL");
    opinfo.mv.visitInsn(Opcodes.DMUL);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container division_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    Debug.println_info("DDIV");
    opinfo.mv.visitInsn(Opcodes.DDIV);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container rest_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    Debug.println_info("DREM");
    opinfo.mv.visitInsn(Opcodes.DREM);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  @Override
  public Container plus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    Debug.println_info("DADD");
    opinfo.mv.visitInsn(Opcodes.DADD);

    do_assign_common(lval, anonyInt, opinfo);
    //// End
    return anonyInt;
  }

  @Override
  public Container minus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");
    //// Compiled Instruction
    Debug.println_info("DSUB");
    opinfo.mv.visitInsn(Opcodes.DSUB);

    do_assign_common(lval, anonyInt, opinfo);

    //// End
    return anonyInt;
  }

  /*
   * double does not support following operation... - shift_left_assign -
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
          Debug.println_info("DCONST_1");
          opinfo.mv.visitInsn(Opcodes.DCONST_1);
          Debug.println_info("DADD");
          opinfo.mv.visitInsn(Opcodes.DADD);
          Debug.println_info("DUP2");
          opinfo.mv.visitInsn(Opcodes.DUP2);

          Debug.println_info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          Debug.println_info("DCONST_1");
          opinfo.mv.visitInsn(Opcodes.DCONST_1);
          Debug.println_info("DADD");
          opinfo.mv.visitInsn(Opcodes.DADD);
          Debug.println_info("DUP2_X1");
          opinfo.mv.visitInsn(Opcodes.DUP2_X1);

          Debug.println_info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }

      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");

        //// Compiled Instruction
        Debug.println_info("DCONST_1");
        opinfo.mv.visitInsn(Opcodes.DCONST_1);

        Debug.println_info("DADD");
        opinfo.mv.visitInsn(Opcodes.DADD);

        Debug.println_info("DSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.DSTORE, lval.getContextVarIdx());

        Debug.println_info("DLOAD_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.DLOAD, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      Debug.println_dbg("DUP2"); // Map ref & Index..
      opinfo.mv.visitInsn(Opcodes.DUP2);
      Debug.println_dbg("DALOAD");
      opinfo.mv.visitInsn(Opcodes.DALOAD);

      Debug.println_info("DCONST_1");
      opinfo.mv.visitInsn(Opcodes.DCONST_1);
      Debug.println_info("DADD");
      opinfo.mv.visitInsn(Opcodes.DADD);

      dup_x2(lval, opinfo);

      Debug.println_dbg("DASTORE");
      opinfo.mv.visitInsn(Opcodes.DASTORE);

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
          Debug.println_info("DCONST_1");
          opinfo.mv.visitInsn(Opcodes.DCONST_1);

          Debug.println_info("DSUB");
          opinfo.mv.visitInsn(Opcodes.DSUB);

          Debug.println_info("DUP2");
          opinfo.mv.visitInsn(Opcodes.DUP2);

          Debug.println_info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          Debug.println_info("DCONST_1");
          opinfo.mv.visitInsn(Opcodes.DCONST_1);

          Debug.println_info("DSUB");
          opinfo.mv.visitInsn(Opcodes.DSUB);

          Debug.println_info("DUP2_X1");
          opinfo.mv.visitInsn(Opcodes.DUP2_X1);

          Debug.println_info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }

      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");

        //// Compiled Instruction
        Debug.println_info("DCONST_1");
        opinfo.mv.visitInsn(Opcodes.DCONST_1);

        Debug.println_info("DSUB");
        opinfo.mv.visitInsn(Opcodes.DSUB);

        Debug.println_info("DSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.DSTORE, lval.getContextVarIdx());

        Debug.println_info("DLOAD_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.DLOAD, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      Debug.println_dbg("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      Debug.println_dbg("DALOAD");
      opinfo.mv.visitInsn(Opcodes.DALOAD);

      Debug.println_info("DCONST_1");
      opinfo.mv.visitInsn(Opcodes.DCONST_1);
      Debug.println_info("DSUB");
      opinfo.mv.visitInsn(Opcodes.DSUB);

      dup_x2(lval, opinfo);

      Debug.println_dbg("DASTORE");
      opinfo.mv.visitInsn(Opcodes.DASTORE);

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
          Debug.println_info("DCONST_1");
          opinfo.mv.visitInsn(Opcodes.DCONST_1);
          Debug.println_info("DADD");
          opinfo.mv.visitInsn(Opcodes.DADD);

          Debug.println_info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          Debug.println_info("DCONST_1");
          opinfo.mv.visitInsn(Opcodes.DCONST_1);
          Debug.println_info("DADD");
          opinfo.mv.visitInsn(Opcodes.DADD);

          Debug.println_info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }
      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");
        //// Compiled Instruction
        Debug.println_info("DUP2");
        opinfo.mv.visitInsn(Opcodes.DUP2);

        Debug.println_info("DCONST_1");
        opinfo.mv.visitInsn(Opcodes.DCONST_1);

        Debug.println_info("DADD");
        opinfo.mv.visitInsn(Opcodes.DADD);

        Debug.println_info("DSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.DSTORE, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      Debug.println_dbg("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      Debug.println_dbg("DALOAD");
      opinfo.mv.visitInsn(Opcodes.DALOAD);

      dup_x2(lval, opinfo);

      Debug.println_info("DCONST_1");
      opinfo.mv.visitInsn(Opcodes.DCONST_1);
      Debug.println_info("DADD");
      opinfo.mv.visitInsn(Opcodes.DADD);
      Debug.println_dbg("DASTORE");
      opinfo.mv.visitInsn(Opcodes.DASTORE);

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
          Debug.println_info("DCONST_1");
          opinfo.mv.visitInsn(Opcodes.DCONST_1);
          Debug.println_info("DSUB");
          opinfo.mv.visitInsn(Opcodes.DSUB);

          Debug.println_info(
              "PUTSTATIC " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTSTATIC, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        } else {
          Debug.println_info("DCONST_1");
          opinfo.mv.visitInsn(Opcodes.DCONST_1);
          Debug.println_info("DSUB");
          opinfo.mv.visitInsn(Opcodes.DSUB);

          Debug.println_info(
              "PUTFIELD " + src_type.getName() + ":" + org_cont.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
          opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), org_cont.getName(),
              sub_ref_type.getMthdDscStr());
        }
      } else {
        Debug.assertion(org_cont.isAssigned(), "org_cont should be assigned status");
        //// Compiled Instruction
        Debug.println_info("DUP2");
        opinfo.mv.visitInsn(Opcodes.DUP2);

        Debug.println_info("DCONST_1");
        opinfo.mv.visitInsn(Opcodes.DCONST_1);

        Debug.println_info("DSUB");
        opinfo.mv.visitInsn(Opcodes.DSUB);

        Debug.println_info("DSTORE_" + lval.getContextVarIdx());
        opinfo.mv.visitVarInsn(Opcodes.DSTORE, lval.getContextVarIdx());
        //// End
      }

      // ISSUE : is this correct ?
      return org_cont.getOpStackClone("anonymous");
    } else if (lval.isForm(Container.FORM_MAPELEMENT_VAR)) {
      Debug.println_dbg("DUP2");
      opinfo.mv.visitInsn(Opcodes.DUP2);
      Debug.println_dbg("DALOAD");
      opinfo.mv.visitInsn(Opcodes.DALOAD);

      dup_x2(lval, opinfo);

      Debug.println_info("DCONST_1");
      opinfo.mv.visitInsn(Opcodes.DCONST_1);
      Debug.println_info("DSUB");
      opinfo.mv.visitInsn(Opcodes.DSUB);
      Debug.println_dbg("DASTORE");
      opinfo.mv.visitInsn(Opcodes.DASTORE);

      return lval.getOpStackClone("anonymous");
    } else {
      throw new CompileException("Invalid lval form (" + lval + ")");
    }

  }

  @Override
  public void load_constant(Container lval, OpInfo opinfo) throws CompileException {

    if (!lval.isConstant()) {
      throw new CompileException("Lvalue(" + lval + ") double not constant");
    }

    double double_val = (Double) lval.getContainerObject();

    if (double_val == 0.0f) {
      opinfo.mv.visitInsn(Opcodes.DCONST_0);
    } else if (double_val == 1.0f) {
      opinfo.mv.visitInsn(Opcodes.DCONST_1);
    } else {
      Debug.println_info("LDC_Double " + double_val);
      opinfo.mv.visitLdcInsn(double_val);
    }

  }

  @Override
  public void store(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.DSTORE, index);
    Debug.println_info("DSTORE_" + index);
  }

  @Override
  public void store_map_element(OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.DASTORE);
    Debug.println_info("DASTORE");
  }

  @Override
  public void load_funcstack_variable(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.DLOAD, index);
    Debug.println_info("DLOAD_" + index);
  }

  @Override
  public void return_variable(Container lval, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(Opcodes.DRETURN);
    Debug.println_info("DRETURN");
  }

  @Override
  public void return_dummy_variable(OpInfo opinfo) throws CompileException {

    opinfo.mv.visitInsn(Opcodes.DCONST_1);
    Debug.println_info("DCONST_1 for Double");

    opinfo.mv.visitInsn(Opcodes.DRETURN);
    Debug.println_info("DRETURN" + " for Double");
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
    if (tgttype.isName("java/lang/Double") || tgttype.isName("java/lang/Object")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Double");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container lval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName("java/lang/Double") || tgttype.isName("java/lang/Object")) {
      Debug.println_info("CHANGE D->java/lang/Double");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/Double");
      return type;
    } else if (tgttype.isName("java/lang/String")) {
      Debug.println_info("CHANGE D->java/lang/String");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "toString", "(D)Ljava/lang/String;", false);

      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else {
      throw new CompileException("Not Supported target type");
    }
  }

  @Override
  public void explicit_casting(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    opinfo.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
    opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);

    Debug.println_info("CAST to D");
  }

}
