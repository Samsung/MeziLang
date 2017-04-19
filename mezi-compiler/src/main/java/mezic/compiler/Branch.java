package mezic.compiler;

import java.util.LinkedList;

import mezic.parser.LangUnitNode;

public class Branch implements java.io.Serializable {

  private static final long serialVersionUID = -5078582884594820576L;

  private LangUnitNode node = null;
  private boolean is_valid = true;

  private Branch parent = null;

  private LinkedList<Branch> child_list = null;

  public Branch(LangUnitNode node) {
    this.node = node;
    this.is_valid = true;
    this.child_list = new LinkedList<Branch>();
  }

  public void setValid(boolean valid) {
    this.is_valid = valid;
  }

  public boolean isValid() {
    return is_valid;
  }

  public boolean isAllChildInvalid() {
    int size = child_list.size();

    for (int i = 0; i < size; i++) {
      if (child_list.get(i).isValid()) {
        return false;
      }
    }

    return true;
  }

  public LangUnitNode getNode() {
    return node;
  }

  public void initParent(Branch parent) throws CompileException {
    if (this.parent != null) {
      throw new CompileException("Branch parent is already initialized(" + this.parent + ")");
    }

    this.parent = parent;
  }

  public Branch getParent() {
    return parent;
  }

  public LinkedList<Branch> getChildList() {
    return child_list;
  }

  @Override
  public String toString() {
    return "Branch(" + node + ")";
  }

}
