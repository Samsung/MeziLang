package mezic.compiler;

import java.util.LinkedList;

import mezic.compiler.type.AbsType;
import mezic.compiler.type.TContext;
import mezic.compiler.type.TContextFunc;
import mezic.compiler.type.TraverseStackNode;
import mezic.parser.ASTAND;
import mezic.parser.ASTAccess;
import mezic.parser.ASTAccessCtrl;
import mezic.parser.ASTAdditive;
import mezic.parser.ASTApplyType;
import mezic.parser.ASTArgument;
import mezic.parser.ASTArgumentHdr;
import mezic.parser.ASTArgumentList;
import mezic.parser.ASTAssignment;
import mezic.parser.ASTAssignmentOperator;
import mezic.parser.ASTCatch;
import mezic.parser.ASTCatchExceptionList;
import mezic.parser.ASTConditional;
import mezic.parser.ASTConstant;
import mezic.parser.ASTCstyleLoopExpr;
import mezic.parser.ASTElseExpr;
import mezic.parser.ASTEquality;
import mezic.parser.ASTExclusiveOR;
import mezic.parser.ASTExplicitCast;
import mezic.parser.ASTForAction;
import mezic.parser.ASTForBody;
import mezic.parser.ASTForCondition;
import mezic.parser.ASTForInit;
import mezic.parser.ASTFuncDefMetaData;
import mezic.parser.ASTFunctionDef;
import mezic.parser.ASTFunctionDefHdr;
import mezic.parser.ASTFunctionDefSignature;
import mezic.parser.ASTFunctionType;
import mezic.parser.ASTIfCaseExpr;
import mezic.parser.ASTIfCondExpr;
import mezic.parser.ASTIfExpr;
import mezic.parser.ASTImport;
import mezic.parser.ASTImportClass;
import mezic.parser.ASTInclusiveOR;
import mezic.parser.ASTInvoke;
import mezic.parser.ASTInvokeHdr;
import mezic.parser.ASTJumpExpr;
import mezic.parser.ASTLogicalAND;
import mezic.parser.ASTLogicalANDHdr;
import mezic.parser.ASTLogicalOR;
import mezic.parser.ASTLogicalORHdr;
import mezic.parser.ASTLoopExpr;
import mezic.parser.ASTLoopHdrExpr;
import mezic.parser.ASTMapAccess;
import mezic.parser.ASTMapType;
import mezic.parser.ASTMatchCaseBodyExpr;
import mezic.parser.ASTMatchCaseExpr;
import mezic.parser.ASTMatchCaseHeadExpr;
import mezic.parser.ASTMatchExpr;
import mezic.parser.ASTMatchHeadExpr;
import mezic.parser.ASTMetaData;
import mezic.parser.ASTMetaExtendsData;
import mezic.parser.ASTMetaImplementsData;
import mezic.parser.ASTMetaThrowsData;
import mezic.parser.ASTMultiplicative;
import mezic.parser.ASTNamedType;
import mezic.parser.ASTOneExprFuncBody;
import mezic.parser.ASTParameter;
import mezic.parser.ASTParameterList;
import mezic.parser.ASTParameterTypeList;
import mezic.parser.ASTPostfix;
import mezic.parser.ASTReduceType;
import mezic.parser.ASTReference;
import mezic.parser.ASTRelational;
import mezic.parser.ASTShift;
import mezic.parser.ASTStream;
import mezic.parser.ASTStreamBlock;
import mezic.parser.ASTStreamBlockElement;
import mezic.parser.ASTStreamBlockElementHdr;
import mezic.parser.ASTStreamBlockHdr;
import mezic.parser.ASTStreamHeader;
import mezic.parser.ASTStreamSignature;
import mezic.parser.ASTStreamType;
import mezic.parser.ASTSubAccess;
import mezic.parser.ASTSubReference;
import mezic.parser.ASTSymbolName;
import mezic.parser.ASTTranslationUnit;
import mezic.parser.ASTTranslationUnitElement;
import mezic.parser.ASTTranslationUnitHeader;
import mezic.parser.ASTType;
import mezic.parser.ASTTypeDefine;
import mezic.parser.ASTUnary;
import mezic.parser.ASTUnaryOperator;
import mezic.parser.LangUnitNode;
import mezic.parser.ParserTreeConstants;
import mezic.parser.ParserVisitor;
import mezic.parser.SimpleNode;
import mezic.util.Util;

import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ASTTraverseVisitor implements ParserVisitor, java.io.Serializable, Opcodes, ParserTreeConstants {
  private static final long serialVersionUID = 5989019583639641868L;

  private static final Logger LOG = LoggerFactory.getLogger(ASTTraverseVisitor.class);

  //protected String entry_point;
  //protected Container[] cont_args;

  transient protected CompilerLoader cpLoader;

  /* Context of Calling function */
  protected LinkedList<TContext> context_stack = null;
  /* Reduction Stack */
  protected LinkedList<Reduction> reduction_stack = null;
  /* Traverse Stack */
  protected LinkedList<TraverseStackNode> traverse_stack = null;
  /* Branch Stack */
  protected BranchTree branch_tree = null;

  protected Container retContainer = null;
  protected TraverseStackNode currTraverse = null;
  protected TraverseStackNode nextTraverse = null;

  protected int run_cnt = 0;

  public static boolean visit_log_on = true;
  private int depth = 0;
  private String log_depth_indent = "";
  private int seq = 0;

  public ASTTraverseVisitor() {
    super();
    init(null);
  }

  public ASTTraverseVisitor(CompilerLoader loader) {
    super();
    init(loader);
  }

  private void init(CompilerLoader loader) {
    this.cpLoader = loader;

    if (loader != null) {
      this.context_stack = loader.getContextStack();
      this.reduction_stack = loader.getReductionStack();
      this.branch_tree = loader.getBranchTree();
    }

    // After creation of visitor, some traverse node can be pushed to traverse
    // stack
    // Before visitor.run() following stack and log should be initialized
    traverse_stack = new LinkedList<TraverseStackNode>();
    visit_log_init();
  }

  public void pushContext(TContext context) throws CompileException {
    if (context_stack == null) {
      throw new CompileException("stack is not initialized");
    }

    context_stack.push(context);
  }

  public TContext popContext() throws CompileException {
    if (context_stack == null) {
      throw new CompileException("app stack is not initialized");
    }

    TContext context = context_stack.pop();

    if (context == null) {
      throw new CompileException("app stack is empty");
    }

    return context;
  }

  public TContext getTopContext() throws CompileException {
    if (context_stack == null) {
      throw new CompileException("app stack is not initialized");
    }

    TContext context = null;

    if (context_stack.size() > 0) {
      context = context_stack.get(0);
      if (context == null) {
        throw new CompileException("app stack is empty");
      }
    }

    return context;
  }

  public TContext getBottomContext() throws CompileException {
    if (context_stack == null) {
      throw new CompileException("app stack is not initialized");
    }

    // Context context = null;
    TContext context = context_stack.get(context_stack.size() - 1);
    if (context == null) {
      throw new CompileException("app stack is empty");
    }

    return context;
  }

  public String dump_context_stack() {
    if (context_stack == null) {
      return "";
    }

    int size = context_stack.size();

    TContext context = null;

    String stackTraceStr = "";

    for (int i = 0; i < size; i++) {
      if (size > 100) {
        if (i == 51) {
          stackTraceStr += " ...\n";
        }

        if (i > 50 && i < (size - 50)) {
          continue;
        }
      }

      context = context_stack.get(i);

      if (context != null) {
        stackTraceStr = stackTraceStr + "#" + (size - 1 - i) + " " + context.toString() + "\n";
      }
    }

    LOG.debug("--Dump App Stack-------------------------------------");
    LOG.debug(stackTraceStr);
    LOG.debug("-----------------------------------------------------");

    return stackTraceStr;
  }

  protected void visit_log_init() {
    if (!visit_log_on) {
      return;
    }

    depth = 0;
    log_depth_indent = "";
    seq = 0;
  }

  protected String logDepthIndent() {
    return log_depth_indent;
  }

  public void visit_log(LangUnitNode node) {
    if (!visit_log_on) {
      return;
    }

    System.out.printf("[%4d]", seq++);
    System.out.println(log_depth_indent + "(" + depth + ")" + node.toString());
  }

  public void visit_log_inc_depth() {
    if (!visit_log_on) {
      return;
    }

    depth++;
    log_depth_indent += " ";
  }

  public void visit_log_dec_depth() {
    if (!visit_log_on) {
      return;
    }

    depth--;
    if (log_depth_indent.length() > 0) {
      log_depth_indent = log_depth_indent.substring(0, log_depth_indent.length() - 1);
    }
  }

  public int visit_log_depth() {
    return depth;
  }

  public int visit_log_seq() {
    return seq;
  }

  public String visit_log_indent() {
    return log_depth_indent;
  }

  public void check_interrupt() throws CompileException {
    if (Thread.interrupted()) {
      System.err.println("Interrupted");
      throw new CompileException("Interrupted");
    }
  }

  /*
   * protected void Debug.assertion(boolean condition, String statement) throws
   * InterpreterException { if( ! condition ) { throw new InterpreterException(
   * statement); } }
   */

  protected void check_numof_childnode(int expecting_num, LangUnitNode node) throws CompileException {
    /* child node checking */
    int num_child = node.jjtGetNumChildren();
    if (num_child != expecting_num) {
      throw new CompileException("child#(" + num_child + ") is invalid", node);
    }
  }

  public void pushTraverse(TraverseStackNode node) throws CompileException {
    TraverseStackNode.push(traverse_stack, node);
    visit_log_inc_depth();
  }

  public TraverseStackNode popTraverse() throws CompileException {
    TraverseStackNode traverse_node = TraverseStackNode.pop(traverse_stack);
    visit_log_dec_depth();
    return traverse_node;
  }

  public void popTraverseUntilTopIs(int[] node_id) throws CompileException {
    TraverseStackNode traverse = null;

    Debug.assertion(node_id != null && node_id.length != 0, "node_id list should not be null & length should not be 0");

    for (;;) {
      traverse = topTraverse();

      // if traverse stack does not have node(having node_id), it means error
      Debug.assertion(traverse != null, "traverse stack top should not be null");

      for (int i = 0; i < node_id.length; i++) {
        // LOG.debug("{}: ({}) {}", traverse.getNode().getNodeId(), i,
        // node_id[i]);
        if (traverse.getNode().isNodeId(node_id[i])) {
          return;
        }
      }

      popTraverse();
    }

  }

  public TraverseStackNode topTraverse() throws CompileException {
    return TraverseStackNode.getTop(traverse_stack);
  }

  public int depthTraverse() throws CompileException {
    Debug.assertion(traverse_stack != null, "traverse_stack should not be null");

    return traverse_stack.size();
  }

  public void dumpTraverse() throws CompileException {
    TraverseStackNode.dump_stack(traverse_stack, "       ");
  }

  public void pushReduction(Reduction node) throws CompileException {
    Debug.assertion(reduction_stack != null, "reduction_stack should not be null");

    TContext top_ctx = getTopContext();
    Debug.assertion(top_ctx != null, "Top Context should not be invalid");
    reduction_stack.push(node);

    LOG.debug("push reduction({})", node);
  }

  public Reduction popReduction() throws CompileException {
    Debug.assertion(reduction_stack != null, "reduction_stack should not be null");

    if (reduction_stack.size() == 0) {
      return null;
    }

    Reduction node = (Reduction) reduction_stack.pop();

    LOG.debug("popped reduction({})", node);

    return node;
  }

  public Reduction topReduction() throws CompileException {
    Debug.assertion(reduction_stack != null, "reduction_stack should not be null");

    if (reduction_stack.size() == 0) {
      return null;
    }

    return reduction_stack.get(0);
  }

  public int depthReduction() throws CompileException {
    Debug.assertion(reduction_stack != null, "reduction_stack should not be null");

    return reduction_stack.size();
  }

  public void dump_reduction_stack() {
    int size = reduction_stack.size();
    LOG.debug("--lasting reduce stack size ({})-----------------------", size);
    for (int i = 0; i < size; i++) {
      LOG.debug("({}) {}", (size - i - 1), reduction_stack.get(i));
    }
    LOG.debug("------------------------------------------------------");
  }

  public boolean isReferencedRefNode(LangUnitNode ref_node) throws CompileException {
    Debug.assertion(ref_node.isNodeId(JJTREFERENCE), "ref_node should ReferenceNode");

    LangUnitNode ref_parent_node = (LangUnitNode) ref_node.jjtGetParent();

    if (ref_parent_node.isOperationNode()) {
      return true;
    } else if (ref_node.jjtGetNumChildren() == 2) { // ref-node has sub-ref-node
      LangUnitNode subref_node = (LangUnitNode) ref_node.jjtGetChild(1);
      Debug.assertion(subref_node.isNodeId(JJTSUBREFERENCE), "Child Node should be ref or sub ref node, but");
      return true;
    } else {
      LangUnitNode parent_ref_node = ref_node.getClosestParent(JJTREFERENCE);

      // root definition is class and function and,
      // the child of definition is definition
      if (parent_ref_node != null && parent_ref_node.isDefinition()) {
        return false;
      }
    }

    return true;
  }

  /*
   * public boolean hasDefinitionRefernce(SpringUnitNode node) throws
   * InterpreterException { SpringUnitNode ref_node =
   * node.getClosestParent(JJTREFERENCE); Debug.assertion(ref_node!=null,
   * "ref_node should not be invalid");
   *
   * return ref_node.isDefinition(); }
   */

  public TContextFunc getClosestFunContext() throws CompileException {
    TContext func_context = getTopContext().getClosestAncestor(AbsType.FORM_FUNC);
    Debug.assertion(func_context != null, "func_context should not be invalid");
    Debug.assertion(func_context.isForm(AbsType.FORM_FUNC),
        "Invalid Context From(" + func_context.getFormString(func_context.getForm()) + ")");

    return (TContextFunc) func_context;
  }

  public LangUnitNode getEdgeChildSubRefeNode(LangUnitNode node) throws CompileException {
    Debug.assertion(node.isNodeId(JJTREFERENCE) || node.isNodeId(JJTSUBREFERENCE),
        "node should be Ref or Sub Ref Node");

    LangUnitNode subref_node = null;
    LangUnitNode next_node = null;

    /*
     * Reference / \ Access SubReference <-- Edge Child Sub Referencing Node /
     * SubReference / SubReference
     */

    if (node.isNodeId(JJTREFERENCE)) {
      if (node.jjtGetNumChildren() == 2) {
        subref_node = (LangUnitNode) node.jjtGetChild(1);
        Debug.assertion(subref_node.isNodeId(JJTSUBREFERENCE), "Child Node should be ref or sub ref node, but");
        return subref_node;
      } else { // it does not have sub reference
        return null;
      }
    } else if (node.isNodeId(JJTSUBREFERENCE)) {
      subref_node = (LangUnitNode) node.jjtGetParent();
      Debug.assertion(subref_node != null, "Parent Node should not be null");

      if (subref_node.isNodeId(JJTREFERENCE)) {
        // it does not have sub reference
        return null;
      }

      int i = 0;
      for (; i < CompilerLoader.MAX_SUB_REF_LEVEL; i++) {
        next_node = (LangUnitNode) subref_node.jjtGetParent();
        Debug.assertion(next_node != null, "Parent Node should not be null");

        if (next_node.isNodeId(JJTREFERENCE)) {
          return subref_node;
        }

        subref_node = next_node;
      }

      throw new CompileException("Maximum Sub Referencing Level(" + CompilerLoader.MAX_SUB_REF_LEVEL + " is excceeded");
    } else {
      throw new CompileException("Parameter node should be Ref or Sub Ref Node, but(" + node.getNodeId() + ")");
    }
  }

  public LangUnitNode getNextChildSubRefeNode(LangUnitNode node) throws CompileException {
    Debug.assertion(node.isNodeId(JJTREFERENCE) || node.isNodeId(JJTSUBREFERENCE),
        "node should be Ref or Sub Ref Node");

    LangUnitNode subref_node = null;

    /*
     * Reference / \ Access SubReference / SubReference / SubReference
     */

    if (node.isNodeId(JJTREFERENCE)) {
      if (node.jjtGetNumChildren() == 2) {
        subref_node = (LangUnitNode) node.jjtGetChild(1);
        Debug.assertion(subref_node.isNodeId(JJTSUBREFERENCE), "Child Node should be ref or sub ref node, but");

        int i = 0;
        for (; i < CompilerLoader.MAX_SUB_REF_LEVEL; i++) {
          if (subref_node.jjtGetNumChildren() != 2) {
            Debug.assertion(subref_node.jjtGetNumChildren() == 1, "subref_node child num should be 1");

            return subref_node;
          }

          subref_node = (LangUnitNode) subref_node.jjtGetChild(0);
          Debug.assertion(subref_node.isNodeId(JJTSUBREFERENCE),
              "Child Node should be sub ref node, but(" + subref_node + ")");
        }

        throw new CompileException(
            "Maximum Sub Referencing Level(" + CompilerLoader.MAX_SUB_REF_LEVEL + " is excceeded");
      } else { // it does not have sub reference
        return null;
      }
    } else if (node.isNodeId(JJTSUBREFERENCE)) {
      subref_node = (LangUnitNode) node.jjtGetParent();
      Debug.assertion(subref_node != null, "Parent Node should not be null");

      if (subref_node.isNodeId(JJTSUBREFERENCE)) {
        return subref_node;
      } else {
        Debug.assertion(subref_node.isNodeId(JJTREFERENCE), "subref_node should be reference");
        return null;
      }
    } else {
      throw new CompileException("Parameter node should be Ref or Sub Ref Node, but(" + node.getNodeId() + ")");
    }
  }

  protected boolean isNextSubRefChildNodeid(LangUnitNode node, int node_id) throws CompileException {
    Debug.assertion(node.isNodeId(JJTREFERENCE) || node.isNodeId(JJTSUBREFERENCE),
        "node should be reference or sub reference, but(" + node + ")");

    LangUnitNode subref_node = getNextChildSubRefeNode(node);

    if (subref_node == null) {
      return false;
    }

    LangUnitNode subaccess_node = null;

    switch (subref_node.jjtGetNumChildren()) {
    case 1:
      subaccess_node = subref_node.getChildren(0);
      break;
    case 2:
      subaccess_node = subref_node.getChildren(1);
      break;
    default:
      throw new CompileException("Invalid Sub Ref Node Child Num(" + subref_node.jjtGetNumChildren() + ")");
    }
    Debug.assertion(subaccess_node != null, "Sub Ref Child Node should not be invalid");

    // sub ref child can be either subaccess or sub'map'access
    return subaccess_node.isNodeId(node_id);
  }

  protected LangUnitNode findRightMostAccessNode(LangUnitNode ref_node) throws CompileException {
    Debug.assertion(ref_node.isNodeId(JJTREFERENCE), "ref_node should be reference node");

    if (ref_node.jjtGetNumChildren() == 1) { // it does not have sub reference
      /*
       * Reference / Access (Right Most Access Node)
       */
      // reference node does not have sub reference
      LangUnitNode access_node = ref_node.getChildren(0);
      Debug.assertion(access_node.isNodeId(JJTACCESS), "first child should be access");
      Debug.assertion(access_node.jjtGetNumChildren() > 0,
          "access_node does not have child(" + access_node.jjtGetNumChildren() + ")");

      LangUnitNode access_child_node = access_node.getChildren(0);

      if (access_child_node.isNodeId(JJTREFERENCE)) {
        return findRightMostAccessNode(access_child_node);
      } else {
        return access_node;
      }
    } else if (ref_node.jjtGetNumChildren() == 2) { // it has sub reference
      // reference node has sub reference
      LangUnitNode sub_ref_node = ref_node.getChildren(1);
      Debug.assertion(sub_ref_node.isNodeId(JJTSUBREFERENCE), "second child should be sub ref member");

      if (sub_ref_node.jjtGetNumChildren() == 1) { // sub reference does not have
                                                 // its own sub reference
        /*
         * Reference / \ Access SubReference / Sub(Map)Access (Right Most Access
         * Node)
         */
        LangUnitNode sub_access_node = sub_ref_node.getChildren(0);

        Debug.assertion(sub_access_node.isNodeId(JJTSUBACCESS) || sub_access_node.isNodeId(JJTMAPACCESS),
            "first child should be sub(map) access");

        return sub_access_node;
      } else if (sub_ref_node.jjtGetNumChildren() == 2) { // sub reference has its
                                                        // own sub reference
        /*
         * Reference / \ Access SubReference / \ SubReference Sub(Map)Access
         * (Right Most Access Node) / Sub(Map)Access
         */
        LangUnitNode sub_access_node = sub_ref_node.getChildren(1);
        Debug.assertion(sub_access_node.isNodeId(JJTSUBACCESS) || sub_access_node.isNodeId(JJTMAPACCESS),
            "first child should be sub(map) access");

        return sub_access_node;
      } else {
        throw new CompileException("Invalid Number of child(" + sub_ref_node.jjtGetNumChildren() + ")");
      }

    } else {
      throw new CompileException("Invalid Number of child(" + ref_node.jjtGetNumChildren() + ")");
    }

  }

  protected LangUnitNode getPrevRefAccessNode(LangUnitNode subref_node) throws CompileException {
    Debug.assertion(subref_node.isNodeId(JJTSUBREFERENCE), "ref_node should be reference node");

    int num_child = subref_node.jjtGetNumChildren();

    LangUnitNode prevref_node = null;
    LangUnitNode prevrefaccess_node = null;

    if (num_child == 2) {
      prevref_node = subref_node.getChildren(0);
      Debug.assertion(prevref_node != null, "prevref_node should be valid");
      Debug.assertion(prevref_node.isNodeId(JJTSUBREFERENCE), "prevref_node should be sub reference node");

      if (prevref_node.jjtGetNumChildren() == 1) {
        prevrefaccess_node = prevref_node.getChildren(0);
      } else if (prevref_node.jjtGetNumChildren() == 2) {
        prevrefaccess_node = prevref_node.getChildren(1);
      } else {
        throw new CompileException("invalid prevref_node childnum " + prevref_node.jjtGetNumChildren());
      }

      Debug.assertion(prevrefaccess_node != null, "prevrefaccess_node should be valid");
      Debug.assertion(prevrefaccess_node.isNodeId(JJTSUBACCESS) || prevrefaccess_node.isNodeId(JJTMAPACCESS),
          "prevrefaccess_node should be sub/map access");
      return prevrefaccess_node;
    } else if (num_child == 1) {
      prevref_node = (LangUnitNode) subref_node.jjtGetParent();

      for (int i = 0; i < CompilerLoader.MAX_SUB_REF_LEVEL; i++) {
        Debug.assertion(prevref_node != null, "prevref_node should be valid");
        if (prevref_node.isNodeId(JJTREFERENCE)) {
          prevrefaccess_node = prevref_node.getChildren(0);
          Debug.assertion(prevrefaccess_node != null, "prevrefaccess_node should be valid");
          Debug.assertion(prevrefaccess_node.isNodeId(JJTACCESS), "prevrefaccess_node should be access");
          return prevrefaccess_node;
        }

        Debug.assertion(prevref_node.isNodeId(JJTSUBREFERENCE) || prevref_node.isNodeId(JJTMAPACCESS),
            "prevref_node should be sub/map reference node");
        prevref_node = (LangUnitNode) prevref_node.jjtGetParent();
      }

      throw new CompileException("max sub ref level exceeded");
    } else {
      throw new CompileException("invalid child num(" + num_child + ")");
    }

  }

  private boolean activate = true;

  public void setActivate(boolean activate) {
    this.activate = activate;
  }

  public void check_pause() throws CompileException {
    while (!activate) {
      try {
        Util.delay(10);
      } catch (Exception e) {
        // e.printStackTrace();
        throw new CompileException("received " + e.getMessage() + " in pause");
      }
    }
  }

  public void traverse(LangUnitNode start_node) throws CompileException {
    TraverseStackNode startTraverse = TraverseStackNode.allocNode(start_node);

    pushTraverse(startTraverse);

    init_traverse_ast();

    traverse_ast();

    // check_reduction_stack_empty(); // commented... traverse() can be used for
    // internal nodes
  }

  public void check_reduction_stack_empty() throws CompileException {

    int depth_reduction = depthReduction();
    if (depth_reduction != 0) {
      dump_reduction_stack();
      throw new CompileException("Reduction Stack is not empty(depth: " + depth_reduction + ") in " + this.getClass());
    }

  }

  public void init_traverse_ast() throws CompileException {
    if ((run_cnt++) != 0) {
      throw new CompileException("visitor.run() should be called only one time");
    }

    retContainer = null;

    // ready for traverse method'entry_point' with arguments (cont_args)
    // base_container.call_method(this, null, entry_point, cont_args);

    currTraverse = popTraverse();

    Debug.assertion(currTraverse != null, "Invalid initial traverse");
    Debug.assertion(currTraverse.getNode() != null, "Invalid initial traverse node");
    // check_condition(currTraverse.getNode().isNodeId(JJTFUNCTION), "Invalid
    // initial traverse node id("+currTraverse.getNode().getNodeName()+")");

    nextTraverse = null;
  }

  // this method should be overrided
  protected boolean pass_traverse(TraverseStackNode currTraverse) throws CompileException {
    // all node traverse
    return false;
  }

  // in-order tree traversal
  // 1. visit child nodes from left to right
  // 2. if the all child nodes are visited, execute node it-self
  public Container traverse_ast() throws CompileException {
    LangUnitNode run_node = null;

    run_cnt++;

    for (;;) {
      // check_pause();

      // check_interrupt();

      if (pass_traverse(currTraverse)) {
        currTraverse = popTraverse();
        continue;
      }

      // scheduling next child node
      nextTraverse = currTraverse.getNextChild();

      if (nextTraverse != null) { // it has child, push current
        pushTraverse(currTraverse);

        currTraverse = nextTraverse;

      } else { // it does not have child, execute itself & pop
        if (traverse_stack.size() == 0) {
          break;
        }

        // in-order tree traversal
        run_node = currTraverse.getNode();

        try {

          before_visit(run_node);
          run_node.node_run(this, null);
          after_visit(run_node);

        } catch (CompileException e) {
          if (!e.node_initialized()) {
            e.set_node(run_node);
          }
          throw e;
        } catch (Exception e) {
          e.printStackTrace();
          CompileException excp = new CompileException("tgt exception", run_node);
          excp.setTargetException(e);
          throw excp;
        }

        currTraverse = popTraverse();

      }
    }

    // in-order tree traversal
    // currTraverse.getNode().node_run(this, null);
    run_node = currTraverse.getNode();
    run_node.node_run(this, null);

    return retContainer;
  }

  /*
   * This language does not permit variable declaration in branch body
   * expression Variable declaration should not depend on branch. It make
   * confusion in variable scope.
   *
   * ex) if( a ) b:int = 10 <-- variable declaration is not permitted else c:int
   * = 10 <-- variable declaration is not permitted
   *
   * But, it permits. ex) if( a ) { b:int = 10 <-- variable scope is clear }
   * else { c:int = 10 <-- variable scope is clear }
   */

  protected boolean isValidASTConextForStackVarRegister(LangUnitNode node) throws CompileException {
    LangUnitNode ifexpr_node = node.getClosestParent(JJTIFEXPR);

    if (ifexpr_node != null) {
      LangUnitNode stream_node = node.getClosestParentUntil(JJTSTREAM, ifexpr_node);

      if (stream_node == null) {
        return false;
      }
    }

    LangUnitNode matchexpr_node = node.getClosestParent(JJTMATCHEXPR);

    if (matchexpr_node != null) {
      LangUnitNode stream_node = node.getClosestParentUntil(JJTSTREAM, matchexpr_node);

      if (stream_node == null) {
        return false;
      }
    }

    return true;
  }

  protected boolean is_lval_refnode(int parent_num_child, int refnode_childidx) {
    if (parent_num_child == 3 && refnode_childidx == 1) {
      return true;
    } else if ((parent_num_child == 2 || parent_num_child == 1) && refnode_childidx == 0) {
      return true;
    }

    return false;
  }

  public void before_visit(LangUnitNode node) throws CompileException {
    // do nothing
  }

  public void after_visit(LangUnitNode node) throws CompileException {
    // do nothing
  }

  @Override
  public Object visit(SimpleNode node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTTranslationUnit node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTTranslationUnitHeader node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTTranslationUnitElement node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTIfCaseExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTIfCondExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTIfExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTElseExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMatchExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMatchHeadExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMatchCaseExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMatchCaseHeadExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMatchCaseBodyExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTLoopHdrExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTLoopExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTJumpExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTAssignmentOperator node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTUnaryOperator node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTConstant node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTSymbolName node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTParameterList node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTParameter node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTConditional node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTForInit node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTForCondition node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTForAction node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTForBody node, Object data) throws CompileException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object visit(ASTCstyleLoopExpr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTStreamBlockElementHdr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTStreamBlockElement node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTStreamBlockHdr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTStreamBlock node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTAssignment node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTTypeDefine node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTType node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTApplyType node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTNamedType node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTStreamType node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMapType node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTStreamHeader node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTStreamSignature node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTReduceType node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTStream node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTCatchExceptionList node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTCatch node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMetaData node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMetaExtendsData node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMetaImplementsData node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTAccessCtrl node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTImport node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTLogicalOR node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTLogicalORHdr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTLogicalAND node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTLogicalANDHdr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTInclusiveOR node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTExclusiveOR node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTAND node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTEquality node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTRelational node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTShift node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTAdditive node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMultiplicative node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTUnary node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTPostfix node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTReference node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTSubReference node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTAccess node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTSubAccess node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTArgumentHdr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTArgument node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTArgumentList node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMapAccess node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTFunctionDefHdr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTFunctionDefSignature node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTFuncDefMetaData node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTMetaThrowsData node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTFunctionDef node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTOneExprFuncBody node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTExplicitCast node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTFunctionType node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTParameterTypeList node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTInvoke node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTInvokeHdr node, Object data) throws CompileException {
    return null;
  }

  @Override
  public Object visit(ASTImportClass node, Object data) throws CompileException {
    return null;
  }

}
