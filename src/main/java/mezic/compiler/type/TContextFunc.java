package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TContextFunc extends TContext implements AbsFuncType {

  private static final long serialVersionUID = -8106771130610455196L;

  private boolean is_constructor = false;
  private boolean is_static = false;
  private boolean is_closure = false;
  private boolean is_apply = false;
  private boolean has_constructor_call = false;

  private MethodVisitor methodVisitor;

  private FuncSignatureDesc sigdesc = null;

  private AbsTypeList throws_list = null;

  private int child_closure_ctx_idx = 0;

  private int child_func_if_impl_idx = 0;

  public TContextFunc(TContext parent, String func_name, OpLvalue type_op, boolean is_closure, CompilerLoader cpLoader)
      throws CompileException {
    super(parent, AbsType.FORM_FUNC, func_name, type_op, cpLoader);

    this.sigdesc = new FuncSignatureDesc(cpLoader); // this function signature
                                                    // descriptor will be
                                                    // updated later

    init_dflt_return_type(func_name);

    this.is_closure = is_closure;
  }

  private void init_dflt_return_type(String func_name) throws CompileException {
    AbsType dflttype = null;

    if (func_name.equals(AbsClassType.CONSTRUCTOR_NAME)) {
      dflttype = (AbsType) cpLoader.findPrimitiveType(TPrimitiveClass.NAME_VOID);
    } else {
      dflttype = (AbsType) cpLoader.findClassFull(CompilerLoader.UNRESOLVED_TYPE_CLASS_NAME);
    }

    if (dflttype == null) {
      throw new CompileException("It cannot find void type");
    }

    this.sigdesc.initReturnType(dflttype);
  }

  public MethodVisitor getMethodVisitor() throws CompileException {
    return methodVisitor;
  }

  public void setMethodVisitor(MethodVisitor methodVisitor) throws CompileException {
    this.methodVisitor = methodVisitor;
  }

  @Override
  public AbsType getReturnType(CompilerLoader cpLoader) throws CompileException {
    return this.sigdesc.getReturnType();
  }

  @Override
  public FuncSignatureDesc getFuncSignatureDesc() throws CompileException {
    return this.sigdesc;
  }

  public void setIsConstuctor(boolean is_constructor) {
    this.is_constructor = is_constructor;
  }

  @Override
  public boolean is_constructor() {
    return is_constructor;
  }

  public boolean has_constructor_call() {
    return has_constructor_call;
  }

  public void set_has_constructor_call(boolean has) {
    has_constructor_call = has;
  }

  @Override
  public boolean is_static() {
    return is_static;
  }

  public void set_is_static(boolean is_static) {
    this.is_static = is_static;
  }

  @Override
  public boolean has_inst_body() {
    return true;
  }

  @Override
  public boolean is_abstract() {
    return false;
  }

  @Override
  public int get_access() throws CompileException {
    return Opcodes.ACC_PUBLIC;
  }

  public boolean is_closure() {
    return is_closure;
  }

  public void set_is_apply(boolean is_apply) {
    this.is_apply = is_apply;
  }

  @Override
  public boolean is_apply() {
    return this.is_apply;
  }

  @Override
  public String getMthdDscStr() throws CompileException {

    return sigdesc.getMthdDscStr();
  }

  public String getParamTypeDsc() throws CompileException {
    return sigdesc.getParamTypeMthdDsc();
  }

  public String allocAnonyFuncName() {
    return CLOSURE_PREFIX + (child_closure_ctx_idx++);
  }

  public String allocFunctionalIfImplName() {
    return FUNC_IF_IMPL_PREFIX + (child_func_if_impl_idx++);
  }

  @Override
  public AbsTypeList getThrowsList() throws CompileException {
    return throws_list;
  }

  public void setThrowsList(AbsTypeList throwslist) {
    this.throws_list = throwslist;
  }

  @Override
  public String toString() {
    String mthdDscStr = "";

    try {
      mthdDscStr = getMthdDscStr();
    } catch (CompileException e) {
    }

    String info = "Context ";
    info += (getFormString(getForm()));
    info += ("(" + getName() + ":" + mthdDscStr + ")");
    /*
     * Container container = null; int length = variable_list.size(); for( int i
     * = 0 ; i < length ; i++) { container = variable_list.get(i); info += (" ("
     * +container.getContextIndex()+")"+container+"\n"); }
     */
    return info;
  }

}
