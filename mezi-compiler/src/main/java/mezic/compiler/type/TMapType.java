package mezic.compiler.type;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;

public class TMapType extends AbsType {

  private static final long serialVersionUID = -8307349131234714159L;

  @SuppressWarnings("unused")
  transient private CompilerLoader cpLoader = null;
  transient private AbsType element_type = null;
  private int dimension = 0;

  public TMapType(AbsType element_type, int dimension, OpLvalue type_op, CompilerLoader cpLoader)
      throws CompileException {
    super(element_type.getName() + "[]");

    this.element_type = element_type;
    this.cpLoader = cpLoader;
    this.type_op = type_op;
    this.form = AbsType.FORM_MAP;
    this.dimension = dimension;
  }

  @Override
  public String getMthdDscStr() throws CompileException {
    return "[" + element_type.getMthdDscStr();
  }

  @Override
  public Container getLocalChildVariable(String name) throws CompileException {
    return null;
  }

  public AbsType getElementType() {
    return element_type;
  }

  public int getDimension() {
    return dimension;
  }

}
