package mezic.compiler.type;

import java.util.Objects;

import mezic.compiler.CompileException;
import mezic.compiler.Container;
import mezic.compiler.Reduction;
import mezic.util.PathUtil;

abstract public class AbsType extends Reduction {

  private static final long serialVersionUID = -7265455918142025997L;

  protected String name = null;
  private Container type_container = null;
  protected int form = 0; // default form is list
  protected OpLvalue type_op = null;

  public AbsType(String type_name) throws CompileException {
    name = type_name;
    type_container = new Container(type_name, Container.FORM_TYPE, true, false);
    type_container.initializeType(this);
  }

  public Container getTypeContainer() {
    return type_container;
  }

  public final static int FORM_STREAM = 0;
  public final static int FORM_TU = 1;
  public final static int FORM_CLASS = 2;
  public final static int FORM_FUNC = 3;
  public final static int FORM_PKG = 4;
  public final static int FORM_MAP = 5;

  public int getForm() {
    return this.form;
  }

  public boolean isForm(int form) {
    return (this.form == form);
  }

  public String getFormString(int form) {
    switch (form) {
    case FORM_STREAM:
      return "stream";
    case FORM_TU:
      return "translation unit";
    case FORM_CLASS:
      return "class";
    case FORM_FUNC:
      return "function";
    case FORM_PKG:
      return "package";
    case FORM_MAP:
      return "map";
    default:
      return "Not Defined";
    }
  }

  public OpLvalue op() throws CompileException {
    if (type_op == null) {
      throw new CompileException("LvalueOp is not configured for this type(" + getName() + ")");
    }

    return type_op;
  }

  public String getName() {
    return name;
  }

  public boolean isName(String name) {
    return this.name.equals(name);
  }

  // If name is 'java/lang/Integer', simple name is 'Integer'.
  public String getSimpleName() {
    return PathUtil.getSimpleName(getName());
  }

  abstract public String getMthdDscStr() throws CompileException;

  abstract public Container getLocalChildVariable(String name) throws CompileException;

  public AbsType getOwnerType() throws CompileException { // this method should be
                                                        // overrided in child
                                                        // classes
    throw new CompileException("Owner Type is not configured for this type(" + getName() + ")");
  }

  @Override
  public int reduceType() {
    return Reduction.TYPE;
  }

  @Override
  public boolean equals(Object type) {
    if (!(type instanceof AbsType)) {
      return false;
    }

    AbsType tgt_type = (AbsType) type;

    if (getForm() != tgt_type.getForm()) { // should have same type form
      return false;
    }

    if (!getName().equals(tgt_type.getName())) { // should have same type name
      return false;
    }

    try {
      if (!type_op.getOpString().equals(tgt_type.op().getOpString())) {
        // should have same type op
        return false;
      }
    } catch (CompileException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }


  @Override
  public int hashCode() {
    // TODO : Is it correct ?
    return Objects.hash(getForm(), getName(), type_op.getOpString());
  }


  @Override
  public String toString() {
    return "AbsType(" + getFormString(this.form) + ":" + getName() + ")";
  }

}
