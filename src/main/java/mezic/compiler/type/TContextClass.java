package mezic.compiler.type;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.util.TypeUtil;

import org.objectweb.asm.ClassWriter;

public class TContextClass extends TContext implements AbsClassType {

  private static final long serialVersionUID = 6509028382198522278L;

  private boolean is_gen_dflt_constructor = false;

  private AbsClassType super_class = null;

  private List<AbsClassType> interface_list = null;

  private int anonyfun_idx = 0;

  public TContextClass(TContext parent, String class_name, OpLvalue type_op, CompilerLoader cpLoader)
      throws CompileException {
    super(parent, AbsType.FORM_CLASS, class_name, type_op, cpLoader);

    super_class = cpLoader.findClassFull("java/lang/Object");
    if (super_class == null) {
      throw new CompileException("it cannot find java/lang/Object class");
    }

    interface_list = new ArrayList<AbsClassType>();

  }

  @Override
  public String getMthdDscStr() throws CompileException {

    String name = getName();

    if (name == null) {
      throw new CompileException("name is invalid ");
    }

    return "L" + name.replace('.', '/') + ";";
  }

  public List<AbsFuncType> getLocalFunctions() throws CompileException {
    List<AbsFuncType> list = new ArrayList<>();
    getLocalFunctions(list);

    return list;
  }

  public void getLocalFunctions(List<AbsFuncType> list) throws CompileException {
    TContext context = null;
    TContextFunc func_context = null;

    int length = childcontext_list.size();
    for (int i = 0; i < length; i++) {
      context = childcontext_list.get(i);
      if (context.isForm(AbsType.FORM_FUNC)) {
        func_context = (TContextFunc) context;
        list.add(func_context);
      }
    }

    if (super_class != null) {
      super_class.getLocalFunctions(list);
    }

  }

  @Override
  public AbsFuncType getLocalFunction(String name, AbsTypeList paratype_list) throws CompileException {
    AbsFuncType func = getLocalFunctionPoly(name, paratype_list, false);

    if (func != null) {
      return func;
    }

    return getLocalFunctionPoly(name, paratype_list, true);
  }

  @Override
  public AbsFuncType getLocalFunctionPoly(String name, AbsTypeList paratype_list, boolean is_polymorphic)
      throws CompileException {
    TContext context = null;
    TContextFunc func_context = null;

    int length = childcontext_list.size();

    // find from local functions
    for (int i = 0; i < length; i++) {
      context = childcontext_list.get(i);
      if (context.isForm(AbsType.FORM_FUNC) && context.isName(name)) {
        func_context = (TContextFunc) context;

        if (is_polymorphic) {
          if (cpLoader.isImpliedCastibleParameterList(paratype_list,
              func_context.getFuncSignatureDesc().getParameterTypeList())) {
            return func_context;
          }
        } else {
          if (paratype_list.getMthdDsc().equals(func_context.getParamTypeDsc())) {
            return func_context;
          }
        }
      }
    }

    // find from super class
    if (super_class != null) {
      AbsFuncType super_func = super_class.getLocalFunctionPoly(name, paratype_list, is_polymorphic);
      if (super_func != null) {
        return super_func;
      }
    }

    // find from interfaces
    for (AbsClassType if_type : interface_list) {
      AbsFuncType if_func = if_type.getLocalFunctionPoly(name, paratype_list, is_polymorphic);
      if (if_func != null) {
        return if_func;
      }
    }

    return null;
  }

  @Override
  public AbsFuncType getLocalVarArgFunction(String name, AbsTypeList paratype_list) throws CompileException {
    return null;
  }

  @Override
  public AbsFuncType getLocalVarArgFunctionPoly(String name, AbsTypeList paratype_list, boolean is_polymorphic)
      throws CompileException {
    return null;
  }

  @Override
  public AbsFuncType getLocalApplyFunction(String name, AbsTypeList paratype_list) throws CompileException {
    TContext context = null;
    TContextFunc func_context = null;

    int length = childcontext_list.size();

    // find from local functions
    for (int i = 0; i < length; i++) {
      context = childcontext_list.get(i);
      if (context.isForm(AbsType.FORM_FUNC) && context.isName(name)) {
        func_context = (TContextFunc) context;

        if (func_context.is_apply()) {

          int func_context_param_size = func_context.getFuncSignatureDesc().getParameterTypeList().size();
          int param_size = paratype_list.size();

          if (func_context_param_size == param_size) {
            return func_context;
          }
        }
      }
    }

    // find from super class
    if (super_class != null) {
      AbsFuncType super_func = super_class.getLocalApplyFunction(name, paratype_list);
      if (super_func != null) {
        return super_func;
      }
    }

    // find from interfaces
    for (AbsClassType if_type : interface_list) {
      AbsFuncType if_func = if_type.getLocalApplyFunction(name, paratype_list);
      if (if_func != null) {
        return if_func;
      }
    }

    return null;
  }

  @Override // override TypeClass.getLocalConstructor
  public AbsFuncType getLocalConstructor(AbsTypeList paratype_list) throws CompileException {

    AbsFuncType func = getLocalConstructorPoly(paratype_list, false);

    if (func != null) {
      return func;
    }

    return getLocalConstructorPoly(paratype_list, true);
  }

  @Override
  public AbsFuncType getLocalConstructorPoly(AbsTypeList paratype_list, boolean is_polymorphic)
      throws CompileException {
    TContext context = null;
    TContextFunc func_context = null;

    // Debug.println_dbg("Class Child Context");
    // dump_child_context("-", 0);

    int length = childcontext_list.size();
    for (int i = 0; i < length; i++) {
      context = childcontext_list.get(i);

      // Debug.println_dbg("("+i+") "+context.getName());
      if (context.isForm(AbsType.FORM_FUNC) && context.isName(AbsClassType.CONSTRUCTOR_NAME)) {
        func_context = (TContextFunc) context;

        // Debug.println_dbg(func_context);
        if (is_polymorphic) {
          if (cpLoader.isImpliedCastibleParameterList(paratype_list,
              func_context.getFuncSignatureDesc().getParameterTypeList())) {
            return func_context;
          }
        } else {
          if (paratype_list.getMthdDsc().equals(func_context.getParamTypeDsc())) {
            return func_context;
          }
        }
      }
    }

    /*
     * // CAUTION !! it is not permitted to use parents class constructor if(
     * super_class != null) { TypeFunction super_func =
     * super_class.getLocalConstructor(arg_type_dsc); return super_func; }
     */
    return null;
  }

  public AbsFuncType[] getLocalConstructors() throws CompileException {
    TContext context = null;
    TContextFunc func_context = null;
    LinkedList<TContextFunc> const_list = new LinkedList<>();

    int length = childcontext_list.size();
    for (int i = 0; i < length; i++) {
      context = childcontext_list.get(i);

      if (context.isForm(AbsType.FORM_FUNC) && context.isName(AbsClassType.CONSTRUCTOR_NAME)) {
        func_context = (TContextFunc) context;

        const_list.add(func_context);
      }
    }

    if (const_list.size() == 0) {
      return null;
    }

    AbsFuncType[] arr = new AbsFuncType[const_list.size()];

    for (int i = 0; i < arr.length; i++) {
      arr[i] = (AbsFuncType) const_list.get(i);
    }

    return arr;
  }

  public void setGenDfltConstructor(boolean gen) throws CompileException {
    this.is_gen_dflt_constructor = gen;
  }

  public boolean isGenDfltConstructor() throws CompileException {
    return this.is_gen_dflt_constructor;
  }

  @Override
  public AbsClassType getSuperClass() throws CompileException {
    return super_class;
  }

  public void setSuperClass(AbsClassType super_class) throws CompileException {
    this.super_class = super_class;
  }

  public void addInterface(AbsClassType class_type) throws CompileException {
    interface_list.add(class_type);
  }

  public List<AbsClassType> getInterfaceList() throws CompileException {
    return interface_list;
  }

  @Override // from AbsClassType
  public boolean isDescendentOf(AbsClassType ancestor) throws CompileException {
    return TypeUtil.isDescendentOfClass(this, ancestor, 0);
  }

  @Override
  public boolean isInterface() throws CompileException {
    return false;
  }

  @Override
  public boolean isFunctionalInterface() throws CompileException {
    return false;
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
