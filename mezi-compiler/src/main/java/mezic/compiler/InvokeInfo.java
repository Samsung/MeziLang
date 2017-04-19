package mezic.compiler;

import mezic.compiler.type.AbsType;
import mezic.parser.LangUnitNode;

public class InvokeInfo extends Reduction {

  private static final long serialVersionUID = -4478881201370549233L;

  public final static int FORM_LOCAL = 0;
  public final static int FORM_MEMBER = 1;

  private int form = 0;

  private AbsType owner_type = null;

  private LangUnitNode symbol_node;

  public InvokeInfo(LangUnitNode symbol_node, int form) throws CompileException {
    if (symbol_node == null) {
      throw new CompileException("Invalid symbol_node");
    }
    this.symbol_node = symbol_node;
    this.form = form;
  }

  public LangUnitNode getSymbolNode() {
    return this.symbol_node;
  }

  public boolean isForm(int form) {
    return this.form == form;
  }

  public String getFormString() {
    switch (form) {
    case FORM_LOCAL:
      return "Local Invoke";
    case FORM_MEMBER:
      return "Member Invoke";
    default:
      return "Not Defined Invoke Type";
    }
  }

  public void setOwnerType(AbsType owner_type) {
    this.owner_type = owner_type;
  }

  public AbsType getOwnerType() {
    return this.owner_type;
  }

  @Override
  public int reduceType() {
    return Reduction.INVOKE_INFO;
  }

  public String toString() {
    String msg;

    msg = "InvokeInfo (" + getFormString() + ":" + symbol_node;
    if (owner_type != null) {
      msg += (":owner=" + owner_type);
    }
    msg += ")";
    return msg;
  }

}
