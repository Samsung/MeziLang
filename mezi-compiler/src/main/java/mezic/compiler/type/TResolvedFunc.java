package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;
import mezic.util.TypeUtil;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public class TResolvedFunc extends AbsType implements AbsFuncType {

  private static final long serialVersionUID = 5135473235222491994L;

  private boolean is_constructor = false;

  private TResolvedClass owner_class;
  private MethodNode method_node;

  private FuncSignatureDesc sigdesc = null;

  private CompilerLoader cpLoader = null;

  public TResolvedFunc(TResolvedClass owner_class, MethodNode method_node, CompilerLoader cpLoader)
      throws CompileException {
    super(method_node.name);
    this.method_node = method_node;
    this.owner_class = owner_class;
    this.form = AbsType.FORM_FUNC;
    this.cpLoader = cpLoader;
    sigdesc = buildFuncSigDesc(cpLoader);
  }

  private FuncSignatureDesc buildFuncSigDesc(CompilerLoader cpLoader) throws CompileException {
    FuncSignatureDesc desc = new FuncSignatureDesc(cpLoader);
    String paratype_dsc = TypeUtil.getArgTypeDsc(method_node.desc);

    String[] paratype_split = TypeUtil.splitArgTypeDsc(paratype_dsc);
    String paratype_name;
    // AbsClassType paratype;
    AbsType paratype;

    for (int i = 0; i < paratype_split.length; i++) {
      paratype_name = TypeUtil.dsc2name(paratype_split[i]);
      paratype = cpLoader.findNamedTypeFull(paratype_name);

      if (paratype == null) {
        throw new CompileException("It cannot find class(" + paratype_name + ")");
      }

      desc.appendTailPrameterType(paratype);
    }

    String rettype_dsc = TypeUtil.getRetTypeDsc(method_node.desc);
    if (rettype_dsc == null) {
      throw new CompileException("Invalid Return Type in (" + method_node.desc + ")");
    }

    String rettype_name = TypeUtil.dsc2name(rettype_dsc);
    AbsType rettype = cpLoader.findNamedTypeFull(rettype_name);

    if (rettype == null) {
      throw new CompileException("It cannot find class(" + rettype_name + ")");
    }

    desc.initReturnType((AbsType) rettype);

    return desc;
  }

  @Override
  public String getMthdDscStr() throws CompileException {
    // return method_node.desc;
    return sigdesc.getMthdDscStr();
  }

  @Override // override Type.getLocalChildVariable
  public Container getLocalChildVariable(String name) throws CompileException {
    throw new CompileException("this method is not supported");
  }

  @Override
  public AbsType getOwnerType() throws CompileException { // this method should be
                                                        // overrided in child
                                                        // classes
    return owner_class;
  }

  @Override
  public AbsType getReturnType(CompilerLoader cpLoader) throws CompileException {
    return sigdesc.getReturnType();
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

  @Override
  public boolean is_static() {
    return ((method_node.access & Opcodes.ACC_STATIC) != 0);
  }

  @Override
  public boolean has_inst_body() {
    return method_node.instructions.size() > 0;
  }

  @Override
  public boolean is_abstract() {
    return ((method_node.access & Opcodes.ACC_ABSTRACT) != 0);
  }

  @Override
  public int get_access() throws CompileException {
    if ((method_node.access & Opcodes.ACC_PUBLIC) != 0) {
      return Opcodes.ACC_PUBLIC;
    } else if ((method_node.access & Opcodes.ACC_PRIVATE) != 0) {
      return Opcodes.ACC_PRIVATE;
    } else if ((method_node.access & Opcodes.ACC_PROTECTED) != 0) {
      return Opcodes.ACC_PROTECTED;
    }
    throw new CompileException("Invalid access priviledge");
  }

  @Override
  public boolean is_apply() throws CompileException {

    AbsType apply_type = (AbsType) cpLoader.findClassFull(CompilerLoader.APPLY_CLASS_NAME);
    Debug.assertion(apply_type != null, "apply_type should be valid");

    AbsTypeList paratype_list = sigdesc.getParameterTypeList();
    Debug.assertion(paratype_list != null, "paratype_list should be valid");

    return paratype_list.has(apply_type);
  }

  @Override
  public AbsTypeList getThrowsList() throws CompileException {
    AbsTypeList excp_type_list = new AbsTypeList();
    AbsClassType excp_class = null;

    int size = method_node.exceptions.size();

    for (int i = 0; i < size; i++) {
      excp_class = cpLoader.findClassFull((String) method_node.exceptions.get(i));
      Debug.assertion(excp_class != null, "excp_class should be valid");
      excp_type_list.add((AbsType) excp_class);
    }

    return excp_type_list;
  }

  public String toString() {
    String info = "Resolved ";
    info += (getFormString(getForm()));
    info += ("(" + getName() + ":" + sigdesc + ")");
    return info;
  }

}
