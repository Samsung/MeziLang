package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;

public class FuncSignatureDesc implements java.io.Serializable {

  private static final long serialVersionUID = -6628934326296942937L;

  private AbsTypeList param_type_list = null;
  private AbsType return_type = null;
  //transient private CompilerLoader cpLoader = null;

  public FuncSignatureDesc(CompilerLoader cpLoader) throws CompileException {
    param_type_list = new AbsTypeList();
    // return_type =
    // (AbsType)cpLoader.findPrimitiveType(TPrimitiveClass.NAME_VOID);
    return_type = (AbsType) cpLoader.findClassFull(CompilerLoader.UNRESOLVED_TYPE_CLASS_NAME);
  }

  public void appendTailPrameterType(AbsType para_type) {
    param_type_list.add(para_type);
  }

  public void pushHeadPrameterType(AbsType para_type) {
    param_type_list.addFirst(para_type);
  }

  public void updateParameterTypeList(AbsTypeList list) {
    this.param_type_list = list;
  }

  public void initReturnType(AbsType return_Type) {
    this.return_type = return_Type;
  }

  public AbsTypeList getParameterTypeList() {
    return param_type_list;
  }

  public String getParamTypeMthdDsc() throws CompileException {
    return param_type_list.getMthdDsc();
  }

  public AbsType getReturnType() {
    return return_type;
  }

  public String getReturnTypeMthdDsc() throws CompileException {

    return return_type.getMthdDscStr();
  }

  // gives standard JVM Method Descriptor
  public String getMthdDscStr() throws CompileException {
    return "(" + getParamTypeMthdDsc() + ")" + getReturnTypeMthdDsc();
  }

  public String getSigStr() throws CompileException {

    String sigStr = "f[";

    sigStr += "(" + param_type_list.getSigStr() + ")";

    try {
      if (return_type instanceof TMethodHandle) {
        sigStr += ((TMethodHandle) return_type).getSigStr();
      } else {
        sigStr += return_type.getMthdDscStr();
      }
    } catch (CompileException e) {
      e.printStackTrace();
    }
    sigStr += "]";

    return sigStr;
  }

  public boolean equalDesc(FuncSignatureDesc desc) throws CompileException {
    // return this.getMthdDscStr().equals(desc.getMthdDscStr());
    return this.getSigStr().equals(desc.getSigStr());
  }

  @Override
  public String toString() {
    try {
      return getSigStr();
    } catch (CompileException e) {
      e.printStackTrace();
    }
    return null;
  }

}
