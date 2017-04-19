package mezic.compiler;

import mezic.compiler.type.AbsType;
import mezic.compiler.type.OpLvalue;
import mezic.compiler.type.TContext;
import mezic.parser.Token;

public class Container extends Reduction {

  private static final long serialVersionUID = -5701252282403450023L;

  public final static int FORM_FUNSTACK_VAR = 1;
  public final static int FORM_OBJMEMBER_VAR = 2;
  public final static int FORM_MAPELEMENT_VAR = 3;
  public final static int FORM_OPSTACK_VAR = 4;
  public final static int FORM_CONSTANT_VAR = 5;
  public final static int FORM_TYPE = 6;
  public final static int FORM_SPECIALTOKEN = 7;

  protected String name = null;

  private int form = -1;

  protected boolean is_constant = false;

  protected boolean is_singleton = false;

  private TContext context = null; // owner Context of this container

  protected Object object; // actual memory reference

  private AbsType type = null;

  private boolean is_assigned = false;

  private Container owner_container = null;

  private Container closure_orgfuncvar_container = null;

  private Token specialtoken = null;


  public Container(String name, int containerform, boolean is_constant, boolean is_singleton) throws CompileException {
    if (!isValidName(name)) {
      throw new CompileException("Container name(" + name + ") is invalid");
    }
    this.name = name;
    this.form = containerform;
    this.is_constant = is_constant;
    this.object = null;
    this.is_singleton = is_singleton;

    if (containerform == FORM_TYPE) {
      if (is_singleton) {
        throw new CompileException("is_singleton cannot be configured for Type Container");
      }
    }

  }

  public void setContext(TContext context) {
    this.context = context;
  }

  public TContext getContext() {
    return this.context;
  }

  private int context_var_idx = -1;

  public void setContextVarIdx(int index) {
    this.context_var_idx = index;
  }

  public int getContextVarIdx() {
    return this.context_var_idx;
  }

  public boolean isContextVarInitialized() {
    return (context_var_idx != -1);
    /*
    if (context_var_idx == -1)
      return false;
    else
      return true;
    */
  }

  public String toString() {
    String msg;

    msg = "Container(" + name + ":" + (is_constant ? "const" : "variable") + ":" + getFormString();
    msg += (":ctxidx=" + getContextVarIdx());
    msg += (":type=" + getType());
    if (is_singleton) {
      msg += (":singleton");
    }
    msg += ")";
    if (object != null && getForm() != Container.FORM_TYPE) {
      msg += object.toString();
    }
    if (owner_container != null) {
      msg += ("Owner:" + owner_container.toString());
    }

    return msg;
  }

  public String getName() {
    return this.name;
  }

  public boolean isName(String name) {
    return (getName().equals(name));
    /*
    if (getName().equals(name))
      return true;
    else
      return false;
    */
  }

  public boolean isValidName(String name) {
    return !(name == null || name.equals(""));
    /*
    if (name == null || name.equals(""))
      return false;
    else
      return true;
    */
  }

  public int getForm() {
    return this.form;
  }

  public boolean isForm(int containertype) {
    return (this.form == containertype);
  }

  public String getFormString() {
    switch (this.form) {
    case FORM_FUNSTACK_VAR:
      return "FUNCSTACK_VAR";
    case FORM_OBJMEMBER_VAR:
      return "OBJMEMBER_VAR";
    case FORM_MAPELEMENT_VAR:
      return "MAPELEMENT_VAR";
    case FORM_OPSTACK_VAR:
      return "OPSTACK_VAR";
    case FORM_CONSTANT_VAR:
      return "CONSTANT_VAR";
    case FORM_TYPE:
      return "TYPE";
    case FORM_SPECIALTOKEN:
      return "SPECIALTOKEN";
    default:
      return "Not Defined";
    }
  }

  public boolean isConstant() {
    return is_constant;
  }

  public boolean isSingleton() {
    return is_singleton;
  }

  public void initializeType(AbsType type) throws CompileException {
    if (type == null) {
      throw new CompileException("Initializing Type is Invalid");
    }
    this.type = type;
  }

  public void uninitializeType() throws CompileException {
    this.type = null;
  }

  public boolean isTypeInitialized() {
    return (type != null);
    /*
    if (type != null)
      return true;
    else
      return false;
    */
  }

  public AbsType getType() {
    return type;
  }

  public boolean isEqualType(Container cont) throws CompileException {
    return (getType().equals(cont.getType()));
    /*
    if (getType().equals(cont.getType()))
      return true;
    else
      return false;
    */
  }

  public OpLvalue op() throws CompileException {
    if (type == null) {
      throw new CompileException(toString() + "Type is not initialized");
    }

    return type.op();
  }

  public void setAssigned(boolean assigned) {
    this.is_assigned = assigned;
  }

  public boolean isAssigned() {
    return this.is_assigned;
  }

  public void setContainerObject(Object src_obj) throws CompileException {
    if (is_constant && this.object != null) {
      throw new CompileException(toString() + " is constnat");
    }

    if (!isTypeInitialized()) {
      throw new CompileException(toString() + " is not initialized");
    }

    this.object = src_obj;
  }

  public Object getContainerObject() throws CompileException {
    if (!isTypeInitialized()) {
      throw new CompileException(toString() + " is not initialized");
    }
    return object;
  }

  public Object getContainerObjectNoCheck() throws CompileException {
    return object;
  }

  public Container getOpStackClone(String name) throws CompileException {
    Container clone = new Container(name, Container.FORM_OPSTACK_VAR, this.is_constant, this.is_singleton);
    // Container clone = new Container(name, this.form, this.is_constant,
    // this.is_singleton);

    // is this correct ?
    clone.initializeType(this.getType());
    clone.setContext(this.getContext());
    clone.setContextVarIdx(this.getContextVarIdx());
    clone.setAssigned(this.isAssigned());
    clone.setContainerObject(this.getContainerObjectNoCheck());
    // clone.initOwnerContainer(this.getOwnerContainer());

    clone.initOwnerContainer(this); // original container

    return clone;
  }

  public void initOwnerContainer(Container owner_container) throws CompileException {
    if (this.owner_container != null) {
      throw new CompileException("owner container(" + this + " is already initialized");
    }

    this.owner_container = owner_container;
  }

  public Container getOwnerContainer() {
    return this.owner_container;
  }

  public void initClosureOrgFuncvarContainer(Container org_container) throws CompileException {
    this.closure_orgfuncvar_container = org_container;
  }

  public Container getClosureOrgFuncvarContainer() throws CompileException {
    return closure_orgfuncvar_container;
  }

  public void bindVarIdx() throws CompileException {
    Debug.assertion(getContext() != null, "Container Context should be valid");
    Debug.assertion(getContextVarIdx() == -1,
        "Container(" + this + ") Var Idx(" + getContextVarIdx() + ") is already binded");
    Debug.assertion(isTypeInitialized(), "Container(" + this + ") Type should be initialized");

    int var_idx = getContext().allocChildVarIdx(getType().op().getCategory());

    Debug.println_dbg("bindVarIdx: var_idx=" + var_idx + ", cont: " + this);

    setContextVarIdx(var_idx);

  }

  public void setSpecialToken(Token sptoken) {
    this.specialtoken = sptoken;
  }

  public Token getSpecialToken() {
    return this.specialtoken;
  }

  public boolean isMapElementOpStackVar() throws CompileException {

    Debug.assertion(isForm(FORM_OPSTACK_VAR), "this container should be opstack var, but " + this);

    Container org_cont = getOwnerContainer();
    Debug.assertion(org_cont != null, "opstack var should have original var");

    return org_cont.isForm(FORM_MAPELEMENT_VAR);
  }

  public int reduceType() {
    return Reduction.CONTAINER;
  }

}
