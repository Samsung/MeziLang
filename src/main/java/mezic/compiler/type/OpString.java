package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpString extends OpObject {

  private static final long serialVersionUID = -6605954588620972340L;
  
  private static final Logger LOG = LoggerFactory.getLogger(OpString.class);

  public OpString(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "StringOp";
  }

  @Override
  public Container assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    if (!lval.isEqualType(rvalue)) {
      throw new CompileException("Type mismatch(dst: " + lval + ", src: " + rvalue + ")");
    }

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

      LOG.info(
          "PUTFIELD " + src_type.getName() + ":" + lval.getName() + "(" + sub_ref_type.getMthdDscStr() + ")");
      //// Compiled Instruction
      opinfo.mv.visitFieldInsn(Opcodes.PUTFIELD, src_type.getName(), lval.getName(), sub_ref_type.getMthdDscStr());
      //// End
      lval.setAssigned(true);
    } else if (lval.getForm() == Container.FORM_MAPELEMENT_VAR) {
      Container src_cont = lval.getOwnerContainer();
      Debug.assertion(src_cont != null, "lval_owner should be valid");
      Debug.assertion(src_cont.isTypeInitialized(), "lval_owner should be type initialized");
      Debug.assertion(src_cont.getType() instanceof TMapType, "lval_owner type should be TMapType");

      opinfo.mv.visitInsn(Opcodes.AASTORE);
    } else {
      //// Compiled Instruction
      opinfo.mv.visitVarInsn(Opcodes.ASTORE, lval.getContextVarIdx());
      LOG.info("ASTORE" + lval.getContextVarIdx());
      //// End
      lval.setAssigned(true);
    }

    return lval.getOpStackClone("anonymous");
  }

  @Override
  public Container plus(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {

    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat",
        "(Ljava/lang/String;)Ljava/lang/String;", false);
    LOG.info("INVOKEVIRTUAL: String concat");
    // added result will be on the operand stack
    //// End

    return anonyInt;
  }

  @Override
  public Container plus_assign(Container lval, Container rvalue, OpInfo opinfo) throws CompileException {
    Container anonyInt = lval.getOpStackClone("anonymous");

    //// Compiled Instruction
    opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat",
        "(Ljava/lang/String;)Ljava/lang/String;", false);
    LOG.info("INVOKEVIRTUAL: String concat");

    do_assign_common(lval, anonyInt, opinfo);
    //// End
    return anonyInt;
  }

  @Override
  public AbsType get_return_type_of_nonassign_dualchild_op(int opcode, AbsType lval_type, AbsType rval_type,
      TContext top_context) throws CompileException {
    AbsType ret_type = (AbsType) cpLoader.findClassFull("java/lang/String");
    return ret_type;
  }

  @Override
  public void load_constant(Container lval, OpInfo opinfo) throws CompileException {

    if (!lval.isConstant()) {
      throw new CompileException("Lvalue(" + lval + ") int not constant");
    }
    String str_constant = (String) lval.getContainerObject();
    opinfo.mv.visitLdcInsn(str_constant);
  }

  @Override
  public void store(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.ASTORE, index);
    LOG.info("ASTORE_" + index);
  }

  @Override
  public void load_funcstack_variable(Container lval, OpInfo opinfo, int index) throws CompileException {
    opinfo.mv.visitVarInsn(Opcodes.ALOAD, index);
    LOG.info("ALOAD_" + index);
  }

}
