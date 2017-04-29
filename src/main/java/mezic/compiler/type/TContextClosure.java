package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Debug;
import mezic.util.TypeUtil;

import org.objectweb.asm.ClassWriter;

public class TContextClosure extends TContextClass {

  private static final long serialVersionUID = 9028672971020383295L;

  private int anonyfun_idx = 0;
  private int last_var_idx = -1;

  private TResolvedClass super_class = null;

  public TContextClosure(TContext parent, String closure_name, OpLvalue type_op, CompilerLoader cpLoader)
      throws CompileException {
    super(parent, closure_name, type_op, cpLoader);
    // last_var_idx = parent.getLastChildVariableIndex();

    super_class = (TResolvedClass) cpLoader.findClassFull("java/lang/Object");
    Debug.assertion(super_class != null, "super_class should be valid");
  }

  public void initLastClosureVarIndex(int last_idx) {
    last_var_idx = last_idx;
  }

  public int getLastClosureVarIndex() throws CompileException {
    // if( last_var_idx == -1) throw new InterpreterException("last_var_idx
    // should not be -1");
    // commented : applied function closure can have '-1' last_var_idx
    return last_var_idx;
  }

  @Override
  public AbsFuncType getLocalFunction(String name, AbsTypeList paratype_list) throws CompileException {
    // throw new InterpreterException("This Method is not implemented");

    // Closure class does not have its own local function.
    // If closure want to access location function of clousre owner function,
    // it needs to use 'this' or 'closure.this'
    // return null;
    return super_class.getLocalFunction(name, paratype_list);
  }

  @Override
  public AbsFuncType getLocalFunctionPoly(String name, AbsTypeList paratype_list, boolean is_polymorphic)
      throws CompileException {
    throw new CompileException("This Method is not implemented");
  }

  @Override // override TypeClass.getLocalConstructor
  public AbsFuncType getLocalConstructor(AbsTypeList paratype_list) throws CompileException {
    throw new CompileException("This Method is not implemented");
  }

  @Override
  public AbsFuncType getLocalConstructorPoly(AbsTypeList paratype_list, boolean is_polymorphic)
      throws CompileException {
    throw new CompileException("This Method is not implemented");
  }

  @Override
  public AbsFuncType[] getLocalConstructors() throws CompileException {
    throw new CompileException("This Method is not implemented");
  }

  @Override
  public void setGenDfltConstructor(boolean gen) throws CompileException {
    throw new CompileException("This Method is not implemented");
  }

  @Override
  public boolean isGenDfltConstructor() throws CompileException {
    throw new CompileException("This Method is not implemented");
  }

  /*
   * @Override public AbsClassType getSuperClass() throws InterpreterException {
   * throw new InterpreterException("This Method is not implemented"); }
   */

  @Override
  public void setSuperClass(AbsClassType super_class) throws CompileException {
    throw new CompileException("This Method is not implemented");
  }

  @Override // from AbsClassType
  public boolean isDescendentOf(AbsClassType ancestor) throws CompileException {
    return TypeUtil.isDescendentOfClass(this, ancestor, 0);
  }

  public static final String ANONYFUN_PREFIX = "__anonymous";

  public String allocAnonyFuncName() {
    return ANONYFUN_PREFIX + (anonyfun_idx++);
  }

  private ClassWriter classWriter;

  public ClassWriter getClassWriter() {
    return classWriter;
  }

  public void setClassWriter(ClassWriter classWriter) throws CompileException {

    if (!isForm(FORM_CLASS)) {
      throw new CompileException("ClassWriter should be set on ContextClass");
    }

    this.classWriter = classWriter;
  }

}
