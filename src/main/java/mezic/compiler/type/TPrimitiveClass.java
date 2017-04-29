package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;

import org.objectweb.asm.tree.ClassNode;

public class TPrimitiveClass extends TResolvedClass implements AbsClassType {

  private static final long serialVersionUID = 3161051081911754352L;

  public final static String NAME_INT = "int";
  public final static String NAME_CHAR = "char";
  public final static String NAME_SHORT = "short";
  public final static String NAME_BYTE = "byte";
  public final static String NAME_LONG = "long";
  public final static String NAME_BOOL = "boolean";
  public final static String NAME_VOID = "void";
  public final static String NAME_FLOAT = "float";
  public final static String NAME_DOUBLE = "double";

  public TPrimitiveClass(String type_str, ClassNode class_node, OpLvalue type_op, CompilerLoader cpLoader)
      throws CompileException {
    super(class_node, type_op, cpLoader);

    // overwrite type name
    name = type_str;
  }

  @Override // override Type.getMthdDscStr
  public String getMthdDscStr() throws CompileException {
    if (class_node == null) {
      throw new CompileException("type_class is invalid ");
    }
    if (this.isName(NAME_INT)) {
      return "I";
    } else if (this.isName(NAME_CHAR)) {
      return "C";
    } else if (this.isName(NAME_SHORT)) {
      return "S";
    } else if (this.isName(NAME_BYTE)) {
      return "B";
    } else if (this.isName(NAME_BOOL)) {
      return "Z";
    } else if (this.isName(NAME_VOID)) {
      return "V";
    } else if (this.isName(NAME_LONG)) {
      return "J";
    } else if (this.isName(NAME_FLOAT)) {
      return "F";
    } else if (this.isName(NAME_DOUBLE)) {
      return "D";
    } else {
      throw new CompileException("Method Dscription is not defined in Type (" + getName() + ")");
    }
  }

  public String getClassName() {
    return class_node.name;
  }

  @Override // from AbsClassType
  public boolean isDescendentOf(AbsClassType ancestor) throws CompileException {
    return false;
  }

  public String toString() {
    String info = "Primitive ";
    info += (getFormString(getForm()));
    info += ("(" + getName() + ")");
    return info;
  }

}
