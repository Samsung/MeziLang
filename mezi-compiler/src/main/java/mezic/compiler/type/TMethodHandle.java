package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import org.objectweb.asm.tree.ClassNode;

public class TMethodHandle extends TResolvedClass implements AbsClassType {

  private static final long serialVersionUID = -589726488749989707L;

  public final static String METHODHANDLE_CLASS = "java/lang/invoke/MethodHandle";
  public final static String METHODHANDLE_CLASS_DESC = "L" + METHODHANDLE_CLASS + ";";

  private FuncSignatureDesc funcsigdesc = null;

  public TMethodHandle(ClassNode class_node, OpLvalue type_op, CompilerLoader cpLoader) throws CompileException {
    super(class_node, type_op, cpLoader);
  }

  public static TMethodHandle getInstance(CompilerLoader cpLoader) throws CompileException {
    ClassNode class_node = cpLoader.findClassNode(METHODHANDLE_CLASS);

    return new TMethodHandle(class_node, new OpFunction(cpLoader), cpLoader);
  }

  @Override // override Type.getMthdDscStr
  public String getMthdDscStr() throws CompileException {
    return METHODHANDLE_CLASS_DESC;
  }

  public String getSigStr() throws CompileException {
    return funcsigdesc.getSigStr();
  }

  public String getClassName() {
    return class_node.name;
  }

  public String toString() {
    String info = "MethodHandle:";
    if (funcsigdesc != null) {
      info += funcsigdesc;
    } else {
      info += "sinature is empty";
    }

    return info;
  }

  public FuncSignatureDesc getFuncSignatureDesc() throws CompileException {

    if (this.funcsigdesc == null) {
      throw new CompileException("This Class does not have func signature desc");
    }
    return funcsigdesc;
  }

  public void setFuncSignatureDesc(FuncSignatureDesc funcsig) throws CompileException {
    this.funcsigdesc = funcsig;
  }

  @Override
  public boolean equals(Object type) {
    try {
      String srcsig = getFuncSignatureDesc().getSigStr();

      if (!(type instanceof TMethodHandle)) {
        return false;
      }

      String tgtsig = ((TMethodHandle) type).getFuncSignatureDesc().getSigStr();

      return srcsig.equals(tgtsig);
    } catch (CompileException e) {
      e.printStackTrace();
      return false;
    }
  }


  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
