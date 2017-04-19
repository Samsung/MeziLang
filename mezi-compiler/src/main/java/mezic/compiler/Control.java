package mezic.compiler;

import java.util.Objects;

public class Control extends Reduction {

  private static final long serialVersionUID = -4779201255285406470L;

  @Override
  public int reduceType() {
    return Reduction.CONTROL;
  }

  private int form = 0;

  public static final int FORM_RETURN = 1;
  public static final int FORM_BRANCH = 2;
  public static final int FORM_CONTINUE = 3;
  public static final int FORM_BREAK = 4;
  public static final int FORM_LOOP = 5;
  public static final int FORM_THROW = 6;

  public Control(int form) {
    this.form = form;
  }

  public int getForm() {
    return this.form;
  }

  public boolean isForm(int form) {
    return (this.form == form);
  }

  public String getFormString(int form) {
    switch (form) {
    case FORM_RETURN:
      return "return";
    case FORM_BRANCH:
      return "branch";
    case FORM_CONTINUE:
      return "continue";
    case FORM_BREAK:
      return "break";
    case FORM_LOOP:
      return "loop";
    case FORM_THROW:
      return "throw";
    default:
      return "Not Defined";
    }
  }

  @Override
  public String toString() {
    return "Control(" + getFormString(form) + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Control)) {
      return false;
    }
    Control ctrl = (Control) obj;
    return (this.form == ctrl.getForm());
    /*
    if (this.form == ctrl.getForm())
      return true;
    else
      return false;
    */
  }

  @Override
  public int hashCode() {
    // TODO : Is it correct ?
    return Objects.hash(this.form);
  }


}
