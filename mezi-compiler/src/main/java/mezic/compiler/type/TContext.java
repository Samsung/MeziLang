package mezic.compiler.type;

import java.util.LinkedList;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Container;
import mezic.compiler.Debug;
import mezic.parser.LangUnitNode;

public class TContext extends AbsType {

  private static final long serialVersionUID = -6593051900415279360L;

  public static final String CLOSURE_PREFIX = "_Closure_";
  public static final String FUNC_IF_IMPL_PREFIX = "_FuncIfImpl_";

  public static final String VAR_THIS_NAME = "this";
  public static final String VAR_SUPER_NAME = "super";
  public static final String VAR_CLOSURE_NAME = "closure";
  public static final String VAR_EXCEPTION_NAME = "excp";

  transient protected CompilerLoader cpLoader = null;

  transient private int start_childvar_idx = 0;
  transient private int next_child_var_idx = 0;
  transient private int last_child_var_idx = -1;

  transient private AbsType owner_type;

  transient protected LinkedList<Container> childvar_list;
  transient protected LinkedList<TContext> childcontext_list;

  transient private LangUnitNode bind_node = null;

  transient private AbsTypeList handled_exception_list = null;

  public TContext(TContext owner_ctx, int form, String name, OpLvalue type_op, CompilerLoader cpLoader)
      throws CompileException {
    super(name);

    this.owner_type = owner_ctx;

    if (owner_ctx != null) {
      owner_ctx.addChildContext(this);
    }

    this.form = form;

    this.childcontext_list = new LinkedList<TContext>();
    this.childvar_list = new LinkedList<Container>();

    initChildVarIdx(0);

    this.type_op = type_op;
    this.cpLoader = cpLoader;

    this.handled_exception_list = new AbsTypeList();
  }

  public void initChildVarIdx(int idx) {
    this.start_childvar_idx = idx;
    this.next_child_var_idx = idx;
    this.last_child_var_idx = -1;

    Debug.println_dbg("initChildVarIdx: idx=" + idx);
  }

  public int allocChildVarIdx(int offset) {
    last_child_var_idx = next_child_var_idx;
    next_child_var_idx += offset;
    return last_child_var_idx;
  }

  public int getStartChidVarIdx() {
    return start_childvar_idx;
  }

  public int getNextChidVarIdx() {
    return next_child_var_idx;
  }

  public int getLastChildVarIdx() throws CompileException {
    if (this.last_child_var_idx == -1) {
      throw new CompileException("-1 last_child_var_idx should not be used");
    }

    return this.last_child_var_idx;
  }

  @Override
  public AbsType getOwnerType() throws CompileException { // this method should be
                                                        // overrided in child
                                                        // classes
    return owner_type;
  }

  public void updateOwnerType(AbsType owner) {
    Debug.println_dbg("Owner Type is updated with " + owner);
    this.owner_type = owner;
  }

  public void addChildContext(TContext child) {
    childcontext_list.add(child);
  }

  public TContext getChildContext(int i) {
    return childcontext_list.get(i);
  }

  public int getChildContextSize() {
    return childcontext_list.size();
  }

  public int getChildContextIndex(String name, int form, int search_start_idx) throws CompileException {
    TContext context = null;

    int length = childcontext_list.size();
    for (int i = search_start_idx; i < length; i++) {
      context = childcontext_list.get(i);

      if (context.isName(name) && context.isForm(form)) {
        return i;
      }

    }
    return -1;
  }

  public void bindNode(LangUnitNode node) throws CompileException {
    if (bind_node != null) {
      throw new CompileException("Context(" + this + ") is alredy bound to node(" + bind_node + ")");
    }

    this.bind_node = node;
  }

  public LangUnitNode getBindNode() {
    return this.bind_node;
  }

  @Override
  public String toString() {
    String info = "Context ";
    info += (getFormString(getForm()));
    info += ("(" + getName() + ")");

    /*
     * // member variable infor info += "\n"; Container container = null; int
     * length = variable_list.size(); for( int i = 0 ; i < length ; i++) {
     * container = variable_list.get(i); info += (" ("
     * +container.getContextIndex()+")"+container+"\n"); }
     */

    return info;
  }

  public String getMthdDscStr() throws CompileException {

    throw new CompileException("This method should not be called(except for class & function)");
  }

  public LinkedList<Container> get_childvar_list() {
    return childvar_list;
  }

  public void registerLocalChildVar(Container cont) throws CompileException {
    if (cont != null) {
      if (cont.getContextVarIdx() != -1) {
        throw new CompileException("Duplicated Container Registration");
      }
      childvar_list.add(cont);
      cont.setContext(this);
    } else {
      throw new CompileException("Registering variable is null");
    }

    Debug.println_dbg("New Container(" + cont + ") is registered");
  }

  public Container getChildVariable(String name) throws CompileException {
    Container container = getLocalChildVariable(name);

    if (container != null) {
      return container;
    }

    TContext parent_context = (TContext) this.getOwnerType();

    // variable search range is within class.
    // closure search range is within closure context
    // (closure context is a kind of class for function stack)
    for (;;) {
      container = parent_context.getLocalChildVariable(name);

      if (container != null) {
        return container;
      }

      if (parent_context.isForm(FORM_CLASS)) {
        break;
      }

      parent_context = (TContext) parent_context.getOwnerType();

    }

    return null;
  }

  @Override // override Type.getLocalChildVariable
  public Container getLocalChildVariable(String name) throws CompileException {
    Container container = null;
    int length = childvar_list.size();
    for (int i = 0; i < length; i++) {
      container = childvar_list.get(i);
      if (container.isName(name)) {
        return container;
      }
    }
    return null;
  }

  public Container getChildVariableWithVarIdx(int varidx) throws CompileException {
    Container container = getLocalChildVariableWithVarIdx(varidx);

    if (container != null) {
      return container;
    }

    TContext parent_context = (TContext) this.getOwnerType();

    for (; parent_context != null && !parent_context.isForm(FORM_TU);) {
      container = parent_context.getLocalChildVariableWithVarIdx(varidx);
      if (container != null) {
        return container;
      }
      parent_context = (TContext) parent_context.getOwnerType();
    }

    return null;
  }

  public Container getLocalChildVariableWithVarIdx(int varidx) throws CompileException {
    Container container = null;

    int size = childvar_list.size();
    for (int i = 0; i < size; i++) {
      container = childvar_list.get(i);
      if (container.getContextVarIdx() == varidx) {
        return container;
      }
    }

    return null;
  }

  // this method should not be used for searching member function(because of
  // polymorphism)
  public TContext getLocalChildContext(int type, String name) throws CompileException {
    TContext context = null;
    int length = childcontext_list.size();
    for (int i = 0; i < length; i++) {
      context = childcontext_list.get(i);
      if (context.getForm() == type && context.isName(name)) {
        return context;
      }
    }
    return null;
  }

  public boolean isLinealAscendent(TContext curr_ctx) throws CompileException {
    TContext ctx = curr_ctx;

    for (; ctx != null;) {
      if (ctx.isForm(AbsType.FORM_CLASS)) {
        return (this == ctx);
        /*
        if (this == ctx)
          return true;
        else
          return false;
        */
      }
      ctx = (TContext) ctx.getOwnerType();

    }
    return false;
  }

  public TContext getClosestAncestor(int form) throws CompileException {
    TContext ctx = this;

    for (; ctx != null;) {
      if (ctx.isForm(form)) {
        return ctx;
      }
      ctx = (TContext) ctx.getOwnerType();
    }

    return null;
  }

  public LinkedList<TContext> getClosestAncestorContextChain(int form) throws CompileException {
    LinkedList<TContext> list = new LinkedList<TContext>();

    TContext ctx = this;

    for (; ctx != null;) {
      list.addFirst(ctx);

      if (ctx.isForm(form)) {
        return list;
      }

      ctx = (TContext) ctx.getOwnerType();
    }

    return null;
  }

  public void dump_child_context(String prefix, int depth) {
    TContext context = null;

    System.out.println(prefix + "(" + depth + ")" + toString());

    int length = childcontext_list.size();
    for (int i = 0; i < length; i++) {
      context = childcontext_list.get(i);
      context.dump_child_context(prefix + " ", depth + 1);
    }
  }

  public void dump_localchild_varialbe(String prefix) {
    Container container = null;
    int length = childvar_list.size();
    System.out.println(prefix + "dump_localchild_varialbe");
    for (int i = 0; i < length; i++) {
      container = childvar_list.get(i);
      System.out.println(prefix + " (" + container.getContextVarIdx() + ")" + container);
    }
  }

  public AbsTypeList getHandledExceptionList() {
    return this.handled_exception_list;
  }

  public boolean isAddressableException(AbsType excp_type) throws CompileException {
    Debug.assertion(getClosestAncestor(AbsType.FORM_FUNC) != null, "It should be under function context");

    TContext ctx = this;

    for (; ctx != null;) {
      switch (ctx.getForm()) {
      case AbsType.FORM_STREAM:
        if (ctx.getHandledExceptionList().has(excp_type)) {
          return true;
        }
        break;

      case AbsType.FORM_FUNC:
        TContextFunc func_ctx = (TContextFunc) ctx;
        AbsTypeList excptype_list = func_ctx.getThrowsList();
        Debug.println_dbg("func_ctx=" + func_ctx);
        Debug.println_dbg("excptype_list=" + excptype_list);
        if (excptype_list != null) {
          return excptype_list.has(excp_type);
        } else {
          return false;
        }
      default:
        // do nothing
        throw new CompileException("Undefined Context form:" + ctx.getForm());
      }
      ctx = (TContext) ctx.getOwnerType();
    }

    return false;
  }

}
