package mezic.compiler;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mezic.parser.ASTAND;
import mezic.parser.ASTAdditive;
import mezic.parser.ASTArgument;
import mezic.parser.ASTArgumentHdr;
import mezic.parser.ASTCstyleLoopExpr;
import mezic.parser.ASTEquality;
import mezic.parser.ASTExclusiveOR;
import mezic.parser.ASTFunctionDefHdr;
import mezic.parser.ASTInclusiveOR;
import mezic.parser.ASTLogicalAND;
import mezic.parser.ASTLogicalANDHdr;
import mezic.parser.ASTLogicalOR;
import mezic.parser.ASTLogicalORHdr;
import mezic.parser.ASTLoopExpr;
import mezic.parser.ASTLoopHdrExpr;
import mezic.parser.ASTMultiplicative;
import mezic.parser.ASTReference;
import mezic.parser.ASTRelational;
import mezic.parser.ASTShift;
import mezic.parser.ASTStream;
import mezic.parser.ASTStreamBlockElement;
import mezic.parser.ASTStreamBlockElementHdr;
import mezic.parser.ASTStreamHeader;
import mezic.parser.ASTStreamSignature;
import mezic.parser.LangUnitNode;

/*
 * AST Pre-Processing Actions
 * 1. right_child_rotate for dual operation nodes( Additive, Mulit- ... )
 * 2. rotate_right_path_to_left for Sub Reference nodes
 * 3. Move Switch Case Nodes to the child position of Switch Node
 * 4. Swap 'for action' node and 'statement list' in for iteration
 * 5. Add dummy StreamHeader for stream type stream
 */

public class ASTpreprocessVisitor extends ASTTraverseVisitor {

  private static final long serialVersionUID = -615175637245623703L;

  private static final Logger LOG = LoggerFactory.getLogger(ASTpreprocessVisitor.class);

  private String prefix = "";
  private int seq = 0;
  private int depth = 0;

  public void visit_log(LangUnitNode node) {
    LOG.info("{} {}({}){}", seq, prefix, depth, node.toString());
  }

  // pre-order(depth first) tree traversal
  // 1. execute node it-self before visiting child nodes
  // 2. visit child nodes from left to right
  public PpVisitReturn recurs_pre_order_tree_traverse(LangUnitNode node, int child_idx) throws CompileException {
    PpVisitReturn ret;

    if (node == null) {
      throw new CompileException("Invalid node(node is null)");
    }

    ret = (PpVisitReturn) node.jjtAccept(this, new Integer(child_idx));

    if (ret == null) {
        //do nothing
      } else {
      switch (ret.getType()) {
      case PpVisitReturn.TRAVERSE_NEXT:
        break;
      case PpVisitReturn.RETRAVERSE_NODE:
      case PpVisitReturn.BREAK_TRAVERSE:
        return ret;
      default:
        throw new CompileException("Invalid PpVisitReturn type(" + ret.getType() + ")");
      }
    }

    seq++;
    depth++;
    prefix += "*";

    int num_child = node.jjtGetNumChildren();
    LangUnitNode child = null;

    for (int i = 0; i < num_child;) {
      child = (LangUnitNode) node.getChildren(i);

      if (child == null) {
        throw new CompileException("Invalid child node(child node " + i + " is null)");
      }

      ret = recurs_pre_order_tree_traverse(child, i);

      if (ret == null) {
        i++; // traverse next child
      } else {
        switch (ret.getType()) {
        case PpVisitReturn.RETRAVERSE_NODE:
          /* it does not increase child index */
          break;
        case PpVisitReturn.BREAK_TRAVERSE:
          /* breaking traverse */
          return ret;
        default:
          i++; // traverse next child
        }
      }
    }

    depth--;
    prefix = prefix.substring(0, prefix.length() - 1);

    return ret;
  }

  @Override
  public Object visit(ASTStream node, Object data) throws CompileException {

    LOG.debug("{} Number of Stream Child = {}", node.toString(), node.jjtGetNumChildren());

    int sigidx = node.getChildIdxWithId(JJTSTREAMSIGNATURE, 0);

    if (sigidx == -1) {
      // Stream does not have Stream Signature
      // add dummy signature & stream_head
      LangUnitNode stream_child = node.getChildren(0);

      Debug.assertion(stream_child != null, "Invalid child node");
      Debug.assertion(stream_child.isNodeId(JJTSTREAMBLOCK), "Invalid child node");

      ASTStreamSignature sigNode = new ASTStreamSignature(JJTSTREAMSIGNATURE);
      sigNode.jjtSetFirstToken(node.jjtGetFirstToken());

      ASTStreamHeader stream_head_Node = new ASTStreamHeader(JJTSTREAMHEADER);
      stream_head_Node.jjtSetFirstToken(node.jjtGetFirstToken());
      stream_head_Node.initStreamForm(LangUnitNode.STREAMFORM_DFLT);

      sigNode.jjtAddChild(stream_head_Node, 0);
      stream_head_Node.jjtSetParent(sigNode);

      node.jjtPushChild(sigNode, 0);
      sigNode.jjtSetParent(node);
    }

    return null;
  }

  @Override
  public Object visit(ASTStreamHeader node, Object data) throws CompileException {

    LangUnitNode ref_node = node.getClosestParent(JJTREFERENCE);
    Debug.assertion(ref_node != null, "Stream should have reference node");

    if (node.getStreamForm() == LangUnitNode.STREAMFORM_CLASS) {
      ref_node.setIsDefinition(true);
    }

    return null;
  }

  @Override
  public Object visit(ASTFunctionDefHdr node, Object data) throws CompileException {

    LangUnitNode ref_node = node.getClosestParent(JJTREFERENCE);
    Debug.assertion(ref_node != null, "Stream should have reference node");

    switch (node.getFuncForm()) {
    case LangUnitNode.FUNCFORM_MEMBER:
    case LangUnitNode.FUNCFORM_STATIC:
      ref_node.setIsDefinition(true);
      break;
    default:
      // do nothing
    }

    return null;
  }

  @Override
  public Object visit(ASTArgument node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 1, "Argument Child should be 1");

    LangUnitNode argchild_node = node.getChildren(0);
    Debug.assertion(argchild_node != null, "argchild_node should be valid");

    ASTArgumentHdr arghdr_node = new ASTArgumentHdr(JJTARGUMENTHDR);
    arghdr_node.jjtSetFirstToken(node.jjtGetFirstToken());

    node.jjtAddChild(argchild_node, 1);
    node.jjtSetChild(0, arghdr_node);
    arghdr_node.jjtSetParent(node);

    return null;
  }

  @Override
  public Object visit(ASTStreamBlockElement node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 1, "Stream element should have one child");

    LangUnitNode child_node = node.getChildren(0);
    Debug.assertion(child_node != null, "child_node should be valid");

    ASTStreamBlockElementHdr elehdr_node = new ASTStreamBlockElementHdr(JJTSTREAMBLOCKELEMENTHDR);
    elehdr_node.jjtSetFirstToken(node.jjtGetFirstToken());

    node.jjtAddChild(child_node, 1);
    node.jjtSetChild(0, elehdr_node);
    elehdr_node.jjtSetParent(node);

    return null;
  }

  @Override
  public Object visit(ASTLogicalOR node, Object data) throws CompileException {
    // visit_log(node);

    if (right_child_rotate(node, data)) {
      return new PpVisitReturn(PpVisitReturn.RETRAVERSE_NODE);
    }

    // Add Logical or hdr node
    Debug.assertion(node.jjtGetNumChildren() == 2, "node child should be 2");

    LangUnitNode lchild = node.getChildren(0);
    LangUnitNode rchild = node.getChildren(1);

    node.jjtAddChild(rchild, 2);
    node.jjtSetChild(1, lchild);

    ASTLogicalORHdr log_orhdr_node = new ASTLogicalORHdr(JJTLOGICALORHDR);
    log_orhdr_node.jjtSetFirstToken(node.jjtGetFirstToken());

    node.jjtSetChild(0, log_orhdr_node);
    log_orhdr_node.jjtSetParent(node);

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

  @Override
  public Object visit(ASTLogicalAND node, Object data) throws CompileException {
    // visit_log(node);

    if (right_child_rotate(node, data)) {
      return new PpVisitReturn(PpVisitReturn.RETRAVERSE_NODE);
    }

    // Add Logical and hdr node
    Debug.assertion(node.jjtGetNumChildren() == 2, "node child should be 2");

    LangUnitNode lchild = node.getChildren(0);
    LangUnitNode rchild = node.getChildren(1);

    node.jjtAddChild(rchild, 2);
    node.jjtSetChild(1, lchild);

    ASTLogicalANDHdr log_andhdr_node = new ASTLogicalANDHdr(JJTLOGICALANDHDR);
    log_andhdr_node.jjtSetFirstToken(node.jjtGetFirstToken());

    node.jjtSetChild(0, log_andhdr_node);
    log_andhdr_node.jjtSetParent(node);

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

  @Override
  public Object visit(ASTInclusiveOR node, Object data) throws CompileException {
    // visit_log(node);

    if (right_child_rotate(node, data)) {
      return new PpVisitReturn(PpVisitReturn.RETRAVERSE_NODE);
    }

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

  @Override
  public Object visit(ASTExclusiveOR node, Object data) throws CompileException {
    // visit_log(node);

    if (right_child_rotate(node, data)) {
      return new PpVisitReturn(PpVisitReturn.RETRAVERSE_NODE);
    }

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

  @Override
  public Object visit(ASTAND node, Object data) throws CompileException {
    // visit_log(node);

    if (right_child_rotate(node, data)) {
      return new PpVisitReturn(PpVisitReturn.RETRAVERSE_NODE);
    }

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

  @Override
  public Object visit(ASTEquality node, Object data) throws CompileException {
    // visit_log(node);

    if (right_child_rotate(node, data)) {
      return new PpVisitReturn(PpVisitReturn.RETRAVERSE_NODE);
    }

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

  @Override
  public Object visit(ASTRelational node, Object data) throws CompileException {
    // visit_log(node);

    if (right_child_rotate(node, data)) {
      return new PpVisitReturn(PpVisitReturn.RETRAVERSE_NODE);
    }

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

  @Override
  public Object visit(ASTShift node, Object data) throws CompileException {
    // visit_log(node);

    if (right_child_rotate(node, data)) {
      return new PpVisitReturn(PpVisitReturn.RETRAVERSE_NODE);
    }

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

  @Override
  public Object visit(ASTAdditive node, Object data) throws CompileException {
    // visit_log(node);

    if (right_child_rotate(node, data)) {
      return new PpVisitReturn(PpVisitReturn.RETRAVERSE_NODE);
    }

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

  @Override
  public Object visit(ASTMultiplicative node, Object data) throws CompileException {
    // visit_log(node);

    if (right_child_rotate(node, data)) {
      return new PpVisitReturn(PpVisitReturn.RETRAVERSE_NODE);
    }

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

  @Override
  public Object visit(ASTReference node, Object data) throws CompileException {

    if (is_meaningless_primary_exp(node)) {
      // System.out.println(node.jjtGetParent());
      // node.dump_tree(".", 0, 0);
      // Debug.stop();

      remove_meaningless_primary_exp(node);
      return new PpVisitReturn(PpVisitReturn.RETRAVERSE_NODE);
    }

    // visit_log(node); // not-implemented
    LangUnitNode right_child;
    LangUnitNode new_right_child;

    // Sub References Rotation
    if (node.jjtGetNumChildren() == 2) {
      right_child = node.getChildren(1);

      if (right_child.isNodeId(JJTSUBREFERENCE)) {
        new_right_child = rotate_same_type_right_path(right_child);

        node.jjtSetChild(1, new_right_child);
      }
    }

    // Update Definition Node
    if (isReferencedRefNode(node)) {
      node.setIsDefinition(false);
    } else {
      node.setIsDefinition(true);
    }

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

  private boolean is_meaningless_primary_exp(LangUnitNode ref_node) throws CompileException {
    Debug.assertion(ref_node.isNodeId(JJTREFERENCE), "node should be reference node");

    LangUnitNode parent_node = (LangUnitNode) ref_node.jjtGetParent();
    Debug.assertion(parent_node != null, "parent_ndoe should be valid");

    switch (parent_node.getNodeId()) {
    case JJTTRANSLATIONUNITELEMENT:
    case JJTSTREAMBLOCKELEMENT:
    case JJTFORBODY:
      return false;
    default:
      // do nothing
    }

    Debug.assertion(ref_node.jjtGetNumChildren() > 0, "ref node should have child node");

    if (ref_node.jjtGetNumChildren() > 1) {
      return false;
    }

    LangUnitNode access_node = ref_node.getChildren(0);
    Debug.assertion(access_node != null, "access_node should be valid");

    if (!access_node.isNodeId(JJTACCESS)) {
      return false;
    }

    if (access_node.jjtGetNumChildren() != 1) {
      return false;
    }

    LangUnitNode accesschild_node = access_node.getChildren(0);
    Debug.assertion(accesschild_node != null, "accesschild_node should be valid");

    if (parent_node.isNodeId(JJTSUBACCESS) && accesschild_node.isNodeId(JJTFUNCTIONDEF)) { // this
                                                                                           // is
                                                                                           // for
                                                                                           // member
                                                                                           // closure
                                                                                           // case
                                                                                           // ...
                                                                                           // 'a'
                                                                                           // is
                                                                                           // some
                                                                                           // object,
                                                                                           // and
                                                                                           // do
                                                                                           // ..'a.(
                                                                                           // fn()->int
                                                                                           // {
                                                                                           // return
                                                                                           // 10
                                                                                           // }
                                                                                           // )()'
      return true;
    }

    switch (accesschild_node.getNodeId()) {
    case JJTSYMBOLNAME:
    case JJTCONSTANT:
    case JJTMATCHEXPR:
    case JJTLOOPEXPR:
    case JJTIFEXPR:
    case JJTSTREAM:
    case JJTEXPLICITCAST:
    case JJTFUNCTIONDEF:
      // for testing --> these cases are meaningless primary expression...
      // case JJTREFERENCE:
      // case JJTADDITIVE:
      // case JJTPOSTFIX:
      return false;
    default:
      return true;
    }
  }

  private void remove_meaningless_primary_exp(LangUnitNode ref_node) throws CompileException {
    Debug.assertion(ref_node.isNodeId(JJTREFERENCE), "node should be reference node");
    Debug.assertion(ref_node.jjtGetNumChildren() == 1, "ref node should have 1 child node");

    LangUnitNode parent_node = (LangUnitNode) ref_node.jjtGetParent();
    Debug.assertion(parent_node != null, "parent_ndoe should be valid");

    int idx = parent_node.getChildIdx(ref_node);
    Debug.assertion(idx != -1, "idx should not be -1");

    LangUnitNode access_node = ref_node.getChildren(0);
    Debug.assertion(access_node != null, "access_node should be valid");
    Debug.assertion(access_node.isNodeId(JJTACCESS), "access_node should be access");
    Debug.assertion(access_node.jjtGetNumChildren() == 1, "access_node should be 1 child");

    LangUnitNode accesschild_node = access_node.getChildren(0);
    Debug.assertion(accesschild_node != null, "accesschild_node should be valid");

    // remove meansingless primary expression
    parent_node.jjtSetChild(idx, accesschild_node);
    accesschild_node.jjtSetParent(parent_node);

  }

  /*
   * (Right Child Rotate) - change operation priority
   *
   * Example: a + ( b - c ) + d
   *
   * Additive_1(+) Additive_3(+) / \ / \ a Additive_3(+) --> Additive_1(+) d / \
   * / \ Unary d a Unary | | Additive_2(-) Additive_2(-) / \ / \ b c b c
   *
   */
  private boolean right_child_rotate(LangUnitNode node, Object data) throws CompileException {
    if (node.isRotated()) {
      // do nothing, this node is already rotated
      return false;
    }

    check_numof_childnode(2, node);

    Debug.assertion(data != null, "data should not be null");
    Debug.assertion(data instanceof Integer, "data should be Integer");
    int node_child_idx = ((Integer) data).intValue();

    LangUnitNode left_child = node.getChildren(0);
    Debug.assertion(left_child != null, "left child should not be null");

    LangUnitNode right_child = node.getChildren(1);
    Debug.assertion(right_child != null, "right child should not be null");

    // if right child is same dual op node, it needs to rotate tree
    if (right_child.isNodeId(node.getNodeId())) {
      check_numof_childnode(2, right_child);
      LangUnitNode left_child_of_right = right_child.getChildren(0);

      Debug.assertion(left_child_of_right != null, "left child of right should not be null");

      // - right child should be parent of node
      // - node should be left child of right child
      // - left child of right child should be right child of node
      LangUnitNode parent = (LangUnitNode) node.jjtGetParent();

      // parent.dump_tree("", 0);
      parent.jjtSetChild(node_child_idx, right_child);
      right_child.jjtSetParent(parent);

      right_child.jjtSetChild(0, node);
      node.jjtSetParent(right_child);

      node.jjtSetChild(1, left_child_of_right);
      left_child_of_right.jjtSetParent(node);

      node.rotated();

      // LOG.debug("Node is rotated");
      // parent.dump_tree("", 0);
      return true;
    } else {
      return false;
    }

  }

  class PpVisitReturn {
    final static int TRAVERSE_NEXT = 0;
    final static int RETRAVERSE_NODE = 1;
    final static int BREAK_TRAVERSE = 2;

    private int visitRetType;

    public PpVisitReturn(int type) {
      this.visitRetType = type;
    }

    public int getType() {
      return visitRetType;
    }

  }

  private LangUnitNode rotate_same_type_right_path(LangUnitNode node) throws CompileException {
    LinkedList<LangUnitNode> list = new LinkedList<LangUnitNode>();

    LangUnitNode current = node;
    LangUnitNode right_child = null;

    list.add(current);

    for (;;) {
      // LOG.debug("Rotating: "+current);

      if (current.jjtGetNumChildren() != 2) {
        break;
      }

      right_child = current.getChildren(1);

      Debug.assertion(right_child != null, "right child should not be null");

      if (!right_child.isNodeId(node.getNodeId())) {
        // if current has two child, and right child is not same type,
        // it can not use the function.- that means invalid AST
        throw new CompileException("Invalid right child(" + right_child + ") of node(" + current + ")");
      }

      list.add(right_child);

      current = right_child;

    }

    if (list.size() < 2) {
      // LOG.debug("Rotating: Size is under 2");
      return node;
    }

    LangUnitNode[] path = new LangUnitNode[list.size()];

    for (int i = 0; i < path.length; i++) {
      path[i] = list.get(i);
    }

    LangUnitNode new_head = rotate_right_path_to_left(path);

    // LOG.debug("Node right path is inverted");

    return new_head;
  }

  /*
   * (Rotate Right Path to Left Path)
   *
   * Example: A[a][b][c]
   *
   * SubRef(0) SubRef(2) / \ / \ a SubRef(1) --> SubRef(1) c / \ / \ b SubRef(2)
   * SubRef(0) b / / c a
   */

  private LangUnitNode rotate_right_path_to_left(LangUnitNode[] path) throws CompileException {
    if (path.length < 2) {
      throw new CompileException("path.lenght should be over 2(" + path.length + ")");
    }

    LangUnitNode base_parent = (LangUnitNode) path[0].jjtGetParent();

    path[0].jjtDelChild(1); // remove right child reference

    for (int i = 1; i < (path.length - 1); i++) {
      Debug.assertion(path[i] != null, "path[" + i + "] should not be null");
      Debug.assertion(path[i].jjtGetNumChildren() == 2, "path[" + i + "] should not be null");

      path[i - 1].jjtSetParent(path[i]); // node becomes parent of right child

      path[i].jjtSetChild(1, path[i].jjtGetChild(0)); // left child is moved to
                                                      // right child

      path[i].jjtSetChild(0, path[i - 1]); // parent becomes left child
    }

    path[path.length - 2].jjtSetParent(path[path.length - 1]);

    // path end child should have only one child(should not have right child)
    check_numof_childnode(1, path[path.length - 1]);

    path[path.length - 1].jjtAddChild(path[path.length - 1].jjtGetChild(0), 1); // left
                                                                                // child
                                                                                // is
                                                                                // moved
                                                                                // to
                                                                                // right
                                                                                // child

    path[path.length - 1].jjtSetChild(0, path[path.length - 2]);

    path[path.length - 1].jjtSetParent(base_parent);

    return path[path.length - 1];
  }

  // adding LoopHdr
  /*
   * LoopExpr LoopExpr | ---> / \ CstyleLoopExpression LoopHdrExpr(Add New)
   * CstyleLoopExpression | ListLoopExpression | ListLoopExpression
   *
   */
  @Override
  public Object visit(ASTLoopExpr node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 1, "Loop Expr child should be 1, but" + node.jjtGetNumChildren());

    LangUnitNode loop_child_node = (LangUnitNode) node.jjtGetChild(0);
    Debug.assertion(loop_child_node != null, "loop_child_node should be valid");
    Debug.assertion(loop_child_node.isNodeId(JJTCSTYLELOOPEXPR), "loop_child_node should be cstype or listloop");

    ASTLoopHdrExpr hdr_node = new ASTLoopHdrExpr(JJTLOOPHDREXPR);
    hdr_node.jjtSetFirstToken(node.jjtGetFirstToken());

    node.jjtAddChild(loop_child_node, 1);
    node.jjtAddChild(hdr_node, 0);
    hdr_node.jjtSetParent(node);

    return null;
  }

  // C Style loop sequence change

  /*
   * pre-processor changed this loop structure to as follows
   *
   * CstyleLoopExpression CstyleLoopExpression | | +-------+----------+--------+
   * ---> +-------+----------+--------+ / | | \ / | | \ / | | \ / | | \ ForInit
   * ForCondition ForAction ForBody ForInit ForBody ForAction ForCondition
   */

  @Override
  public Object visit(ASTCstyleLoopExpr node, Object data) throws CompileException {

    int num_child = node.jjtGetNumChildren();
    Debug.assertion(num_child == 4, "CStyle Loop Child should be 4(" + num_child + ")");

    LangUnitNode for_cond_node = node.getChildren(1);
    Debug.assertion(for_cond_node != null, "for_cond_node should not be invalid");
    Debug.assertion(for_cond_node.isNodeId(JJTFORCONDITION), "parent child 1  should be For Condition");

    LangUnitNode for_action_node = node.getChildren(2);
    Debug.assertion(for_action_node != null, "for_action_node should not be invalid");
    Debug.assertion(for_action_node.isNodeId(JJTFORACTION), "parent child 2  should be For Action");

    LangUnitNode for_body_node = node.getChildren(3);
    Debug.assertion(for_body_node != null, "for_body_node should not be invalid");
    Debug.assertion(for_body_node.isNodeId(JJTFORBODY), "parent child 3  should be For Body");

    node.jjtSetChild(1, for_body_node);
    node.jjtSetChild(2, for_action_node);
    node.jjtSetChild(3, for_cond_node);

    return new PpVisitReturn(PpVisitReturn.TRAVERSE_NEXT);
  }

}
