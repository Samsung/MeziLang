package mezic.compiler.type;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.util.TypeUtil;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TResolvedClass extends AbsType implements AbsClassType {

  private static final long serialVersionUID = -8463717196949481880L;
  
  private static final Logger LOG = LoggerFactory.getLogger(TResolvedClass.class);


  transient private CompilerLoader cpLoader = null;

  transient protected ClassNode class_node = null;

  private AbsClassType super_class = null;

  private List<AbsClassType> interface_list = null;

  private boolean is_functional_interface = false;

  public TResolvedClass(ClassNode class_node, OpLvalue type_op, CompilerLoader cpLoader) throws CompileException {
    super(class_node.name);

    this.cpLoader = cpLoader;
    this.class_node = class_node;
    this.type_op = type_op;
    this.form = AbsType.FORM_CLASS;

    String superclass_str = class_node.superName;
    if (superclass_str != null) {
      super_class = cpLoader.findClassFull(superclass_str);
      if (super_class == null) {
        throw new CompileException("It can not find super class(" + superclass_str + ")");
      }
    }

    // construct interface list
    interface_list = new ArrayList<AbsClassType>();
    @SuppressWarnings("unchecked")
    List<String> ifstr_list = class_node.interfaces;

    if (ifstr_list != null) {

      for (String if_str : ifstr_list) {
        AbsClassType if_class = cpLoader.findClassFull(if_str);
        if (if_class == null) {
          throw new CompileException("It can not find interface(" + if_str + ")");
        }
        interface_list.add(if_class);
      }
    }

    // decides whether is functional interface
    @SuppressWarnings("unchecked")
    List<AnnotationNode> visibleAnnotations = class_node.visibleAnnotations;
    if (visibleAnnotations != null) {
      if (visibleAnnotations.stream().anyMatch((anno) -> anno.desc.equals(CompilerLoader.FUNCTIONAL_INTERFACE_DESC))) {
        is_functional_interface = true;
      }
    }

  }

  @Override // override from Context
  public String getMthdDscStr() throws CompileException {
    String dsc = "";
    dsc += "L" + getName().replace('.', '/') + ";";
    return dsc;
  }

  @Override // override Type.getLocalChildVariable
  public Container getLocalChildVariable(String field_name) throws CompileException {
    @SuppressWarnings("unchecked")
    List<FieldNode> fields = class_node.fields;
    FieldNode field = null;
    String type_name = null;
    AbsType child_type = null;
    Container container = null;
    boolean is_singleton = false;

    int len = fields.size();
    for (int i = 0; i < len; i++) {
      field = fields.get(i);

      if (field_name.equals(field.name)) {
        type_name = TypeUtil.dsc2name(field.desc);
        if (type_name == null) {
          throw new CompileException(
              "Invalid Field Description(" + getName() + "." + field.name + "[" + field.desc + "]");
        }

        // child_type = (AbsType)cpLoader.findClassFull(type_name);
        child_type = (AbsType) cpLoader.findNamedTypeFull(type_name);

        if (child_type == null) {
          throw new CompileException(
              "Failed to fined field class (" + getName() + "." + field.name + "[" + type_name + "])");
        }

        is_singleton = ((field.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC);
        container = new Container(field_name, Container.FORM_OBJMEMBER_VAR, false, is_singleton);
        container.initializeType(child_type);

        return container;
      }
    }
    return null;
  }

  public List<AbsFuncType> getLocalFunctions() throws CompileException {
    List<AbsFuncType> list = new ArrayList<>();
    getLocalFunctions(list);

    return list;
  }

  public void getLocalFunctions(List<AbsFuncType> list) throws CompileException {
    @SuppressWarnings("unchecked")
    List<MethodNode> methods = class_node.methods;
    MethodNode method = null;
    TResolvedFunc func_type = null;

    int length = methods.size();
    for (int i = 0; i < length; i++) {
      method = methods.get(i);
      func_type = new TResolvedFunc(this, method, cpLoader);
      list.add(func_type);
    }

    if (super_class != null) {
      super_class.getLocalFunctions(list);
    }

  }

  @Override // override TypeClass.getLocalFunction
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

    @SuppressWarnings("unchecked")
    List<MethodNode> methods = class_node.methods;
    MethodNode method = null;

    int length = methods.size();

    // find from local functions
    for (int i = 0; i < length; i++) {
      method = methods.get(i);

      if (name.equals(method.name)) {
        String tgt_paratype_dsc = TypeUtil.getArgTypeDsc(method.desc);

        LOG.debug("getLocalFunctionPoly: method.desc[" + method.desc + "] paratype_list[" + paratype_list + "]");

        if (tgt_paratype_dsc == null) {
          throw new CompileException(
              "Invalid Target Method(" + AbsClassType.CONSTRUCTOR_NAME + "." + method.name + "[" + method.desc + "]");
        }

        AbsTypeList tgt_paratype_list = AbsTypeList.construct(tgt_paratype_dsc, cpLoader);

        if (is_polymorphic) {

          if (cpLoader.isImpliedCastibleParameterList(paratype_list, tgt_paratype_list)) {
            return new TResolvedFunc(this, method, cpLoader);
          }
        } else {
          if (paratype_list.equalAbsTypeList(tgt_paratype_list)) {
            return new TResolvedFunc(this, method, cpLoader);
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
    AbsFuncType func = getLocalVarArgFunctionPoly(name, paratype_list, false);

    if (func != null) {
      return func;
    }

    return getLocalVarArgFunctionPoly(name, paratype_list, true);
  }

  @Override
  public AbsFuncType getLocalVarArgFunctionPoly(String name, AbsTypeList invoke_paratype_list, boolean is_polymorphic)
      throws CompileException {
    @SuppressWarnings("unchecked")
    List<MethodNode> methods = class_node.methods;
    MethodNode method = null;

    int len = methods.size();

    // find from local functions
    for (int i = 0; i < len; i++) {
      method = methods.get(i);
      if (name.equals(method.name)) {
        String tgt_paratype_dsc = TypeUtil.getArgTypeDsc(method.desc);
        LOG.debug(
            "getLocalVarArgFunctionPoly: method.desc[" + method.desc + "] paratype_list[" + invoke_paratype_list + "]");

        if (tgt_paratype_dsc == null) {
          throw new CompileException(
              "Invalid Target Method(" + AbsClassType.CONSTRUCTOR_NAME + "." + method.name + "[" + method.desc + "]");
        }

        AbsTypeList tgt_paratype_list = AbsTypeList.construct(tgt_paratype_dsc, cpLoader);

        // if( name.equals("printf") && tgt_paratype_list.size() > 0 )
        if (tgt_paratype_list.size() > 0) {
          if (cpLoader.isVarArgAbsTypeList(invoke_paratype_list, tgt_paratype_list, is_polymorphic)) {
            // Debug.stop();
            return new TResolvedFunc(this, method, cpLoader);
          }
        }
      }
    }

    // find from super class
    if (super_class != null) {
      AbsFuncType super_func = super_class.getLocalVarArgFunctionPoly(name, invoke_paratype_list, is_polymorphic);
      if (super_func != null) {
        return super_func;
      }
    }

    // find from interfaces
    for (AbsClassType if_type : interface_list) {
      AbsFuncType if_func = if_type.getLocalVarArgFunctionPoly(name, invoke_paratype_list, is_polymorphic);
      if (if_func != null) {
        return if_func;
      }
    }

    return null;
  }

  @Override
  public AbsFuncType getLocalApplyFunction(String name, AbsTypeList paratype_list) throws CompileException {
    @SuppressWarnings("unchecked")
    List<MethodNode> methods = class_node.methods;
    MethodNode method = null;

    AbsType apply_type = (AbsType) cpLoader.findClassFull(CompilerLoader.APPLY_CLASS_NAME);

    int length = methods.size();
    // find from local functions
    for (int i = 0; i < length; i++) {
      method = methods.get(i);

      if (name.equals(method.name)) {
        String tgt_paratype_dsc = TypeUtil.getArgTypeDsc(method.desc);

        LOG.debug(
            "getLocalApplyFunction: method.desc[" + method.desc + "] paratype_list[" + paratype_list + "]");

        if (tgt_paratype_dsc == null) {
          throw new CompileException(
              "Invalid Target Method(" + AbsClassType.CONSTRUCTOR_NAME + "." + method.name + "[" + method.desc + "]");
        }

        AbsTypeList tgt_paratype_list = AbsTypeList.construct(tgt_paratype_dsc, cpLoader);

        if (tgt_paratype_list.has(apply_type)) {
          int tgt_param_size = tgt_paratype_list.size();
          int param_size = paratype_list.size();
          if (tgt_param_size == param_size) {
            return new TResolvedFunc(this, method, cpLoader);
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
    @SuppressWarnings("unchecked")
    List<MethodNode> methods = class_node.methods;
    MethodNode method = null;
    String tgt_arg_type_dsc = "";
    TResolvedFunc rsol_func = null;

    int len = methods.size();
    for (int i = 0; i < len; i++) {
      method = methods.get(i);

      if (AbsClassType.CONSTRUCTOR_NAME.equals(method.name)) {
        tgt_arg_type_dsc = TypeUtil.getArgTypeDsc(method.desc);
        if (tgt_arg_type_dsc == null) {
          throw new CompileException(
              "Invalid Target Method(" + AbsClassType.CONSTRUCTOR_NAME + "." + method.name + "[" + method.desc + "]");
        }

        AbsTypeList tgt_paratype_list = AbsTypeList.construct(tgt_arg_type_dsc, cpLoader);

        // LOG.debug("("+i+")
        // ["+method.name+"/"+method.desc+"]["+tgt_arg_type_dsc+"/"+tgt_paratype_list+"]
        // for ["+paratype_list+"]");

        if (is_polymorphic) {
          if (cpLoader.isImpliedCastibleParameterList(paratype_list, tgt_paratype_list)) {
            rsol_func = new TResolvedFunc(this, method, cpLoader);
            rsol_func.setIsConstuctor(true);
            return rsol_func;
          }
        } else {
          if (paratype_list.getMthdDsc().equals(tgt_arg_type_dsc)) {
            rsol_func = new TResolvedFunc(this, method, cpLoader);
            rsol_func.setIsConstuctor(true);
            return rsol_func;
          }
        }
      }
    }

    /*
     * // CAUTION !! it is not permitted to use parents class constructor if(
     * super_class != null) { TypeFunction super_func =
     * super_class.getLocalConstructorPoly(arg_type_dsc, is_polymorphic); return
     * super_func; }
     */

    return null;
  }

  public AbsFuncType[] getLocalConstructors() throws CompileException {
    @SuppressWarnings("unchecked")
    List<MethodNode> methods = class_node.methods;
    MethodNode method = null;
    TResolvedFunc rsol_func = null;
    LinkedList<TResolvedFunc> const_list = new LinkedList<TResolvedFunc>();

    int len = methods.size();
    for (int i = 0; i < len; i++) {
      method = methods.get(i);

      if (AbsClassType.CONSTRUCTOR_NAME.equals(method.name)) {
        rsol_func = new TResolvedFunc(this, method, cpLoader);
        rsol_func.setIsConstuctor(true);
        const_list.add(rsol_func);
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

  public AbsClassType getSuperClass() throws CompileException {
    return super_class;
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
    return ((this.class_node.access & Opcodes.ACC_INTERFACE) != 0);
  }

  @Override
  public boolean isFunctionalInterface() throws CompileException {
    return is_functional_interface;
  }

  @Override
  public String toString() {
    String info = "Resolved ";
    info += (getFormString(getForm()));
    info += ("(" + getName() + ")");

    /*
     * if( super_class!=null) info +=
     * ("(super:"+((AbsType)super_class).getName()+")");
     *
     * for( AbsClassType if_type: interface_list) { info +=
     * ("(impl:"+((AbsType)if_type).getName()+")"); }
     */

    return info;
  }

}
