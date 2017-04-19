package mezic.compiler.type;

import java.util.List;

import mezic.compiler.CompileException;
import mezic.compiler.Compiler;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class OpFunction extends OpObject {

  private static final long serialVersionUID = -7302343190978333299L;

  public OpFunction(CompilerLoader cpLoader) {
    super(cpLoader);
  }

  @Override
  public String getOpString() {
    return "FunctionOp";
  }

  @Override
  public AbsType get_converting_type(AbsType tgttype) throws CompileException {
    if (tgttype.isName("java/lang/String")) {
      AbsType type = (AbsType) cpLoader.findClassFull("java/lang/String");
      return type;
    } else if (tgttype.isName(TPrimitiveClass.NAME_VOID)) { // for null
      return tgttype;
    } else if (tgttype instanceof AbsClassType && ((AbsClassType) tgttype).isFunctionalInterface()) {
      return tgttype;
    }

    return null;
  }

  @Override
  public AbsType type_convert(Container rval, AbsType tgttype, OpInfo opinfo) throws CompileException {
    if (tgttype.isName("java/lang/String")) {
      Debug.println_info("java/lang/Object->java/lang/String");

      return string_type_convert(rval, opinfo);
    } else if (tgttype.isName(TPrimitiveClass.NAME_VOID)) { // for null
      return tgttype;
    } else if (tgttype instanceof AbsClassType && ((AbsClassType) tgttype).isFunctionalInterface()) {

      mh_2_functional_if_impl(rval, (AbsClassType) tgttype, opinfo);
      return tgttype;
    }

    throw new CompileException("not supported operation for " + toString());
  }

  private void mh_2_functional_if_impl(Container r_mh_val, AbsClassType tgt_if_type, OpInfo opinfo)
      throws CompileException {
    AbsType rval_type = r_mh_val.getType();
    Debug.assertion(rval_type != null, "laval_type should be valid");
    Debug.assertion(rval_type instanceof TMethodHandle, "laval_type should be TMethodHandle, but " + rval_type);

    TMethodHandle r_mh_type = (TMethodHandle) rval_type;
    FuncSignatureDesc r_mh_desc = r_mh_type.getFuncSignatureDesc();

    AbsFuncType tgt_if_abs_method = get_functional_if_abs_mthd(tgt_if_type, r_mh_desc.getParameterTypeList().size());

    if (tgt_if_abs_method == null) {
      throw new CompileException("Functional Interface does not have valid abstract method(" + tgt_if_type + ")");
    }

    FuncSignatureDesc tgt_if_desc = tgt_if_abs_method.getFuncSignatureDesc();

    // class FunctionalInterfaceImpl implement FunctionalInterface
    // {
    // mh : MethodHandle
    // fn func(arg: [some type] ... )-> [some type] // tgt_if_type
    // {
    // return mh.invoke(arg ...) // r_mh_val
    // }
    // }
    // so... parameters of tgt_if_type should be compatible to those of r_mh_val
    // return value of 'mh.invoke(arg ...)' should be compatible to that of
    // tgt_if_type

    Debug.println_dbg("compatibility checking " + tgt_if_desc + " -> " + r_mh_desc);

    // parameter type implied castibility checking (tgt_if_desc -> r_mh_desc)
    if (!cpLoader.isImpliedCastibleParameterList(tgt_if_desc.getParameterTypeList(), r_mh_desc.getParameterTypeList())) {
      throw new CompileException(tgt_if_abs_method + " parameter is not compatible to " + r_mh_type);
    }

    // return type implied castibility checking (r_mh_desc -> tgt_if_desc)
    // if( !cpLoader.isImpliedCastible(tgt_if_desc.getReturnType(),
    // r_mh_desc.getReturnType()) )
    if (!cpLoader.isImpliedCastible(r_mh_desc.getReturnType(), tgt_if_desc.getReturnType())) {
      throw new CompileException(r_mh_type + "return type is not compatible to " + tgt_if_abs_method);
    }

    TContextFunc func_context = (TContextFunc) opinfo.top_context.getClosestAncestor(AbsType.FORM_FUNC);
    Debug.assertion(func_context != null, "func_context should be valid");

    TContextClass class_context = (TContextClass) func_context.getClosestAncestor(AbsType.FORM_CLASS);
    Debug.assertion(class_context != null, "class_context should be valid");

    String func_if_implclass_name = class_context.getName() + "_"
        + (func_context.getName().equals("<init>") ? "init" : func_context.getName())
        + ((TContextFunc) func_context).allocFunctionalIfImplName();

    Debug.println_dbg("generating [" + func_if_implclass_name + "]");

    gen_functional_if_impl_class(func_if_implclass_name, tgt_if_type, tgt_if_abs_method, r_mh_type);

    AbsType func_if_impl_type = (AbsType) cpLoader.findClassFull(func_if_implclass_name);
    Debug.assertion(func_if_impl_type != null, "func_if_impl_type should be valid");

    // instantiate functional interface impl class
    Debug.println_info("Opcodes.NEW " + func_if_impl_type.getName());
    Debug.println_info("DUP_X1");
    Debug.println_info("SWAP");
    Debug.println_info("INVOKESPECIAL" + func_if_impl_type.getName());

    opinfo.mv.visitTypeInsn(Opcodes.NEW, func_if_impl_type.getName());
    // opstack already had method handle reference(parameter of functional
    // interface impl class constructor)
    opinfo.mv.visitInsn(Opcodes.DUP_X1); // dup closure class (cat. 1)
    opinfo.mv.visitInsn(Opcodes.SWAP); // dup closure class (cat. 1)
    String constructor_sig = "(" + TMethodHandle.METHODHANDLE_CLASS_DESC + ")V";
    opinfo.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, func_if_impl_type.getName(), "<init>", constructor_sig, false);

    opinfo.mv.visitTypeInsn(Opcodes.CHECKCAST, ((AbsType) tgt_if_type).getMthdDscStr());

    // Debug.stop();
  }

  private AbsFuncType get_functional_if_abs_mthd(AbsClassType if_type, int num_params) throws CompileException {
    Debug.println_dbg(" get_functional_if_abs_mthd(" + if_type + ") num_params(" + num_params + ")");

    Debug.assertion(if_type.isFunctionalInterface(), "if_type should be functional interface, but " + if_type);

    List<AbsFuncType> func_list = if_type.getLocalFunctions();

    AbsFuncType functIfAbsMthd = null;

    for (AbsFuncType func_type : func_list) {

      Debug.println_dbg(" func_type:" + func_type + ":" + (func_type.is_abstract() ? "abstract" : "") + "("
          + func_type.getFuncSignatureDesc() + ")");

      if (!func_type.is_abstract()) {
        continue; // may be default method in Java 8
                  // (interface can have function body for default method)
      }
      if (func_type.getFuncSignatureDesc().getParameterTypeList().size() != num_params) {
        continue;
      }

      if (functIfAbsMthd != null) {
        throw new CompileException("Functional Interface has duplicated method definition(" + if_type + ")");
      }

      functIfAbsMthd = func_type;
    }

    if (functIfAbsMthd != null) {
      return functIfAbsMthd;
    }

    List<AbsClassType> impl_if_list = if_type.getInterfaceList();

    for (AbsClassType impl_if : impl_if_list) {
      functIfAbsMthd = get_functional_if_abs_mthd(impl_if, num_params);

      if (functIfAbsMthd != null) {
        return functIfAbsMthd;
      }
    }

    // throw new CompileException("Functional Interface does not have valid
    // method definition(" + if_type+")");
    return null;
  }

  private void gen_functional_if_impl_class(String class_name, AbsClassType if_type, AbsFuncType if_abs_method,
      TMethodHandle r_mh_type) throws CompileException {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

    String[] interfaces = new String[] { ((AbsType) if_type).getName() };

    cw.visit(Compiler.java_version, Opcodes.ACC_PUBLIC, class_name, null, "java/lang/Object", interfaces);

    final String mh_field_name = "mh";

    // regitser method handle field
    cw.visitField(Opcodes.ACC_PUBLIC, mh_field_name, TMethodHandle.METHODHANDLE_CLASS_DESC, null, null).visitEnd();

    // default constructor
    String constructor_sig = "(" + TMethodHandle.METHODHANDLE_CLASS_DESC + ")V";
    MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", constructor_sig, null, null);
    mv.visitCode();

    Debug.println_info("ALOAD 0 for this");
    Debug.println_info("INVOKESPECIAL java/lang/Object");

    mv.visitVarInsn(Opcodes.ALOAD, 0); // this
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    if (Debug.enable_compile_debug_print) {
      mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
      mv.visitLdcInsn(class_name + " was instantiated");
      mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    mv.visitVarInsn(Opcodes.ALOAD, 0); // this
    mv.visitVarInsn(Opcodes.ALOAD, 1); // method handle parameter
    mv.visitFieldInsn(Opcodes.PUTFIELD, class_name, mh_field_name, TMethodHandle.METHODHANDLE_CLASS_DESC);

    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // abstract method implementation
    mv = cw.visitMethod(if_abs_method.get_access(), ((AbsType) if_abs_method).getName(),
        ((AbsType) if_abs_method).getMthdDscStr(), null, null);
    mv.visitCode();

    // load mh field
    mv.visitVarInsn(Opcodes.ALOAD, 0); // this
    mv.visitFieldInsn(Opcodes.GETFIELD, class_name, mh_field_name, TMethodHandle.METHODHANDLE_CLASS_DESC);

    // load all parameters
    AbsTypeList paratype_list = if_abs_method.getFuncSignatureDesc().getParameterTypeList();

    int local_var_idx = 1;
    OpInfo opinfo = new OpInfo(mv);

    for (AbsType para_type : paratype_list) {
      para_type.op().load_funcstack_variable(null, opinfo, local_var_idx);
      local_var_idx += (para_type.op().getCategory());
    }

    Debug.println_dbg("Method Handle Call : " + r_mh_type);

    FuncSignatureDesc funcsig = r_mh_type.getFuncSignatureDesc();
    Debug.assertion(funcsig != null, "fucsig should be valid");

    Debug.println_info("INVOKEVIRTUAL java/lang/invoke/MethodHandle/invoke");
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", funcsig.getMthdDscStr(),
        false);

    // return processing
    AbsType r_mh_ret_type = funcsig.getReturnType();
    AbsType if_ret_type = if_abs_method.getFuncSignatureDesc().getReturnType();

    if (cpLoader.isCompatibleClass(r_mh_ret_type, if_ret_type)) {
      // do nothing
    } else if (cpLoader.isConvertibleClass(r_mh_ret_type, if_ret_type)) {
      // assign_rval_node.setConvertingType(l_value_type);
      Container lval = new Container("anonymous", Container.FORM_OPSTACK_VAR, false, false);
      lval.initializeType(r_mh_ret_type);
      lval.setAssigned(true);
      r_mh_ret_type.op().type_convert(lval, if_ret_type, opinfo);
    } else {
      throw new CompileException("mh return type(" + r_mh_ret_type
          + ") is not compatible for functinoal interface return type(" + if_ret_type + ")");
    }

    if_ret_type.op().return_variable(null, opinfo);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // write class file
    try {
      Debug.println_dbg("Writing Class File(" + class_name + ")");
      cpLoader.writeClassFile(cw, class_name);
    } catch (Exception e) {
      // e.printStackTrace();
      CompileException excp = new CompileException("Exception occurred in writing class file");
      excp.setTargetException(e);
      throw excp;
    }

  }

}
