package mezic.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import mezic.compiler.ASTTraverseVisitor;
import mezic.compiler.CompileException;
import mezic.compiler.Container;
import mezic.compiler.Debug;
import mezic.compiler.type.AbsType;
import mezic.compiler.type.AbsTypeList;
import mezic.compiler.type.TContext;

import org.objectweb.asm.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LangUnitNode extends SimpleNode implements ParserTreeConstants {

  private static final long serialVersionUID = -2324904526672602403L;
  
  private static final Logger LOG = LoggerFactory.getLogger(LangUnitNode.class);

  public LangUnitNode() {
    super(0);
  }

  public LangUnitNode(int i) {
    super(i);
  }

  public LangUnitNode(Parser p, int id) {
    super(p, id);
  }

  private boolean is_rotated = false;

  public void rotated() {
    is_rotated = true;
  }

  public boolean isRotated() {
    return is_rotated;
  }

  public Object node_run(ASTTraverseVisitor visitor, Object data) throws CompileException {

    Object retObj = null;

    // try{
    // visitor.check_interrupt();

    if (Debug.enable_print_info) {
      visitor.visit_log(this);
    }

    // jjtAccept will call the vistor.visit for this Node. It use poly-morphism.
    retObj = jjtAccept(visitor, data);

    /*
     * } catch (CompileException e) { //e.printStackTrace();
     * if(!e.node_initialized()) { e.set_node(this); }
     *
     * throw e; } catch(Exception e){
     *
     * e.printStackTrace();
     *
     * CompileException excp = new CompileException("tgt exception", this);
     * excp.setTargetException(e); throw excp; }
     */

    return retObj;
  }

  public int getNodeId() {
    return id;
  }

  public boolean isNodeId(int id) {
    return (getNodeId() == id);
    /*
    if (getNodeId() == id)
      return true;
    else
      return false;
    */
  }

  public LangUnitNode getChildren(int index) {
    if (children == null) {
      return null;
    }

    if (index >= children.length) {
      return null;
    }

    return (LangUnitNode) children[index];
  }

  public int getChildIdx(LangUnitNode child_node) {
    for (int i = 0; i < children.length; i++) {
      if (((LangUnitNode) children[i]) == child_node) {
        return i;
      }
    }

    return -1;
  }

  public int getChildIdxWithId(int id, int start_idx) {
    if (children == null) {
      return -1;
    }

    for (int i = start_idx; i < children.length; i++) {
      if (((LangUnitNode) children[i]).isNodeId(id)) {
        return i;
      }
    }

    return -1;
  }

  public LangUnitNode[] getChildNodeArray() {
    if (children.length == 0) {
      return null;
    }

    LangUnitNode[] nodes = new LangUnitNode[children.length];

    for (int i = 0; i < nodes.length; i++) {
      nodes[i] = (LangUnitNode) children[i];
    }

    return nodes;
  }

  public void jjtSetChild(int index, Node child) throws CompileException {

    if (children == null) {
      throw new CompileException("node children is invalid");
    }

    if (index >= children.length) {
      throw new CompileException("invalid index(" + index + ") where children.length(" + children.length + ")");
    }

    children[index] = child;
  }

  public void jjtDelChild(int i) throws CompileException {

    if (children == null) {
      throw new CompileException("Invalid jjtDelChild(no children)");
    }

    if (i < 0 || i >= children.length) {
      throw new CompileException("Invalid jjtDelChild(len:" + children.length + ") del index(" + i + ")");
    }

    if (children.length == 1) {
      children = null;
      return;
    }

    Node [] c = new Node[children.length - 1];

    System.arraycopy(children, 0, c, 0, i);

    if (children.length - i - 1 > 0) {
      System.arraycopy(children, i + 1, c, i, children.length - i - 1);
    }

    children = c;
  }

  public void jjtPushChild(Node n, int i) throws CompileException {

    Debug.assertion(children != null, "children should be valid");
    Debug.assertion(i >= 0, "i(" + i + ") should be bigger than 0");
    Debug.assertion(children.length > i, "children size(" + children.length + ") should be over i(" + i + ")");

    Node [] c = new Node[children.length + 1];
    System.arraycopy(children, 0, c, 0, i);
    c[i] = n;
    System.arraycopy(children, i, c, i + 1, children.length - i);
    children = c;
  }

  public LangUnitNode getClosestParent(int id) throws CompileException {
    return getClosestParentUntil(id, null);
  }

  public LangUnitNode getClosestParentUntil(int id, LangUnitNode until_node) throws CompileException {
    LangUnitNode node = this;

    for (int i = 0; i < 10000; i++) {
      node = (LangUnitNode) node.jjtGetParent();

      if (node == null) {
        return null;
      }

      if (node.isNodeId(id)) {
        return node;
      }

      if (node == until_node) {
        return null;
      }
    }

    throw new CompileException("AST Max depth is excceeded");
  }

  public LangUnitNode getClosestParentsUntil(int[] ids, LangUnitNode until_node) throws CompileException {
    LangUnitNode node = this;

    for (int i = 0; i < 10000; i++) {
      node = (LangUnitNode) node.jjtGetParent();

      if (node == null) {
        return null;
      }

      for (int j = 0; j < ids.length; j++) {
        if (node.isNodeId(ids[j])) {
          return node;
        }
      }

      if (node == until_node) {
        return null;
      }
    }

    throw new CompileException("AST Max depth is excceeded");
  }

  private Token ast_token = null;

  public void initAstToken(Token t) throws ParseException {
    if (ast_token != null) {
      throw new ParseException("duplicated ast_token initialization" + toString());
    }
    this.ast_token = t;
  }

  public Token getAstToken() throws CompileException {
    /*
     * if( ast_token != null ) return ast_token; else throw new
     * InterpreterException("ast_token is null in "+toString(), this);
     */
    return ast_token;
  }

  public String getNodeName() {
    return jjtNodeName[id];
  }

  private String getTokenInfo(Token t) {
    if (t != null) {
      String str = t.toString();
      if (str.indexOf("\n") != -1) {
        str = "\\n";
      }
      return "(line:" + t.beginLine + " column:" + t.beginColumn + "[" + ParserConstants.tokenImage[t.kind] + "]" + " '"
          + str + "')";
    } else {
      return "(null token)";
    }
  }

  public String toString() {
    String msg;

    if (ast_token != null) {
      msg = getNodeName() + getTokenInfo(ast_token);
    } else {
      msg = getNodeName() + getTokenInfo(jjtGetFirstToken());
    }

    if (this.isNodeId(JJTREFERENCE)) {
      if (this.isDefinition()) {
        msg += " [Definition]";
      }
    }

    // if( namedType != null ) msg += ("[itf: "+namedType+"]");

    return msg;
  }

  public int dump_tree(String prefix, int depth, int seq) {
    System.out.printf("\n-- Tree Dump -------------------------------------------------------------------------\n");
    int ret = dump_tree_recursive(prefix, depth, seq);
    // System.out.printf("------------------------------------------------------------------------------------\n");
    return ret;
  }

  public int dump_tree_recursive(String prefix, int depth, int seq) {
    int num_child = this.jjtGetNumChildren();
    LangUnitNode child = null;

    // if(depth == 0 || depth == 1)
    System.out.printf("(%4d)", seq++);
    System.out.println(prefix + "(" + depth + ")" + toString());
    for (int i = 0; i < num_child; i++) {
      child = (LangUnitNode) this.getChildren(i);
      seq = child.dump_tree_recursive(prefix + ".", depth + 1, seq);
    }

    return seq;
  }

  transient private TContext bind_context = null;

  public void bindContext(TContext context) throws CompileException {
    Debug.assertion(bind_context == null, "This node(" + this + ") is already bound to context(" + bind_context + ")");

    this.bind_context = context;

    bind_context.bindNode(this);
  }

  public TContext getBindContext() {
    return this.bind_context;
  }

  /* For Reference Node - configured by preprocessor */
  private boolean is_definition = false;

  public void setIsDefinition(boolean declare) {
    this.is_definition = declare;
  }

  public boolean isDefinition() {
    return is_definition;
  }

  public boolean isOperationNode() {
    switch (this.id) {
    case JJTASSIGNMENT:
    case JJTJUMPEXPR:
    case JJTONEEXPRFUNCBODY:
    case JJTARGUMENT:
    case JJTEXPLICITCAST: // currently this node is void node
      return true;
    default:
      return isNonAssignOperationNode();
    }
  }

  public boolean isNonAssignOperationNode() {
    switch (this.id) {
    case JJTCONDITIONAL:
    case JJTLOGICALOR:
    case JJTLOGICALAND:
    case JJTINCLUSIVEOR:
    case JJTEXCLUSIVEOR:
    case JJTAND:
    case JJTEQUALITY:
    case JJTRELATIONAL:
    case JJTSHIFT:
    case JJTADDITIVE:
    case JJTMULTIPLICATIVE:
    case JJTPOSTFIX:
      return true;
    default:
      return false;
    }
  }

  public boolean isBooleanReduceNode() throws CompileException {
    Token t = null;

    switch (this.id) {
    case JJTIFCONDEXPR:
      return true;
    case JJTUNARY:
      t = this.getAstToken();
      Debug.assertion(t != null, "t should be valid");

      switch (t.kind) {
      case ParserConstants.UNARY_NOT:
        return true;
      default:
        return false;
      }
    default:
      return false;
    }
  }

  public boolean requiringOpStackRvalue() throws CompileException {
    if (isNonAssignOperationNode()) {
      return true;
    }

    switch (this.id) {
    case JJTASSIGNMENT:
    case JJTARGUMENT:
    case JJTSTREAMBLOCKELEMENT:
    case JJTONEEXPRFUNCBODY:
    case JJTMATCHCASEBODYEXPR:
    case JJTFORACTION:
    case JJTFORBODY:
    case JJTJUMPEXPR:
      return true;

    case JJTIFEXPR: // this is for if false(else) case
    case JJTIFCASEEXPR: // this is for if true case
      // example)
      // IfExpr
      // / | \
      // / | \
      // / | \
      // IfCaseExpr [ElseExpr] Expr(AssignExpr)
      // / \
      // / \
      // IfCond Expr(AssignExpr)
      //
      // def reference checking is not required...
      // because if ref node is definition, IfExpr Node will do 'pop'
      // if( ! hasDefinitionRefernce() ) return true;
      // break;
    case JJTIFCONDEXPR: // this is for 'if( bv = true )' case
      return true;
    default:
      return false;
    }
  }

  public boolean hasDefinitionRefernce() throws CompileException {
    LangUnitNode ref_node = this.getClosestParent(JJTREFERENCE);
    Debug.assertion(ref_node != null, "ref_node should not be invalid");

    return ref_node.isDefinition();
  }

  /* For Stream Type */
  public final static int STREAMFORM_DFLT = 0;
  public final static int STREAMFORM_CLASS = 1;

  private int stream_form = -1;

  public void initStreamForm(int form) {
    this.stream_form = form;
  }

  public int getStreamForm() {
    return this.stream_form;
  }

  public final static int FUNCFORM_MEMBER = 0;
  public final static int FUNCFORM_STATIC = 1;

  private int func_form = FUNCFORM_MEMBER;

  public void initFuncForm(int form) {
    this.func_form = form;
  }

  public int getFuncForm() {
    return this.func_form;
  }

  /* for Named Itf */
  private String namedType = null;

  public void appendNamedTypeToken(String token) {
    if (namedType == null) {
      namedType = token;
    } else {
      namedType += ("/" + token);
    }
  }

  public String getNamedType() {
    return namedType;
  }

  /*
   * private Type var_itf = null; public void setVarItf(Type itf) { this.var_itf
   * = itf; } public Type getVarItf() { return this.var_itf; }
   */

  /* For SymbolName Node */

  public final static int VAR_T_VAR = 0;
  public final static int VAR_T_CONST = 1;
  public final static int VAR_T_SINGLETON = 2;
  public final static int VAR_T_INTERFACE = 3;

  private int var_type = VAR_T_VAR; // default

  public void setVarType(int type) {
    this.var_type = type;
  }

  public int getVarType() {
    return this.var_type;
  }

  transient private Container container;

  public void setContainer(Container container) {
    /* Debug.println_dbg("setContainer("+container+")"); */ this.container = container;
  }

  public Container getContainer() {
    /* Debug.println_dbg("getContainer:"+container); */ return container;
  }

  /*
   * Default Context is 1) Class Context indicated by 'this' variable 2) Closure
   * Context indicated by 'closure' variable ( Closure Context also can have
   * 'this' variable as its member ... )
   *
   * Default Context Member Means 1) member of 'this' variable in Method 2)
   * member of 'closure' variable in Closure
   */
  transient private boolean is_dflt_ctx_member = false;

  public void setIsDfltContextMember(boolean is_member) {
    is_dflt_ctx_member = is_member;
  }

  public boolean isDfltContextMember() {
    return is_dflt_ctx_member;
  }

  /* For Nodes having reduce type */
  private boolean has_reducetype_node = false; // configured by parser

  public void setHasReduceTypeNode(boolean has) {
    has_reducetype_node = has;
  }

  public boolean hasReduceTypeNode() {
    return has_reducetype_node;
  }

  /* For Access Node */
  transient private boolean is_assign_tgt = false;

  public void setAssignTgt(boolean is_assign_tgt) throws CompileException {
    if (!this.isNodeId(JJTACCESS) && !this.isNodeId(JJTSUBACCESS) && !this.isNodeId(JJTMAPACCESS)) {
      throw new CompileException("This Method is for Access Node");
    }
    LOG.debug("Set Assign Tgt" + this);
    this.is_assign_tgt = is_assign_tgt;
  }

  public boolean isAssignTgt() throws CompileException {
    if (!this.isNodeId(JJTACCESS) && !this.isNodeId(JJTSUBACCESS) && !this.isNodeId(JJTMAPACCESS)) {
      throw new CompileException("This Method is for Access Node");
    }
    return this.is_assign_tgt;
  }

  /* For ApplyType Node */
  transient private AbsType apply_type = null;

  public void setApplyType(AbsType type) {
    /* Debug.println_dbg("setApplyType("+type+") on " + this+")"); */ apply_type = type;
  }

  public AbsType getApplyType() {
    return apply_type;
  }

  /* For Type Converting */
  transient private AbsType converting_type = null;

  public void setConvertingType(AbsType type) throws CompileException {
    if (type == null) {
      throw new CompileException("converting_type should not be null", this);
    }

    if (converting_type != null) {
      throw new CompileException("converting_type is not null(" + converting_type + ")", this);
    }
    converting_type = type;
  }

  public void clearConvertingType() {
    converting_type = null;
  }

  public AbsType getConvertingType() {
    return converting_type;
  }

  transient private AbsTypeList argument_type_list = null;

  public void setArgumentTypeList(AbsTypeList list) {
    argument_type_list = list;
  }

  public AbsTypeList getArgumentTypeList() {
    return argument_type_list;
  }

  /* For If condition, else node */
  transient private Label br_label = null;

  public void setBrLabel(Label br_label) {
    this.br_label = br_label;
  }

  public Label getBrLabel() {
    return this.br_label;
  }

  /* For CStyle Loop */
  transient private Label loopcond_label = null;

  public void setLoopCondLabel(Label label) {
    this.loopcond_label = label;
  }

  public Label getLoopCondLabel() {
    return this.loopcond_label;
  }

  transient private Label loopbody_label = null;

  public void setLoopBodyLabel(Label label) {
    this.loopbody_label = label;
  }

  public Label getLoopBodyLabel() {
    return this.loopbody_label;
  }

  transient private Label loopaction_label = null;

  public void setLoopActionLabel(Label label) {
    this.loopaction_label = label;
  }

  public Label getLoopActionLabel() {
    return this.loopaction_label;
  }

  transient private Label loopend_label = null;

  public void setLoopEndLabel(Label label) {
    this.loopend_label = label;
  }

  public Label getLoopEndLabel() {
    return this.loopend_label;
  }

  /* For Match */
  transient private Label match_case_label = null;

  public void setMatchCaseLabel(Label label) {
    this.match_case_label = label;
  }

  public Label getMatchCaseLabel() {
    return this.match_case_label;
  }

  transient private Label match_end_label = null;

  public void setMatchEndLabel(Label label) {
    this.match_end_label = label;
  }

  public Label getMatchEndLabel() {
    return this.match_end_label;
  }

  /* For List Catch */
  transient private AbsTypeList catch_excplist = null;

  public void setCatchExceptionList(AbsTypeList list) {
    this.catch_excplist = list;
  }

  public AbsTypeList getCatchExceptionList() {
    return this.catch_excplist;
  }

  transient private Label stream_end_label = null;

  public void setStreamEndLabel(Label label) {
    this.stream_end_label = label;
  }

  public Label getStreamEndLabel() {
    return this.stream_end_label;
  }

  transient private Label stremblock_start_label = null;

  public void setStreamBlockStartLabel(Label label) {
    this.stremblock_start_label = label;
  }

  public Label getStreamBlockStartLabel() {
    return this.stremblock_start_label;
  }

  transient private Label streamblock_end_label = null;

  public void setStreamBlockEndLabel(Label label) {
    this.streamblock_end_label = label;
  }

  public Label getStreamBlockEndLabel() {
    return this.streamblock_end_label;
  }

  transient private Label catch_label = null;

  public void setCatchLabel(Label label) {
    this.catch_label = label;
  }

  public Label getCatchLabel() {
    return this.catch_label;
  }

  transient private boolean is_set_dup_x1 = false;

  public void setDupX1(boolean is_set) {
    this.is_set_dup_x1 = is_set;
  }

  public boolean isSetDupX1() {
    return this.is_set_dup_x1;
  }

  transient private boolean is_set_dup = false;

  public void setDup(boolean is_set) {
    this.is_set_dup = is_set;
  }

  public boolean isSetDup() {
    return this.is_set_dup;
  }

  transient private boolean is_set_dup2 = false;

  public void setDup2(boolean is_set) {
    this.is_set_dup2 = is_set;
  }

  public boolean isSetDup2() {
    return this.is_set_dup2;
  }

  private int map_dimension = 0;

  public void incMapDimension() {
    map_dimension++;
  }

  public int getMapDimesion() {
    return map_dimension;
  }

  transient private Container vararg_maptype_cont = null;

  public void setVarArgMapTypeContainer(Container cont) {
    vararg_maptype_cont = cont;
  }

  public Container getVarArgMapTypeContainer() {
    return vararg_maptype_cont;
  }

  transient private int vararg_map_idx = -1;

  public void setVarArgMapIdx(int idx) {
    vararg_map_idx = idx;
  }

  public int getVarArgMapIdx() {
    return vararg_map_idx;
  }

  transient private int vararg_map_size = 0;

  public void setVarArgMapSize(int size) {
    vararg_map_size = size;
  }

  public int getVarArgMapSize() {
    return vararg_map_size;
  }

  transient private boolean is_applied_func = false;

  public void setAppliedFunc(boolean applied_func) {
    is_applied_func = applied_func;
  }

  public boolean isAppliedFunc() {
    return is_applied_func;
  }

  private Label logicalop_branch_label = null;

  public void setLogicalOpBrLabel(Label label) {
    logicalop_branch_label = label;
  }

  public Label getLogicalOpBrLabel() {
    return logicalop_branch_label;
  }

  private boolean is_alive_ctrl_flow = true;

  public boolean isAliveCtrlFlow() {
    return is_alive_ctrl_flow;
  }

  public void setAliveCtrlFlow(boolean is_alive) {
    this.is_alive_ctrl_flow = is_alive;
  }

  public void serialize(OutputStream out) throws CompileException {
    ObjectOutputStream oos = null;

    // backup parent
    LangUnitNode tmp_parent = (LangUnitNode) this.parent;
    this.parent = null;

    try {
      oos = new ObjectOutputStream(out);
      oos.writeObject(this);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new CompileException(e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new CompileException(e.getMessage());
    }

    try {
      if (oos != null) {
        oos.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new CompileException(e.getMessage());
    }

    // recover parent
    this.parent = tmp_parent;
  }

  public static LangUnitNode de_serialize(InputStream in) throws CompileException {
    ObjectInputStream ois = null;
    LangUnitNode node = null;

    try {
      ois = new ObjectInputStream(in);
      node = (LangUnitNode) ois.readObject();
      ois.close();
    } catch (IOException e) {
      e.printStackTrace();
      throw new CompileException(e.getMessage());
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new CompileException(e.getMessage());
    }

    if (node == null) {
      throw new CompileException("de-seriliazed node should be valid");
    }

    return node;
  }

}
