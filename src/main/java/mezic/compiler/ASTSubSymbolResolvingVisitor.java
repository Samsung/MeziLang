package mezic.compiler;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mezic.compiler.type.AbsClassType;
import mezic.compiler.type.AbsFuncType;
import mezic.compiler.type.AbsType;
import mezic.compiler.type.AbsTypeList;
import mezic.compiler.type.FuncSignatureDesc;
import mezic.compiler.type.Operation;
import mezic.compiler.type.TContext;
import mezic.compiler.type.TContextClass;
import mezic.compiler.type.TContextClosure;
import mezic.compiler.type.TContextFunc;
import mezic.compiler.type.TContextStream;
import mezic.compiler.type.TContextTU;
import mezic.compiler.type.TMapType;
import mezic.compiler.type.TMethodHandle;
import mezic.compiler.type.TPrimitiveClass;
import mezic.compiler.type.TraverseStackNode;
import mezic.parser.ASTAND;
import mezic.parser.ASTAccess;
import mezic.parser.ASTAdditive;
import mezic.parser.ASTApplyType;
import mezic.parser.ASTArgumentList;
import mezic.parser.ASTAssignment;
import mezic.parser.ASTCatch;
import mezic.parser.ASTCatchExceptionList;
import mezic.parser.ASTConstant;
import mezic.parser.ASTElseExpr;
import mezic.parser.ASTEquality;
import mezic.parser.ASTExclusiveOR;
import mezic.parser.ASTExplicitCast;
import mezic.parser.ASTForAction;
import mezic.parser.ASTForBody;
import mezic.parser.ASTForCondition;
import mezic.parser.ASTForInit;
import mezic.parser.ASTFunctionDef;
import mezic.parser.ASTFunctionDefHdr;
import mezic.parser.ASTIfCaseExpr;
import mezic.parser.ASTIfCondExpr;
import mezic.parser.ASTIfExpr;
import mezic.parser.ASTInclusiveOR;
import mezic.parser.ASTInvoke;
import mezic.parser.ASTJumpExpr;
import mezic.parser.ASTLogicalAND;
import mezic.parser.ASTLogicalOR;
import mezic.parser.ASTLoopExpr;
import mezic.parser.ASTLoopHdrExpr;
import mezic.parser.ASTMapAccess;
import mezic.parser.ASTMatchCaseBodyExpr;
import mezic.parser.ASTMatchCaseHeadExpr;
import mezic.parser.ASTMatchExpr;
import mezic.parser.ASTMatchHeadExpr;
import mezic.parser.ASTMultiplicative;
import mezic.parser.ASTOneExprFuncBody;
import mezic.parser.ASTPostfix;
import mezic.parser.ASTReduceType;
import mezic.parser.ASTReference;
import mezic.parser.ASTRelational;
import mezic.parser.ASTShift;
import mezic.parser.ASTStream;
import mezic.parser.ASTStreamBlockElement;
import mezic.parser.ASTStreamBlockElementHdr;
import mezic.parser.ASTStreamHeader;
import mezic.parser.ASTSubAccess;
import mezic.parser.ASTTranslationUnit;
import mezic.parser.ASTTranslationUnitElement;
import mezic.parser.ASTTranslationUnitHeader;
import mezic.parser.ASTType;
import mezic.parser.ASTTypeDefine;
import mezic.parser.ASTUnary;
import mezic.parser.LangUnitNode;
import mezic.parser.ParserConstants;
import mezic.parser.Token;
import mezic.util.PathUtil;
import mezic.util.TypeUtil;

/*
 * AST Context Building Action
 * 1. Build Context Dependency Tree
 * 2. register variables( class member, function parameter/local variable, list variable) to the context
 * 3. set container for top reference variable name node
 */

public class ASTSubSymbolResolvingVisitor extends ASTTraverseVisitor {

  private static final long serialVersionUID = 4946017827730827713L;

  private static final Logger LOG = LoggerFactory.getLogger(ASTSubSymbolResolvingVisitor.class);

  //protected LinkedList<TContext> func_ctx_list = null;
  private LinkedList<TContext> func_ctx_list = null;

  public ASTSubSymbolResolvingVisitor(CompilerLoader loader) {
    super(loader);

    func_ctx_list = new LinkedList<TContext>();
  }

  @Override
  protected boolean pass_traverse(TraverseStackNode currTraverse) throws CompileException {
    Debug.assertion(currTraverse != null, "currTraverse should be valid");

    LangUnitNode node = currTraverse.getNode();
    Debug.assertion(node != null, "node should be valid");

    if (node.isNodeId(JJTSTREAMBLOCK) || node.isNodeId(JJTONEEXPRFUNCBODY)) {
      LangUnitNode func_node = (LangUnitNode) node.jjtGetParent();

      if (func_node.isNodeId(JJTFUNCTIONDEF)) {
        LangUnitNode func_sig_node = func_node.getChildren(0);
        Debug.assertion(func_sig_node != null, "child node should not be null");
        Debug.assertion(func_sig_node.isNodeId(JJTFUNCTIONDEFSIGNATURE), "child should be function signature");

        LangUnitNode func_head_node = func_sig_node.getChildren(0);
        Debug.assertion(func_head_node != null, "child node should not be null");
        Debug.assertion(func_head_node.isNodeId(JJTFUNCTIONDEFHDR), "grand child should be function expr type");

        TContextFunc func_ctx = (TContextFunc) func_head_node.getBindContext();

        if (func_ctx.is_apply()) {

          return true;
        }

      }
    }

    return false;
  }

  public void dependency_traverse(LangUnitNode tu_node) throws CompileException {
    depency_schedule_ctx_list(tu_node);

    traverse_func_ctx_list();
  }

  protected void depency_schedule_ctx_list(LangUnitNode tu_node) throws CompileException {
    // System.out.println("#depency_schedule_ctx_list");

    LinkedList<TContext> mid_priority_func_ctx_list = new LinkedList<TContext>();
    LinkedList<TContext> low_priority_func_ctx_list = new LinkedList<TContext>();

    Debug.assertion(tu_node.isNodeId(JJTTRANSLATIONUNIT), "tu_node should be translation unit");

    LangUnitNode tuHead = tu_node.getChildren(0);
    Debug.assertion(tuHead != null, "child node should not be null");
    Debug.assertion(tuHead.isNodeId(JJTTRANSLATIONUNITHEADER), "child should be translation unit header");

    TContext tu_context = tuHead.getBindContext();
    Debug.assertion(tu_context != null, "tu_context should be valid");

    int size = tu_context.getChildContextSize();

    TContext class_context = null;

    LangUnitNode classhead_node = null;
    LangUnitNode classsig_node = null;
    LangUnitNode class_node = null;

    FuncSignatureDesc funcdsc = null;

    for (int i = 0; i < size; i++) {
      class_context = tu_context.getChildContext(i);
      Debug.assertion(tu_context != null, "tu_context should be valid");

      classhead_node = class_context.getBindNode();
      Debug.assertion(classhead_node != null, "classhead_node should be valid");
      Debug.assertion(classhead_node.isNodeId(JJTSTREAMHEADER), "classhead_node should be stream header");

      classsig_node = (LangUnitNode) classhead_node.jjtGetParent();
      Debug.assertion(classsig_node != null, "class_node should be valid");
      Debug.assertion(classsig_node.isNodeId(JJTSTREAMSIGNATURE), "class_node should be stream");

      class_node = (LangUnitNode) classsig_node.jjtGetParent();
      Debug.assertion(class_node != null, "class_node should be valid");
      Debug.assertion(class_node.isNodeId(JJTSTREAM), "class_node should be stream");

      int num_class = class_context.getChildContextSize();
      TContext func_context = null;

      for (int j = 0; j < num_class; j++) {
        func_context = class_context.getChildContext(j);
        Debug.assertion(func_context != null, "func_context should be valid");

        funcdsc = ((TContextFunc) func_context).getFuncSignatureDesc();
        Debug.assertion(funcdsc != null, "funcdsc should be valid");

        if (((AbsFuncType) func_context).is_apply()) {
          func_ctx_list.add(func_context);
        } else if (funcdsc.getReturnType().isName(CompilerLoader.UNRESOLVED_TYPE_CLASS_NAME)) {
          mid_priority_func_ctx_list.add(func_context);
        } else {
          low_priority_func_ctx_list.add(func_context);
        }
      }
    }

    size = mid_priority_func_ctx_list.size();
    for (int i = 0; i < size; i++) {
      func_ctx_list.add(mid_priority_func_ctx_list.get(i));
    }

    size = low_priority_func_ctx_list.size();
    for (int i = 0; i < size; i++) {
      func_ctx_list.add(low_priority_func_ctx_list.get(i));
    }

  }

  protected void traverse_func_ctx_list() throws CompileException {
    int size = this.func_ctx_list.size();

    for (int i = 0; i < size; i++) {
      traverse_function(func_ctx_list.get(i));
    }
  }

  protected void traverse_function(TContext func_context) throws CompileException {
    LangUnitNode funchead_node = null;
    LangUnitNode funcsig_node = null;
    LangUnitNode func_node = null;

    funchead_node = func_context.getBindNode();

    if (funchead_node != null) {
      Debug.assertion(funchead_node.isNodeId(JJTFUNCTIONDEFHDR), "funchead_node should be function def header");

      TContext class_context = (TContext) func_context.getOwnerType();
      Debug.assertion(class_context != null, "class_context should be valid");

      TContext tu_context = (TContext) class_context.getOwnerType();
      Debug.assertion(tu_context != null, "class_context should be valid");

      pushContext(tu_context);
      pushContext(class_context);

      branch_tree.initRootBranch(new Branch(class_context.getBindNode()));

      funcsig_node = (LangUnitNode) funchead_node.jjtGetParent();
      Debug.assertion(funcsig_node != null, "funcsig_node should be valid");
      Debug.assertion(funcsig_node.isNodeId(JJTFUNCTIONDEFSIGNATURE), "funcsig_node should be function def signature");

      func_node = (LangUnitNode) funcsig_node.jjtGetParent();
      Debug.assertion(func_node != null, "func_node should be valid");
      Debug.assertion(func_node.isNodeId(JJTFUNCTIONDEF), "func_node should be function def");

      run_cnt = 0;
      traverse(func_node);

      Reduction reduce = popReduction();
      Debug.assertion(reduce != null, "popped stream  element should not be invalid");

      popContext(); // class context
      popContext(); // tu context

    }

  }

  protected void check_unreachablecode(LangUnitNode node) throws CompileException {
    Branch branch = branch_tree.getCurrBranch();

    if (branch != null && !branch.isValid()) {
      throw new CompileException("Unreachable Code", node);
    }
  }

  @Override
  public Object visit(ASTTranslationUnit node, Object data) throws CompileException {

    dump_context_stack();
    dump_reduction_stack();

    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");
    Debug.assertion(context.isForm(AbsType.FORM_TU), "Context Type should be Translation Unit");

    return null;
  }

  @Override
  public Object visit(ASTTranslationUnitHeader node, Object data) throws CompileException {

    TContext context = node.getBindContext();
    Debug.assertion(context != null, "Context should not be null");
    Debug.assertion(context.isForm(AbsType.FORM_TU), "Context Type should be Translation Unit");
    this.pushContext(context);
    dump_context_stack();

    return null;
  }

  @Override
  public Object visit(ASTTranslationUnitElement node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 1, "TU element should have one child");

    LangUnitNode child = (LangUnitNode) node.jjtGetChild(0);

    Debug.assertion(child != null, "TU element should not be invalid");

    Reduction reduce = popReduction();
    Debug.assertion(reduce != null, "popped TU element should not be invalid");

    return null;
  }

  @Override
  public Object visit(ASTStreamHeader node, Object data) throws CompileException {

    TContext stream_context = node.getBindContext();
    Debug.assertion(stream_context != null, "Context should not be null");
    TContext upper_context = (TContext) stream_context.getOwnerType();

    switch (node.getStreamForm()) {
    case LangUnitNode.STREAMFORM_DFLT:
      Debug.assertion(stream_context.isForm(AbsType.FORM_STREAM),
          "Context Type should be stream, but " + stream_context.getFormString(stream_context.getForm()));
      stream_context.initChildVarIdx(upper_context.getNextChidVarIdx());

      ((TContextStream) stream_context).setIsDefinitionStream(node.hasDefinitionRefernce());

      break;
    case LangUnitNode.STREAMFORM_CLASS:
      Debug.assertion(stream_context.isForm(AbsType.FORM_CLASS),
          "Context Type should be Class, but " + stream_context.getFormString(stream_context.getForm()));

      branch_tree.initRootBranch(new Branch(node));

      break;
    default:
      throw new CompileException("Invalid stream Type(" + node.getStreamForm() + ")");
    }

    this.pushContext(stream_context);

    dump_context_stack();

    return null;
  }

  @Override
  public Object visit(ASTStream node, Object data) throws CompileException {
    dump_reduction_stack();
    dump_context_stack();
    TContext stream_ctx = popContext();
    Debug.assertion(stream_ctx != null, "Context should not be null");

    LangUnitNode signature_node = node.getChildren(0);
    Debug.assertion(signature_node != null, "child node should not be null");
    Debug.assertion(signature_node.isNodeId(JJTSTREAMSIGNATURE), "child should be signature");

    LangUnitNode streamheader_node = signature_node.getChildren(0);
    Debug.assertion(streamheader_node != null, "child node should not be null");
    Debug.assertion(streamheader_node.isNodeId(JJTSTREAMHEADER), "grand child should be stream head");

    switch (streamheader_node.getStreamForm()) {
    case LangUnitNode.STREAMFORM_DFLT:
      Debug.assertion(stream_ctx.isForm(AbsType.FORM_STREAM),
          "Context Form should be stream, but " + stream_ctx.getFormString(stream_ctx.getForm()));

      if (node.hasDefinitionRefernce()) { // this stream is code block
        pushReduction(stream_ctx);
      } else { // this stream is not code block, this is stream instance
        AbsType stream_type = (AbsType) cpLoader.findClassFull(CompilerLoader.LANGSTREAM_CLASS_NAME);
        Debug.assertion(stream_type != null, "stream_type should not be invalid");
        pushReduction(stream_type);
      }
      break;

    case LangUnitNode.STREAMFORM_CLASS:

      pushReduction(stream_ctx);
      break;
    default:
      throw new CompileException("Invalid stream form");
    }

    return null;
  }

  @Override
  public Object visit(ASTCatchExceptionList node, Object data) throws CompileException {

    TContext catch_context = node.getBindContext();
    Debug.assertion(catch_context != null, "Context should not be null");
    Debug.assertion(catch_context.isForm(AbsType.FORM_STREAM),
        "Context Type should be stream, but " + catch_context.getFormString(catch_context.getForm()));

    TContext upperstream_ctx = (TContext) catch_context.getOwnerType();
    Debug.assertion(upperstream_ctx != null, "stream Context should be valid");
    Debug.assertion(upperstream_ctx.isForm(TContext.FORM_STREAM), "upperstream_ctx should be stream Context");

    catch_context.initChildVarIdx(upperstream_ctx.getNextChidVarIdx());

    Container dfltexcp_cont = catch_context.getLocalChildVariable(TContext.VAR_EXCEPTION_NAME);
    Debug.assertion(dfltexcp_cont != null, "dfltexcp_cont should be valid");
    dfltexcp_cont.bindVarIdx();

    this.pushContext(catch_context);
    dump_context_stack();

    branch_tree.pushBranch(new Branch(node));

    return null;
  }

  @Override
  public Object visit(ASTCatch node, Object data) throws CompileException {

    dump_context_stack();
    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");

    branch_tree.popBranch();

    return null;
  }

  @Override
  public Object visit(ASTFunctionDefHdr node, Object data) throws CompileException {

    TContext func_ctx = node.getBindContext();
    Debug.assertion(func_ctx != null, "Context should not be null");

    switch (node.getFuncForm()) {
    case LangUnitNode.FUNCFORM_MEMBER:
    case LangUnitNode.FUNCFORM_STATIC:
      Debug.assertion(func_ctx.isForm(AbsType.FORM_FUNC),
          "Context Type should be Function, but " + func_ctx.getFormString(func_ctx.getForm()));
      break;
    default:
      throw new CompileException("Invalid Func Form(" + node.getFuncForm() + ")");
    }

    if (((TContextFunc) func_ctx).is_closure()) {

      TContext closure_context = (TContext) func_ctx.getOwnerType();
      Debug.assertion(closure_context != null, "Context should not be null");
      Debug.assertion(closure_context instanceof TContextClosure, "popped context should be TContextClosure");

      updateClosureMember(closure_context);

      this.pushContext(closure_context);
    }

    pushContext(func_ctx);

    branch_tree.pushBranch(new Branch(node));

    dump_context_stack();

    return null;
  }

  private void updateClosureMember(TContext closure_ctx) throws CompileException {
    TContext closureowner_ctx = (TContext) closure_ctx.getOwnerType();
    Debug.assertion(closureowner_ctx != null, "curr_ctx should be valid");

    Container var_cont = null;

    LinkedList<Container> var_list = closure_ctx.get_childvar_list();
    int var_size = var_list.size();
    Container closure_member_var = null;

    for (int i = 0; i < var_size; i++) {
      closure_member_var = var_list.get(i);
      var_cont = closure_member_var.getClosureOrgFuncvarContainer();

      Debug.assertion(var_cont != null, "var_cont should be valid");
      // Debug.assertion(var_cont.isForm(Container.FORM_FUNSTACK_VAR), "var_cont
      // should be stack variable");
      Debug.assertion(var_cont.isTypeInitialized(), "var_cont should be type initialized(var_cont=" + var_cont + ")");

      closure_member_var.initializeType(var_cont.getType());
      closure_member_var.bindVarIdx();
    }

  }

  @Override
  public Object visit(ASTFunctionDef node, Object data) throws CompileException {
    dump_reduction_stack();
    dump_context_stack();
    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");

    LangUnitNode func_sig_node = node.getChildren(0);
    Debug.assertion(func_sig_node != null, "child node should not be null");
    Debug.assertion(func_sig_node.isNodeId(JJTFUNCTIONDEFSIGNATURE), "child should be signature");

    LangUnitNode functionheader_node = func_sig_node.getChildren(0);
    Debug.assertion(functionheader_node != null, "child node should not be null");
    Debug.assertion(functionheader_node.isNodeId(JJTFUNCTIONDEFHDR), "grand child should be func head");

    switch (functionheader_node.getFuncForm()) {
    case LangUnitNode.FUNCFORM_MEMBER:
    case LangUnitNode.FUNCFORM_STATIC:

      TContextFunc func_ctx = (TContextFunc) context;

      if (func_ctx.is_apply()) {

        AbsType class_context = func_ctx.getOwnerType();
        String node_name = CompilerLoader.getApplyNodeFileName(class_context.getName(), func_ctx.getName(),
            PathUtil.mtdDscToFileName(func_ctx.getMthdDscStr()));

        LangUnitNode ref_node = node.getClosestParent(JJTREFERENCE);
        Debug.assertion(ref_node != null, "Function Def should have reference node");

        cpLoader.writeNodeFile(ref_node, node_name);
      }

      if (func_ctx.is_closure()) {
        TContext closure_context = popContext();
        Debug.assertion(closure_context != null, "Context should not be null");
        Debug.assertion(closure_context instanceof TContextClosure, "popped context should be TContextClosure");

        if (func_ctx.is_apply()) {

          // apply function cannot be delivered as Method Handle.
          // If it needs to deliver as method handle, type should be applied.
          // So, here it reduce a function type instead of Method Handle
          pushReduction(func_ctx);
        } else {
          TMethodHandle mh_type = TMethodHandle.getInstance(cpLoader);
          Debug.assertion(mh_type != null, "mh_type should not be invalid");
          mh_type.setFuncSignatureDesc((func_ctx).getFuncSignatureDesc());
          pushReduction((AbsType) mh_type);
        }
      } else {

        pushReduction(func_ctx);
      }

      if (!func_ctx.is_apply()) {
        checkFunctionReturn(node, functionheader_node, func_ctx);
      }

      branch_tree.popBranch();

      break;
    default:
      throw new CompileException("Invalid func form");
    }

    return null;
  }

  private void checkFunctionReturn(LangUnitNode funcdef_node, LangUnitNode functionheader_node, TContextFunc func_ctx)
      throws CompileException {
    AbsType ret_type = func_ctx.getReturnType(cpLoader);

    Branch branch = branch_tree.getCurrBranch();

    Debug.assertion(branch != null, "branch should be valid");

    Debug.assertion(branch.getNode() != null, "branch node should be valid");

    if (branch.getNode() != functionheader_node) {
      // throw new InterpreterException("Invalid Function
      // Return("+branch.getNode()+")", funcdef_node);
      throw new CompileException("Invalid Function Return", funcdef_node);
    }

    int onexprfuncbody_idx = funcdef_node.getChildIdxWithId(JJTONEEXPRFUNCBODY, 0);

    if (onexprfuncbody_idx == -1) {
      if (ret_type.isName(CompilerLoader.UNRESOLVED_TYPE_CLASS_NAME)) {
        if (branch.isValid()) {
          AbsType void_ret_type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_VOID);
          Debug.assertion(void_ret_type != null, "inferenced_ret_type should be valid");
          func_ctx.getFuncSignatureDesc().initReturnType(void_ret_type);
        } else {
          throw new CompileException("This function must return a result ", funcdef_node);
        }

      } else if (!ret_type.isName(TPrimitiveClass.NAME_VOID)) {
        if (branch.isValid()) {
          throw new CompileException("This function must return a result of type " + ret_type.getName(), funcdef_node);
        }
      }

    }

  }

  @Override
  public Object visit(ASTOneExprFuncBody node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 1, "One Expression Function Body should have one child");

    Reduction reduce = popReduction(); // consume reduction
    Debug.assertion(reduce != null, "Reduction should not be invalid");
    Debug.assertion(reduce.isType()
    /* || reduce.isControl() */, "Invalid Popped Reduction " + reduce);

    AbsType ret_type = (AbsType) reduce;

    TContext func_context = getTopContext();
    Debug.assertion(func_context.isForm(TContext.FORM_FUNC), "Top Contextion should be function");

    AbsType func_ret_type = ((AbsFuncType) func_context).getReturnType(cpLoader);
    Debug.assertion(func_ret_type != null, "Reduce Type is not defined");

    LOG.debug("REDUCE TYPE: {}", func_ret_type);
    LOG.debug("CONT TYPE: {}", ret_type);

    if (func_ret_type.isName(CompilerLoader.UNRESOLVED_TYPE_CLASS_NAME)) {
      if (!ret_type.isName(CompilerLoader.APPLY_CLASS_NAME)) {
        // return type inference
        ((TContextFunc) func_context).getFuncSignatureDesc().initReturnType(ret_type);
      }
    } else {
      if (!func_ret_type.equals(ret_type)) {
        throw new CompileException("Invalid Function Return Type(" + "Function Return Type: " + func_ret_type
            + ", but return with : " + ret_type);
      }
    }

    dump_reduction_stack();

    return null;
  }

  @Override
  public Object visit(ASTStreamBlockElementHdr node, Object data) throws CompileException {

    check_unreachablecode(node);

    return null;
  }

  @Override
  public Object visit(ASTStreamBlockElement node, Object data) throws CompileException {

    // check_unreachablecode(node);

    Debug.assertion(node.jjtGetNumChildren() == 2, "stream block element should have one child");

    Reduction reduce = popReduction(); // consume reduction
    Debug.assertion(reduce != null, "Reduction should not be invalid");
    Debug.assertion(reduce.isType() || reduce.isControl(), "Invalid Popped Reduction " + reduce);

    dump_reduction_stack();
    return null;
  }

  @Override
  public Object visit(ASTArgumentList node, Object data) throws CompileException {

    int num_args = node.jjtGetNumChildren();
    Debug.assertion(num_args >= 1, "It should have arguments");

    // consumes arguments from reduction stack
    LOG.debug("Consumes arguments from reduction stack");
    Reduction reduce = null;
    AbsType type = null;

    dump_reduction_stack();

    // construct function argument type list ( this is for finding function.. )

    AbsTypeList argtype_list = new AbsTypeList();

    for (int i = 0; i < num_args; i++) {
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);

      type = (AbsType) reduce;
      argtype_list.addFirst(type);
    }

    LOG.debug("Calling function Argument method dsc string: {}", argtype_list.getMthdDsc());
    node.setArgumentTypeList(argtype_list);

    return null;
  }

  @Override
  public Object visit(ASTReference node, Object data) throws CompileException {

    LangUnitNode parent_node = (LangUnitNode) node.jjtGetParent();
    Reduction reduce = null;
    AbsType type = null;
    AbsType converting_type = null;

    // apply function reference can have null parent
    if (parent_node != null) {
      if (parent_node.isNonAssignOperationNode()) {
        int num_parent_child = parent_node.jjtGetNumChildren();

        Debug.assertion(
            (num_parent_child == 3) // some non-assing op can have 'hdr'
                                    // node(LogicalAND..)
                || (num_parent_child == 2) || (num_parent_child == 1),
            "invalid parent childnum(" + parent_node.jjtGetNumChildren() + "), " + "Parent(" + parent_node + ")");

        int idx = parent_node.getChildIdx(node);

        if (is_lval_refnode(num_parent_child, idx)) {
          // for auto converting
          // a = java.lang.Integer
          // b = 10 or java.lang.Integer
          // a + b : here, a is converted to 'int'
          reduce = popReduction();
          Debug.assertion(reduce != null, "reduce should be valid");
          Debug.assertion(reduce.isType(), "reduce should be type");

          type = (AbsType) reduce;
          converting_type = type.op().get_lval_coverting_type_of_nonassign_dual_op();

          if (converting_type == null) {
            pushReduction(reduce);
          } else {
            node.setConvertingType(converting_type);
            pushReduction(converting_type);
          }

        }
      } else if (parent_node.isBooleanReduceNode()) {
        Debug.assertion(parent_node.jjtGetNumChildren() == 1, "Boolean Reduce Node child Num should be 1");
        reduce = popReduction();
        Debug.assertion(reduce != null, "reduce should be valid");
        Debug.assertion(reduce.isType(), "reduce should be type");

        type = (AbsType) reduce;
        converting_type = type.op().get_boolreduce_coverting_type();

        if (converting_type == null) {
          pushReduction(reduce);
        } else {
          node.setConvertingType(converting_type);
          pushReduction(converting_type);
        }
      }

    }

    return null;
  }

  @Override
  public Object visit(ASTAccess node, Object data) throws CompileException {

    LangUnitNode access_child_node = (LangUnitNode) node.jjtGetChild(0);
    Debug.assertion(access_child_node != null, "first child should not be null");
    LangUnitNode ref_node = (LangUnitNode) node.jjtGetParent();

    if (access_child_node.isNodeId(JJTSYMBOLNAME)) {
      Container container = access_child_node.getContainer(); // this container
                                                              // is configured
                                                              // on
                                                              // SymbolResolvingVisitor
      LOG.debug("container: {}", container);

      if (container != null) {
        // for map creation in MAP Access
        if (isNextSubRefChildNodeid(ref_node, JJTMAPACCESS)) {
          if (container.isForm(Container.FORM_TYPE)) {
            pushReduction(container);
            return null;
          }
        }

        // Variable Access Case
        // Debug.assertion(container.isTypeInitialized(), "variable type should
        // be initialized");
        if (!container.isTypeInitialized()) {
          if (node.isAssignTgt()) {
            // case for type initialization in assignment (stack variable)
            pushReduction(container);
            return null;
          } else {
            throw new CompileException("variable type should be initialized");
          }
        } else {
          // type & context is initialized
          if (!container.isContextVarInitialized() && container.getContext() != null) {
            container.bindVarIdx();
          }
        }

        AbsType reduce_type = container.getType();
        pushReduction(reduce_type);
        return null;
      } else {
        // Invoking Function Case
        // symbol node does not have container. It means this is invoking
        // function
        // It needs to resolve invoking function with token image
        Debug.assertion(isNextSubRefChildNodeid(ref_node, JJTINVOKE), "Next Child Sub Ref should be Function Call.");

        InvokeInfo call_info = new InvokeInfo(access_child_node, InvokeInfo.FORM_LOCAL);
        pushReduction(call_info);
        return null;
      }
    } else {
      // do nothing for other child node types
    }

    return null;

  }

  @Override
  public Object visit(ASTSubAccess node, Object data) throws CompileException {

    LangUnitNode subaccess_child_node = (LangUnitNode) node.jjtGetChild(0);
    Debug.assertion(subaccess_child_node != null, "first child should not be null");

    if (subaccess_child_node.isNodeId(JJTSYMBOLNAME)) {
      Token symbol_token = subaccess_child_node.getAstToken();
      Debug.assertion(symbol_token != null, "SubAccess Symbol should be valid");

      Reduction owner_reduce = popReduction(); // pop src type
      Debug.assertion(owner_reduce != null, "Invalid Popped Reduction");
      Debug.assertion(owner_reduce.isType(), "Popped Reduction should be type " + owner_reduce);

      AbsType owner_type = (AbsType) owner_reduce;
      AbsType symbol_type = null;
      AbsType subref_reduce_type = null;
      Container symbol_cont = null;

      LangUnitNode sub_ref_node = (LangUnitNode) node.jjtGetParent();

      if (isNextSubRefChildNodeid(sub_ref_node, JJTINVOKE)) {
        // Invoking Function Case
        InvokeInfo invoke_info = new InvokeInfo(subaccess_child_node, InvokeInfo.FORM_MEMBER);
        invoke_info.setOwnerType(owner_type);
        pushReduction(invoke_info);
      } else {
        // Accessing Variable Case
        if (owner_type.isForm(AbsType.FORM_PKG)) {
          String pkgmember_name = owner_type.getName() + CompilerLoader.COMPILER_PKG_DELIMETER + symbol_token.image;

          symbol_type = cpLoader.findPackage(pkgmember_name);

          if (symbol_type == null) {
            // 'pkgmember_name' is not package
            symbol_type = (AbsType) cpLoader.findClassFull(pkgmember_name);
            if (symbol_type == null) {
              throw new CompileException("It can not find (" + pkgmember_name + ")", node);
            }
          }
          // here it succeeded to find package or class
          symbol_cont = new Container(symbol_type.getName(), Container.FORM_TYPE, true, false);

          symbol_cont.initializeType(symbol_type);
          subaccess_child_node.setContainer(symbol_cont);
          subref_reduce_type = symbol_type;

          // for map creation in MAP Access
          if (isNextSubRefChildNodeid(sub_ref_node, JJTMAPACCESS)) {
            pushReduction(symbol_cont);
            return null;
          }

        } else if (owner_type.isForm(AbsType.FORM_CLASS)) {
          LOG.debug("Finding member({}) from {}", symbol_token.image, owner_type);
          symbol_cont = owner_type.getLocalChildVariable(symbol_token.image);
          Debug.assertion(symbol_cont != null, "Invalid Member Variable(" + symbol_token.image + ") in " + owner_type);
          Debug.assertion(symbol_cont.isTypeInitialized(),
              "Member(" + symbol_token.image + " in " + owner_type + ") type should be initialized");
          subaccess_child_node.setContainer(symbol_cont);
          subref_reduce_type = symbol_cont.getType();
        } else if (owner_type.isForm(AbsType.FORM_MAP)) {
          if (symbol_token.image.equals("length")) {
            subref_reduce_type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_INT);

            Container token_cont = new Container("anonymous", Container.FORM_SPECIALTOKEN, true, false);
            token_cont.initializeType(subref_reduce_type);
            token_cont.setAssigned(true);
            token_cont.setSpecialToken(symbol_token);

            subaccess_child_node.setContainer(token_cont);
          } else {
            throw new CompileException(
                "owner_type(" + owner_type + ") does not have member(" + symbol_token.image + ")");
          }
        } else {
          throw new CompileException("owner_type(" + owner_type + ") does not support sub access");
        }

        pushReduction(subref_reduce_type);
      }
    } else {
      // do nothing for other child node types
    }

    return null;
  }

  @Override
  public Object visit(ASTInvoke node, Object data) throws CompileException {

    LangUnitNode arglist_node = null;
    AbsTypeList invoke_argtype_list = new AbsTypeList();
    AbsType reduce_type = null;
    AbsType class_type = null;
    AbsType found_func_type = null;
    AbsType owner_type = null;
    AbsType const_type = null;

    TContextFunc ctx_func = getClosestFunContext();
    Debug.assertion(ctx_func != null, "ctx_func should be valid");

    if (node.jjtGetNumChildren() == 2) {
      // it has argument
      arglist_node = (LangUnitNode) node.jjtGetChild(1); // argument list node
      Debug.assertion(arglist_node != null, "2'nd child should not be null");
      Debug.assertion(arglist_node.isNodeId(JJTARGUMENTLIST), "2'nd child should be Argument");

      Debug.assertion(arglist_node.getArgumentTypeList() != null, "argument type list should not be null");
      invoke_argtype_list = arglist_node.getArgumentTypeList();
    }

    Reduction reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");

    if (reduce.isInvokeInfo()) {
      // invoking function with invoke info(use symbol token)
      InvokeInfo invoke_info = (InvokeInfo) reduce;

      TContext tuContext = getTopContext().getClosestAncestor(AbsType.FORM_TU);
      Debug.assertion(tuContext != null, "tuContext should not be invalid");
      Debug.assertion(tuContext.isForm(TContext.FORM_TU), "tuContext should be Translation Unit");

      LangUnitNode symbol_node = invoke_info.getSymbolNode();
      Debug.assertion(symbol_node != null, "symbol_node should be valid");
      Debug.assertion(symbol_node.isNodeId(JJTSYMBOLNAME), "symbol_node should be Symbol Node");

      Token symbol_token = symbol_node.getAstToken();

      if (invoke_info.isForm(InvokeInfo.FORM_LOCAL)) {
        // find class on local Translation Unit
        class_type = (AbsType) cpLoader.findClass(symbol_token.image, (TContextTU) tuContext);
        found_func_type = null;

        if (class_type != null) { // constructor call with token image
          LOG.debug("Finding Class Constructor(arg dsc: {})", invoke_argtype_list.getMthdDsc());

          found_func_type = (AbsType) ((AbsClassType) class_type).getLocalConstructor(invoke_argtype_list);
          Debug.assertion(found_func_type != null,
              class_type.getName() + " Constructor (" + invoke_argtype_list.getMthdDsc() + ") is not defined");
          reduce_type = class_type;

          LOG.debug("found_func_type: {} {}", found_func_type, found_func_type.getMthdDscStr());
        } else { // function call case with token image
          LOG.debug("Finding Local Function on Local Class");
          class_type = (TContextClass) getTopContext().getClosestAncestor(AbsType.FORM_CLASS); // calling
                                                                                               // this
                                                                                               // function
          Debug.assertion(class_type != null, "class context should not be invalid");

          // func_type =
          // (AbsType)((AbsClassType)class_type).getLocalFunction(symbol_token.image,
          // argtype_list);
          found_func_type = cpLoader.findLocalFunction((AbsClassType) class_type, symbol_token.image,
              invoke_argtype_list);

          Debug.assertion(found_func_type != null,
              symbol_token.image + "(" + invoke_argtype_list.getMthdDsc() + ") is not defined");
          LOG.debug("found_func_type={}", found_func_type);

          // Apply Type 2 : Function Parameter is an apply function.
          // method foo( fvar:f(int, int)->int )->int : fvar(1, 2)
          // foo( fn(a, b) := a + b )
          // - argument of foo is apply function type 'fn(a, b) := a + b'
          // - argument apply function is applied to parameter type 'f(int,
          // int)->int )->int'
          // - argument apply function 'fn(a, b) := a + b' is changed to
          // 'fn(a:int, b:int)->int := a + b'
          if (has_applyfunctype_argument(invoke_argtype_list)) {
            // some parameter is apply type
            Debug.assertion(found_func_type instanceof AbsFuncType, "func_type should be AbsFuncType");

            if (((AbsFuncType) found_func_type).is_apply()) {
              throw new CompileException("Apply Function Parameter cannot be applied to Apply Function");
            }

            apply_applyfunctype_argument(invoke_argtype_list, (AbsFuncType) found_func_type, node);
          }

          LOG.debug("found_func_type={} {}", found_func_type, found_func_type.getMthdDscStr());
          if (((AbsFuncType) found_func_type).is_apply()) {
            // Apply Type 3-2: Invoking Function is apply function. Apply
            // function based on the invoking parameter
            // c = foo2(10, 20)
            // method foo2(a, b) := a + b
            apply_at_invoke(node, invoke_argtype_list, (AbsFuncType) found_func_type);

            if (!ctx_func.is_static()) {
              // current function is not static
              invoke_info.getSymbolNode().setIsDfltContextMember(true);
              // Found function is a 'this' member of class
            }

            return null; // return here
          }

          if (ctx_func.is_static()) {
            // current function is not static
            if (!((AbsFuncType) found_func_type).is_static()) {
              throw new CompileException("static function cannot invoke non-static function");
            }
          } else {
            // current function is not static
            // static function does not have dflt ctx member... this...
            invoke_info.getSymbolNode().setIsDfltContextMember(true); // Found
                                                                      // function
                                                                      // is a
                                                                      // this
                                                                      // member
                                                                      // of
                                                                      // class
          }

          reduce_type = ((AbsFuncType) found_func_type).getReturnType(cpLoader);

        }

        cpLoader.checkExceptionHandling(found_func_type, getTopContext());

        update_symbol_node(symbol_node, found_func_type, invoke_argtype_list, arglist_node);

        pushReduction(reduce_type);
      } else if (invoke_info.isForm(InvokeInfo.FORM_MEMBER)) {
        owner_type = invoke_info.getOwnerType();
        Debug.assertion(owner_type != null, "src_type should be valid");

        if (owner_type.isForm(AbsType.FORM_PKG)) {
          String pkgmember_name = owner_type.getName() + CompilerLoader.COMPILER_PKG_DELIMETER + symbol_token.image;

          class_type = (AbsType) cpLoader.findClassFull(pkgmember_name);
          Debug.assertion(class_type != null, "class_type(" + pkgmember_name + ") should be valid");

          const_type = (AbsType) ((AbsClassType) class_type).getLocalConstructor(invoke_argtype_list);
          Debug.assertion(const_type != null,
              symbol_token.image + "(" + invoke_argtype_list.getMthdDsc() + ") is not defined");
          reduce_type = class_type;

          update_symbol_node(symbol_node, const_type, invoke_argtype_list, arglist_node);
        } else if (owner_type.isForm(AbsType.FORM_CLASS)) {
          Container container = owner_type.getLocalChildVariable(symbol_token.image);

          if (container != null && container.getType() instanceof TMethodHandle) {
            // case for calling member variable(method handle)
            TMethodHandle mh_type = (TMethodHandle) container.getType();
            reduce_type = mh_type.getFuncSignatureDesc().getReturnType();

            symbol_node.setContainer(container);
          } else {
            // case for finding member function
            // here, owner_type local child variable can have same name with
            // 'symbol_token.image'
            // but, that is not the thing to find. Class can have both the
            // function and the variable
            // which have same name (ex. LinkedList has both the 'size' member
            // var and 'size()' function

            LOG.debug("Finding Member Function");

            class_type = owner_type;
            found_func_type = cpLoader.findLocalFunction((AbsClassType) class_type, symbol_token.image,
                invoke_argtype_list);
            Debug.assertion(found_func_type != null,
                symbol_token.image + "(" + invoke_argtype_list.getMthdDsc() + ") is not defined in " + class_type);

            LOG.debug("found_func_type={}", found_func_type);

            // Apply Type 2 : Function Parameter is an apply function.
            // method foo( fvar:f(int, int)->int )->int : fvar(1, 2)
            // foo( fn(a, b) := a + b )
            // - argument of foo is apply function type 'fn(a, b) := a + b'
            // - argument apply function is applied to parameter type 'f(int,
            // int)->int )->int'
            // - argument apply function 'fn(a, b) := a + b' is changed to
            // 'fn(a:int, b:int)->int := a + b'
            if (has_applyfunctype_argument(invoke_argtype_list)) {
              Debug.assertion(found_func_type instanceof AbsFuncType, "func_type should be AbsFuncType");

              if (((AbsFuncType) found_func_type).is_apply()) {
                throw new CompileException("Apply Function Parameter cannot be applied to Apply Function");
              }
              apply_applyfunctype_argument(invoke_argtype_list, (AbsFuncType) found_func_type, node);
            }

            LOG.debug("found_func_type={} {}", found_func_type, found_func_type.getMthdDscStr());
            if (((AbsFuncType) found_func_type).is_apply()) {
              // Apply Type 3-2: Invoking Function is apply function. Apply
              // function based on the invoking parameter
              // c = foo2(10, 20)
              // method foo2(a, b) := a + b
              apply_at_invoke_sub(node, invoke_argtype_list, (AbsFuncType) found_func_type);
              // invoke_info.getSymbolNode().setIsDfltContextMember(true); //
              // Found function is a 'this' member of class
              // update_invoke_node(node, func_type, invoke_argtype_list,
              // arglist_node);

              return null; // return here
              // throw new InterpreterException("Member apply function is not
              // supported currently");
            }

            reduce_type = ((AbsFuncType) found_func_type).getReturnType(cpLoader);

            update_symbol_node(symbol_node, found_func_type, invoke_argtype_list, arglist_node);
          }

        } else {
          throw new CompileException("Not Supported Owner Type(" + owner_type + ")");
        }

        pushReduction(reduce_type);

      } else {
        throw new CompileException("Not Supported InvokeInfo From(" + invoke_info + ")");
      }

    } else if (reduce.isType()) {
      if (reduce instanceof TMethodHandle) {
        TMethodHandle mh_type = (TMethodHandle) reduce;

        LOG.debug("argtype_list(arg dsc:{})", invoke_argtype_list.getMthdDsc());
        LOG.debug("method handle(arg dsc:{})", mh_type.getFuncSignatureDesc().getParamTypeMthdDsc());

        if (!cpLoader.isImpliedCastibleParameterList(invoke_argtype_list,
            mh_type.getFuncSignatureDesc().getParameterTypeList())) {
          throw new CompileException("Non compatible parameter invoke( method hande: " + mh_type.getFuncSignatureDesc()
              + ", invoking argument: " + invoke_argtype_list + ")");
        }

        reduce_type = mh_type.getFuncSignatureDesc().getReturnType();
        Debug.assertion(reduce_type != null, "reduce_type should be valid");
        pushReduction(reduce_type);

      } else if (reduce instanceof AbsFuncType) {
        // Apply Type 3-1 : function apply when function invoke
        // c = ( fn(a, b) := a + b ) (5, 5)
        AbsFuncType func_type = (AbsFuncType) reduce;
        LOG.debug("func_type: {}", func_type);
        if (func_type.is_apply()) {
          apply_at_invoke(node, invoke_argtype_list, func_type);
        } else {
          throw new CompileException("reduce(" + reduce + ") is FuncType, but is_apply is false");
        }
      } else if (reduce instanceof AbsClassType) {
        // Calling 'this()' or 'super()' can reach here
        process_this_super_call_in_constructor(node, reduce, arglist_node, invoke_argtype_list);

      } else {
        throw new CompileException("Not Supported reduce(" + reduce + ")");
      }
    } else {
      throw new CompileException("Not Supported reduce(" + reduce + ")");
    }

    return null;
  }

  private void process_this_super_call_in_constructor(ASTInvoke node, Reduction reduce, LangUnitNode arglist_node,
      AbsTypeList invoke_argtype_list) throws CompileException {
    // Calling 'this()' or 'super()' can reach here
    AbsType class_type = (AbsType) reduce;

    LangUnitNode ref_node = node.getClosestParent(JJTREFERENCE);
    Debug.assertion(ref_node != null, "ref_node should be valid");
    Debug.assertion(ref_node.isNodeId(JJTREFERENCE), "ref_node should be ref node");

    LangUnitNode access_node = ref_node.getChildren(0);
    Debug.assertion(access_node != null, "access_node should be valid");
    Debug.assertion(access_node.isNodeId(JJTACCESS), "access_node should be access node");

    LangUnitNode symbol_node = access_node.getChildren(0);
    Debug.assertion(symbol_node != null, "symbol_node should be valid");
    Debug.assertion(symbol_node.isNodeId(JJTSYMBOLNAME), "symbol_node should be symbol node");

    Token symbol_token = symbol_node.getAstToken();
    Debug.assertion(symbol_token != null, "symbol_token should be valid");

    switch (symbol_token.kind) {
    case ParserConstants.THIS:
    case ParserConstants.SUPER:
      break;
    default:
      if (reduce.isType()) {
        if (((AbsType) reduce).isName(CompilerLoader.APPLY_CLASS_NAME)) {
          pushReduction(reduce);
          return;
        }
      }

      throw new CompileException("Variable Invoking is only permitted for 'this' or 'super'");
    }

    LangUnitNode streamblock_ele_node = (LangUnitNode) ref_node.jjtGetParent();
    Debug.assertion(streamblock_ele_node != null, "streamblock_ele_node should be valid");
    Debug.assertion(streamblock_ele_node.isNodeId(JJTSTREAMBLOCKELEMENT),
        "invalid invoking '" + symbol_token.image + "' position " + streamblock_ele_node);

    LangUnitNode streamblock_node = (LangUnitNode) streamblock_ele_node.jjtGetParent();
    Debug.assertion(streamblock_node != null, "streamblock_node should be valid");
    Debug.assertion(streamblock_node.isNodeId(JJTSTREAMBLOCK),
        "invalid invoking '" + symbol_token.image + "' position " + streamblock_node);

    int idx = streamblock_node.getChildIdx(streamblock_ele_node);
    if (idx != 1) {
      throw new CompileException("Invoking '" + symbol_token.image + "' must be first expression");
    }

    TContext curr_class = getTopContext().getClosestAncestor(TContext.FORM_CLASS);
    Debug.assertion(curr_class != null, "curr_class should be valid");

    if (symbol_token.kind == ParserConstants.THIS && !class_type.equals(curr_class)) {
      throw new CompileException("Variable cannot be invoked");
    }

    AbsClassType super_class = ((AbsClassType) curr_class).getSuperClass();
    Debug.assertion(super_class != null, "super_class should be valid");

    if (symbol_token.kind == ParserConstants.SUPER && !class_type.equals(super_class)) {
      throw new CompileException("Variable cannot be invoked");
    }

    TContext curr_func = getTopContext().getClosestAncestor(TContext.FORM_FUNC);
    Debug.assertion(curr_func != null, "curr_func should be valid");

    if (!((TContextFunc) curr_func).is_constructor()) {
      throw new CompileException("Variable Invoking can be permitted in constructor");
    }

    LOG.debug("Finding Class Constructor(arg dsc:{})", invoke_argtype_list.getMthdDsc());

    AbsType const_type = (AbsType) ((AbsClassType) class_type).getLocalConstructor(invoke_argtype_list);
    Debug.assertion(const_type != null,
        class_type.getName() + "(" + invoke_argtype_list.getMthdDsc() + ") is not defined");

    ((TContextFunc) curr_func).set_has_constructor_call(true);

    AbsType reduce_type = ((AbsFuncType) const_type).getReturnType(cpLoader);

    update_invoke_node(node, const_type, invoke_argtype_list, arglist_node);

    pushReduction(reduce_type);
  }

  private void apply_at_invoke(ASTInvoke node, AbsTypeList invoke_argtype_list, AbsFuncType found_func_type)
      throws CompileException {
    FuncSignatureDesc tgt_sig = new FuncSignatureDesc(cpLoader);

    tgt_sig.updateParameterTypeList(invoke_argtype_list);

    LangUnitNode func_ref_node = apply((AbsType) found_func_type, tgt_sig, node);

    Debug.assertion(func_ref_node.jjtGetNumChildren() == 1, "new func ref should have only one child");

    LangUnitNode funchdr_node = getFuncHdrNodeFromFunRefNode(func_ref_node);
    Debug.assertion(funchdr_node != null, "funchdr_node should be valid");

    TContext func_ctx = funchdr_node.getBindContext();
    Debug.assertion(func_ctx != null, "func_ctx should be valid");

    AbsType tgt_ret_type = ((AbsFuncType) func_ctx).getReturnType(cpLoader);
    Debug.assertion(tgt_ret_type != null, "tgt_ret_type should be valid");

    // Temporal : Is this correct .. ?
    LangUnitNode org_ref_node = node.getClosestParent(JJTREFERENCE);
    Debug.assertion(org_ref_node != null, "ref_node should be valid");
    Debug.assertion(org_ref_node.isNodeId(JJTREFERENCE), "ref_node should be ref node");
    // 2nd child is sub-ref for invoke !!
    Debug.assertion(org_ref_node.jjtGetNumChildren() == 2, "org_ref_node should have 2 child");

    LangUnitNode org_subref_node = org_ref_node.getChildren(1);
    Debug.assertion(org_subref_node != null, "org_subref_node should be valid");
    Debug.assertion(org_subref_node.isNodeId(JJTSUBREFERENCE), "ref_node should be ref node");

    // Sub reference should be invoke
    LangUnitNode org_subref_access_node = null;
    if (org_subref_node.jjtGetNumChildren() == 1) {
      org_subref_access_node = org_subref_node.getChildren(0);
    } else if (org_subref_node.jjtGetNumChildren() == 2) {
      org_subref_access_node = org_subref_node.getChildren(1);
    } else {
      throw new CompileException("Invalid org_subref_node child num (" + org_subref_node.jjtGetNumChildren() + ")");
    }

    Debug.assertion(org_subref_access_node.isNodeId(JJTINVOKE), "Sub Ref Access child should be invoke.");

    func_ref_node.jjtAddChild(org_subref_node, 1);
    org_subref_node.jjtSetParent(func_ref_node);

    LangUnitNode refparent_node = (LangUnitNode) org_ref_node.jjtGetParent();
    Debug.assertion(refparent_node != null, "refparent_node should be valid");

    int org_refnode_idx = refparent_node.getChildIdx(org_ref_node);
    Debug.assertion(org_refnode_idx != -1, "arg_node should have reference child");

    refparent_node.jjtSetChild(org_refnode_idx, func_ref_node);
    func_ref_node.jjtSetParent(refparent_node);

    Reduction new_reduce = popReduction();
    Debug.assertion(new_reduce != null, "Invalid Popped Reduction");
    Debug.assertion(new_reduce.isType(), "Invalid Popped Reduction " + new_reduce);

    pushReduction(tgt_ret_type);
  }

  private void apply_at_invoke_sub(ASTInvoke node, AbsTypeList invoke_argtype_list, AbsFuncType found_func_type)
      throws CompileException {
    FuncSignatureDesc tgt_sig = new FuncSignatureDesc(cpLoader);

    tgt_sig.updateParameterTypeList(invoke_argtype_list);

    LangUnitNode func_ref_node = apply((AbsType) found_func_type, tgt_sig, node);

    LangUnitNode apply_access_node = (LangUnitNode) func_ref_node.jjtGetChild(0);
    Debug.assertion(apply_access_node != null, "apply_access_node should be valid");
    Debug.assertion(apply_access_node.isNodeId(JJTACCESS),
        "apply_access_node should be access, but " + apply_access_node);

    LangUnitNode apply_funcdef_node = (LangUnitNode) apply_access_node.jjtGetChild(0);
    Debug.assertion(apply_funcdef_node != null, "apply_funcdef_node should be valid");
    Debug.assertion(apply_funcdef_node.isNodeId(JJTFUNCTIONDEF),
        "apply_funcdef_node should be function def, but " + apply_funcdef_node);

    Debug.assertion(func_ref_node.jjtGetNumChildren() == 1, "new func ref should have only one child");

    LangUnitNode funchdr_node = getFuncHdrNodeFromFunRefNode(func_ref_node);
    Debug.assertion(funchdr_node != null, "funchdr_node should be valid");

    TContext func_ctx = funchdr_node.getBindContext();
    Debug.assertion(func_ctx != null, "func_ctx should be valid");

    AbsType tgt_ret_type = ((AbsFuncType) func_ctx).getReturnType(cpLoader);
    Debug.assertion(tgt_ret_type != null, "tgt_ret_type should be valid");

    // Temporal : Is this correct .. ?
    LangUnitNode invoke_subref_node = node.getClosestParent(JJTSUBREFERENCE);
    Debug.assertion(invoke_subref_node != null, "invoke_subref_node should be valid");
    Debug.assertion(invoke_subref_node.isNodeId(JJTSUBREFERENCE), "invoke_subref_node should be sub ref node");

    LangUnitNode prev_subaccess_node = getPrevRefAccessNode(invoke_subref_node);
    Debug.assertion(prev_subaccess_node != null, "prev_subaccess_node should be valid");
    Debug.assertion(prev_subaccess_node.isNodeId(JJTSUBACCESS),
        "prev_subaccess_node should be subaccess node, but " + prev_subaccess_node);

    LangUnitNode prev_subaccesschild_node = prev_subaccess_node.getChildren(0);
    Debug.assertion(prev_subaccesschild_node != null, "prev_subaccesschild_node should be valid");
    Debug.assertion(prev_subaccesschild_node.isNodeId(JJTSYMBOLNAME),
        "prev_subaccesschild_node should be symbolname node, but " + prev_subaccesschild_node);

    LangUnitNode prev_sub_ref_node = (LangUnitNode) prev_subaccess_node.jjtGetParent();
    Debug.assertion(prev_sub_ref_node != null, "prev_sub_ref_node should be valid");
    Debug.assertion(prev_sub_ref_node.isNodeId(JJTSUBREFERENCE), "prev_sub_ref_node should be sub ref node");

    prev_subaccess_node.jjtSetChild(0, apply_funcdef_node);
    apply_funcdef_node.jjtSetParent(prev_subaccess_node);

    Reduction new_reduce = popReduction();
    Debug.assertion(new_reduce != null, "Invalid Popped Reduction");
    Debug.assertion(new_reduce.isType(), "Invalid Popped Reduction " + new_reduce);

    Container type_cont = func_ctx.getTypeContainer();
    // Container type_cont = new Container(func_ctx.getName(),
    // Container.FORM_TYPE, false, true);
    // type_cont.initializeType(func_ctx);

    apply_funcdef_node.setContainer(type_cont);

    LangUnitNode closese_refnode = node.getClosestParent(JJTREFERENCE);
    Debug.assertion(closese_refnode != null, "closese_refnode should be valid");

    closese_refnode.setIsDefinition(true);

    pushReduction(tgt_ret_type);

  }

  private LangUnitNode getFuncHdrNodeFromFunRefNode(LangUnitNode func_ref_node) throws CompileException {
    LangUnitNode apply_access_node = (LangUnitNode) func_ref_node.jjtGetChild(0);
    Debug.assertion(apply_access_node != null, "apply_access_node should be valid");
    Debug.assertion(apply_access_node.isNodeId(JJTACCESS),
        "apply_access_node should be access, but " + apply_access_node);

    LangUnitNode apply_funcdef_node = (LangUnitNode) apply_access_node.jjtGetChild(0);
    Debug.assertion(apply_funcdef_node != null, "apply_funcdef_node should be valid");
    Debug.assertion(apply_funcdef_node.isNodeId(JJTFUNCTIONDEF),
        "apply_funcdef_node should be function def, but " + apply_funcdef_node);

    LangUnitNode apply_funcsig_node = (LangUnitNode) apply_funcdef_node.jjtGetChild(0);
    Debug.assertion(apply_funcsig_node != null, "apply_funcsig_node should be valid");
    Debug.assertion(apply_funcsig_node.isNodeId(JJTFUNCTIONDEFSIGNATURE),
        "apply_funcsig_node should be function sig, but " + apply_funcsig_node);

    int funchdr_idx = apply_funcsig_node.getChildIdxWithId(JJTFUNCTIONDEFHDR, 0);
    Debug.assertion(funchdr_idx != -1, "funcsig_node should have paralist");

    LangUnitNode funchdr_node = (LangUnitNode) apply_funcsig_node.jjtGetChild(funchdr_idx);
    Debug.assertion(funchdr_node != null, "funchdr_node should be valid");
    Debug.assertion(funchdr_node.isNodeId(JJTFUNCTIONDEFHDR),
        "funchdr_node should be funchdr_node, but " + funchdr_node);

    return funchdr_node;
  }

  private void apply_applyfunctype_argument(AbsTypeList invoke_argtype_list, AbsFuncType found_func_type,
      LangUnitNode dbg_node) throws CompileException {
    FuncSignatureDesc found_func_sig = found_func_type.getFuncSignatureDesc();

    Debug.assertion(found_func_sig != null, "found_func_sig should be valid");
    AbsTypeList found_func_argtype_list = found_func_sig.getParameterTypeList();

    Debug.assertion(found_func_argtype_list != null, "found_func_argtype_list should be valid");
    AbsType invoke_arg_type = null;
    AbsType found_func_para_type = null;
    int size = invoke_argtype_list.size();

    for (int i = 0; i < size; i++) {
      invoke_arg_type = invoke_argtype_list.get(i);

      if (invoke_arg_type instanceof AbsFuncType && ((AbsFuncType) invoke_arg_type).is_apply()) {
        found_func_para_type = found_func_argtype_list.get(i);

        Debug.assertion(found_func_para_type instanceof TMethodHandle,
            "tgt_para_type should be TMethodHandle, but " + found_func_para_type);

        LangUnitNode func_ref_node = apply(invoke_arg_type,
            ((TMethodHandle) found_func_para_type).getFuncSignatureDesc(), dbg_node);

        // apply closure should be TContext ..?
        Debug.assertion(invoke_arg_type instanceof TContext, "invoke_arg_type should be TContext");

        LangUnitNode apply_funcnode = ((TContext) invoke_arg_type).getBindNode();
        Debug.assertion(apply_funcnode != null, "apply_funcnode should be valid");
        Debug.assertion(apply_funcnode.isNodeId(JJTFUNCTIONDEFHDR), "apply_funcnode should be function hdr");

        LangUnitNode applyfunc_ref_node = apply_funcnode.getClosestParent(JJTREFERENCE);
        Debug.assertion(applyfunc_ref_node != null, "applyfunc_ref_node should be valid");
        Debug.assertion(applyfunc_ref_node.isNodeId(JJTREFERENCE), "applyfunc_ref_node should be reference");

        LangUnitNode arg_node = (LangUnitNode) applyfunc_ref_node.jjtGetParent();
        Debug.assertion(arg_node != null, "arg_node should be valid");
        Debug.assertion(arg_node.isNodeId(JJTARGUMENT), "arg_node should be argument node");

        int refnode_idx = arg_node.getChildIdxWithId(JJTREFERENCE, 0);
        Debug.assertion(refnode_idx != -1, "arg_node should have reference child");

        int another_refnode_idx = arg_node.getChildIdxWithId(JJTREFERENCE, refnode_idx + 1);
        Debug.assertion(another_refnode_idx == -1, "arg_node should have only one reference child");

        arg_node.jjtSetChild(refnode_idx, func_ref_node);
        func_ref_node.jjtSetParent(arg_node);

        // underlying Closure might push its method handle
        Reduction reduce = popReduction();
        Debug.assertion(reduce != null, "Invalid Popped Reduction");
        Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
        AbsType new_applyfunc_type = (AbsType) reduce;

        invoke_argtype_list.set(i, new_applyfunc_type);
      }

    }

  }

  private boolean has_applyfunctype_argument(AbsTypeList argtype_list) throws CompileException {
    int size = argtype_list.size();
    AbsType arg_type = null;

    for (int i = 0; i < size; i++) {
      arg_type = (AbsType) argtype_list.get(i);

      if (arg_type instanceof AbsFuncType && ((AbsFuncType) arg_type).is_apply()) {
        return true;
      }
    }

    return false;
  }

  private void update_symbol_node(LangUnitNode symbol_node, AbsType found_func_type, AbsTypeList invoke_argtype_list,
      LangUnitNode arglist_node) throws CompileException {
    Debug.assertion(symbol_node.isNodeId(JJTSYMBOLNAME), "funcallheader_node should be symbol");

    Container foundfunctype_container = found_func_type.getTypeContainer();
    Debug.assertion(foundfunctype_container != null, "type_container should be valid");

    symbol_node.setContainer(foundfunctype_container);

    AbsTypeList tgt_argtype_list = ((AbsFuncType) found_func_type).getFuncSignatureDesc().getParameterTypeList();
    Debug.assertion(tgt_argtype_list != null, "tgt_argtype_list should be valid");

    String tgt_arg_type_dsc = TypeUtil.getArgTypeDsc(found_func_type.getMthdDscStr());
    Debug.assertion(tgt_arg_type_dsc != null, "Target Function Argument Type Dsc should be valid");

    if (!invoke_argtype_list.getMthdDsc().equals(tgt_arg_type_dsc)) {
      if (cpLoader.isVarArgAbsTypeList(invoke_argtype_list, tgt_argtype_list, true)) {
        // found function has variable argument list
        // it needs to construct array of argument
        updateVarArgConversion(invoke_argtype_list, tgt_argtype_list, arglist_node);
      } else {
        updateArgumentImpliedCasting(invoke_argtype_list, tgt_argtype_list, arglist_node);
      }
    }
  }

  private void update_invoke_node(LangUnitNode invoke_node, AbsType found_func_type, AbsTypeList invoke_argtype_list,
      LangUnitNode arglist_node) throws CompileException {
    Debug.assertion(invoke_node.isNodeId(JJTINVOKE), "funcallheader_node should be invoke");

    Container foundfunctype_container = found_func_type.getTypeContainer();
    Debug.assertion(foundfunctype_container != null, "type_container should be valid");

    invoke_node.setContainer(foundfunctype_container);

    AbsTypeList tgt_argtype_list = ((AbsFuncType) found_func_type).getFuncSignatureDesc().getParameterTypeList();
    Debug.assertion(tgt_argtype_list != null, "tgt_argtype_list should be valid");

    String tgt_arg_type_dsc = TypeUtil.getArgTypeDsc(found_func_type.getMthdDscStr());
    Debug.assertion(tgt_arg_type_dsc != null, "Target Function Argument Type Dsc should not be invalid");

    if (!invoke_argtype_list.getMthdDsc().equals(tgt_arg_type_dsc)) {
      updateArgumentImpliedCasting(invoke_argtype_list, tgt_argtype_list, arglist_node);
    }
  }

  private void setArgNodeChildConvertingType(LangUnitNode arg_node, AbsType type) throws CompileException {
    Debug.assertion(arg_node.isNodeId(JJTARGUMENT), "arg_node should be argument node");
    Debug.assertion(arg_node.jjtGetNumChildren() == 2, "num of arg_node child should be 2");

    LangUnitNode argnode_child = arg_node.getChildren(1);
    Debug.assertion(argnode_child != null, "argnode_child should be valid");

    LOG.debug("setArgNodeChildConverting:{}", argnode_child);

    argnode_child.setConvertingType(type);
  }

  private void updateVarArgConversion(AbsTypeList invoke_argtype_list, AbsTypeList found_argtype_list,
      LangUnitNode arglist_node) throws CompileException {
    int num_invoking_args = arglist_node.jjtGetNumChildren();
    Debug.assertion(num_invoking_args >= 1, "It should have arguments");

    if (num_invoking_args != invoke_argtype_list.size()) {
      throw new CompileException(
          "number of argument is not mismatch(" + num_invoking_args + ", " + found_argtype_list.size());
    }

    int found_func_vararg_idx = found_argtype_list.size() - 1; // variable
                                                               // argument
                                                               // list(array) is
                                                               // the last
                                                               // argument

    AbsType vararg_type = found_argtype_list.get(found_func_vararg_idx);
    Debug.assertion(vararg_type != null, "vararg_type should be valid");
    Debug.assertion(vararg_type instanceof TMapType, "vararg_type should be TMapType");

    AbsType ele_type = ((TMapType) vararg_type).getElementType();
    Debug.assertion(ele_type != null, "ele_type should be valid");

    TMapType map_type = cpLoader.findMapType(ele_type, 1); // array type
    Debug.assertion(map_type != null, "map_type should be valid");
    Container maptype_cont = map_type.getTypeContainer();
    Debug.assertion(maptype_cont != null, "maptype_cont should be valid");

    LangUnitNode arg_node = null;

    // non variable argument parts
    for (int i = 0; i < found_func_vararg_idx; i++) {
      if (cpLoader.isCompatibleClass(invoke_argtype_list.get(i), found_argtype_list.get(i))) {
        // do nothing
      } else if (cpLoader.isConvertibleClass(invoke_argtype_list.get(i), found_argtype_list.get(i))) {
        arg_node = (LangUnitNode) arglist_node.jjtGetChild(i);
        Debug.assertion(arg_node != null, "Argument Node should not be invalid");
        LOG.debug("setConvertingType({})->({})", invoke_argtype_list.get(i), found_argtype_list.get(i));
        setArgNodeChildConvertingType(arg_node, found_argtype_list.get(i));
      } else {
        throw new CompileException(
            "Invalid Type casting " + invoke_argtype_list.get(i) + "-> " + found_argtype_list.get(i));
      }
    }

    LangUnitNode arghdr_node = null;
    int varargmap_size = num_invoking_args - found_func_vararg_idx;
    // varargmap_size can be zero

    Debug.assertion(varargmap_size >= 0, "varargmap_size (" + varargmap_size + ") should be zero or positive");

    if (varargmap_size > 0) {
      // variable argument parts
      for (int i = found_func_vararg_idx, mapidx = 0; i < num_invoking_args; i++, mapidx++) {
        arg_node = (LangUnitNode) arglist_node.jjtGetChild(i);
        Debug.assertion(arg_node != null, "Argument Node should not be invalid");
        Debug.assertion(arg_node.isNodeId(JJTARGUMENT), "arg_node should be argument");

        arg_node.setVarArgMapTypeContainer(maptype_cont);
        arg_node.setVarArgMapIdx(mapidx);
        arg_node.setVarArgMapSize(varargmap_size);

        arghdr_node = arg_node.getChildren(0);
        Debug.assertion(arghdr_node != null, "arghdr_node should not be invalid");
        Debug.assertion(arghdr_node.isNodeId(JJTARGUMENTHDR), "arghdr_node should be argument hdr");

        arghdr_node.setVarArgMapTypeContainer(maptype_cont);
        arghdr_node.setVarArgMapIdx(mapidx);
        arghdr_node.setVarArgMapSize(varargmap_size);

        if (cpLoader.isCompatibleClass(invoke_argtype_list.get(i), ele_type)) {
          // do nothing
        } else if (cpLoader.isConvertibleClass(invoke_argtype_list.get(i), ele_type)) {
          LOG.debug("setConvertingType({})->({})", invoke_argtype_list.get(i), ele_type);
          setArgNodeChildConvertingType(arg_node, ele_type);
        } else {
          throw new CompileException("Invalid Type casting " + invoke_argtype_list.get(i) + "-> " + ele_type);
        }
      }
    } else if (varargmap_size == 0) {
      // for making dummy var arg array
      // Note !!
      // * invoke_argtype_list can have no var arg list
      // example) public static java.nio.file.Path get(String first, String...
      // more)
      // but, 'Path.get("./a") is possible( invoking parameter does not have var
      // arg list ).
      // Compiler automatically add String[0] following the "./a"

      arglist_node.setVarArgMapTypeContainer(maptype_cont);
      arglist_node.setVarArgMapSize(varargmap_size);
    } else {
      throw new CompileException("varargmap_size (" + varargmap_size + ") should be zero or positive", arglist_node);
    }

  }

  private void updateArgumentImpliedCasting(AbsTypeList src_argtype_list, AbsTypeList tgt_argtype_list,
      LangUnitNode arglist_node) throws CompileException {
    int num_args = arglist_node.jjtGetNumChildren();
    Debug.assertion(num_args >= 1, "It should have arguments");

    if (num_args != tgt_argtype_list.size()) {
      throw new CompileException("number of argument is not mismatch(" + num_args + ", " + tgt_argtype_list.size());
    }

    LangUnitNode arg_node = null;

    for (int i = 0; i < num_args; i++) {
      if (cpLoader.isCompatibleClass(src_argtype_list.get(i), tgt_argtype_list.get(i))) {
        // do nothing
      } else if (cpLoader.isConvertibleClass(src_argtype_list.get(i), tgt_argtype_list.get(i))) {
        arg_node = (LangUnitNode) arglist_node.jjtGetChild(i);
        Debug.assertion(arg_node != null, "Argument Node should not be invalid");
        Debug.assertion(arg_node.isNodeId(JJTARGUMENT), "arg_node should be argument");

        LOG.debug("setConvertingType({})->({})", src_argtype_list.get(i), tgt_argtype_list.get(i));
        setArgNodeChildConvertingType(arg_node, tgt_argtype_list.get(i));
      } else {
        throw new CompileException("Invalid Type casting " + src_argtype_list.get(i) + "-> " + tgt_argtype_list.get(i));
      }
    }
  }

  @Override
  public Object visit(ASTMapAccess node, Object data) throws CompileException {

    Reduction reduce = popReduction(); // pop key type
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isType(), "Popped Reduction should be type " + reduce);

    AbsType arg_type = (AbsType) reduce;

    reduce = popReduction(); // pop src type
    Debug.assertion(reduce != null, "Invalid Popped Reduction");

    if (reduce.isType()) {
      // Map Instance Type - Map Access Case
      AbsType src_type = (AbsType) reduce;

      LOG.debug("key_type:{}", arg_type);
      LOG.debug("src_type:{}", src_type);

      // Container maptype_cont = src_type.getTypeContainer();
      // Debug.assertion( maptype_cont!=null, "maptype_cont should be valid");
      // node.setContainer(maptype_cont);

      //Operation op = new Operation(ParserConstants.GET, node, getTopContext());

      // AbsType ret_type = op.getRetTypeOfNonAssingOperation(src_type,
      // key_type, cpLoader);
      AbsType ret_type = src_type.op().get_return_type_of_nonassign_dualchild_op(ParserConstants.GET, src_type,
          arg_type, getTopContext());

      LOG.debug("ret_type:{}", ret_type);

      pushReduction(ret_type);
    } else if (reduce.isContainer()) {
      // Type Container - Map Creation Case
      Container type_cont = (Container) reduce;
      Debug.assertion(type_cont.isForm(Container.FORM_TYPE), "type_cont should be type container, but " + type_cont);

      AbsType ele_type = type_cont.getType();
      Debug.assertion(ele_type != null, "map_type should be valid");

      LangUnitNode sub_ref_node = (LangUnitNode) node.jjtGetParent();
      Debug.assertion(sub_ref_node.isNodeId(JJTSUBREFERENCE), "ref_node should be subref node");

      int map_dimension = 1;
      TMapType map_type = null;
      Container maptype_cont = null;

      if (isNextSubRefChildNodeid(sub_ref_node, JJTMAPACCESS)) { // [case-01]
                                                                 // v - next is
                                                                 // MapAccess
                                                                 // map creation
                                                                 // -
                                                                 // int[10][10][10]
        map_type = cpLoader.findMapType(ele_type, map_dimension);
        Debug.assertion(map_type != null, "map_type reduce should be valid");
        LOG.debug("new map_type: {} ({})", map_type, map_type.getMthdDscStr());

        maptype_cont = map_type.getTypeContainer();
        Debug.assertion(maptype_cont != null, "maptype_cont should be valid");
        pushReduction(maptype_cont);
        // Debug.stop();
      } else { // [case-02]
               // v - create map
               // map creation - int[10][][] : create 10 * int[][][]
               // or
               // v - next is not Map Access(null or other thing..)
               // map creation - int[10][10][10] : create int[10][10][10]
               // [case-01] will be end here

        if (ele_type instanceof TMapType) {
          LOG.debug("creting map type {}", ele_type);
        }

        map_dimension = node.getMapDimesion() + 1;
        map_type = cpLoader.findMapType(ele_type, map_dimension);
        Debug.assertion(map_type != null, "map_type reduce should be valid");
        LOG.debug("new map_type: {}({})", map_type, map_type.getMthdDscStr());

        maptype_cont = map_type.getTypeContainer();
        Debug.assertion(maptype_cont != null, "maptype_cont should be valid");
        node.setContainer(maptype_cont);
        pushReduction(map_type);
      }

    } else {
      throw new CompileException("Popped Reduction should be type or container, but " + reduce);
    }

    return null;
  }

  @Override
  public Object visit(ASTConstant node, Object data) throws CompileException {

    Container const_cont = node.getContainer();
    Debug.assertion(const_cont != null, "Container should not be null");

    AbsType const_type = const_cont.getType();
    Debug.assertion(const_type != null, "Constant type should not be null");

    pushReduction(const_type); // this push is for parameter type resolution
    return null;
  }

  @Override
  public Object visit(ASTJumpExpr node, Object data) throws CompileException {

    Token t = node.getAstToken();
    Debug.assertion((t != null), "AstToken should not be null");

    switch (t.kind) {
    case ParserConstants.RETURN:
      return_type_resolution(node);
      branch_tree.getCurrBranch().setValid(false);
      break;

    case ParserConstants.THROW:
      throw_type_resolution(node);
      branch_tree.getCurrBranch().setValid(false);
      break;

    case ParserConstants.CONTINUE:
      continue_process(node);
      branch_tree.getCurrBranch().setValid(false);
      break;

    case ParserConstants.BREAK:
      break_process(node);
      branch_tree.getCurrBranch().setValid(false);
      break;

    default:
      throw new CompileException("JumpExpr(" + t.image + ") is not defined");
    }

    dump_reduction_stack();
    return null;
  }

  private void break_process(ASTJumpExpr node) throws CompileException {
    LangUnitNode func_node = node.getClosestParent(JJTFUNCTIONDEF);
    Debug.assertion(func_node != null, "func_node should be valid");

    LangUnitNode breakrange_node = node.getClosestParentsUntil(new int[] { JJTFORBODY }, func_node);

    if (breakrange_node == null) {
      throw new CompileException("'break' cannot be used outside of a loop/switch", node);
    }

    if (breakrange_node.isNodeId(JJTFORBODY)) {
      LangUnitNode loopimpl_node = (LangUnitNode) breakrange_node.jjtGetParent();
      Debug.assertion(loopimpl_node != null, "loopimpl_node should be valid");

      if (!loopimpl_node.hasDefinitionRefernce()) {
        throw new CompileException("'break' cannot be used in reducing loop/switch", node);
      }

      pushReduction(new Control(Control.FORM_BREAK));

    } else {
      throw new CompileException("Invalid Node(" + ParserConstants.tokenImage[breakrange_node.getNodeId()] + ")");
    }
  }

  private void continue_process(ASTJumpExpr node) throws CompileException {
    LangUnitNode func_node = node.getClosestParent(JJTFUNCTIONDEF);
    Debug.assertion(func_node != null, "func_node should be valid");

    LangUnitNode forbody_node = node.getClosestParentUntil(JJTFORBODY, func_node);
    if (forbody_node == null) {
      throw new CompileException("'continue' cannot be used outside of a loop", node);
    }

    LangUnitNode loopimpl_node = (LangUnitNode) forbody_node.jjtGetParent();
    Debug.assertion(loopimpl_node != null, "loopimpl_node should be valid");

    if (!loopimpl_node.hasDefinitionRefernce()) {
      throw new CompileException("'continue' cannot be used in reducing loop", node);
    }

    pushReduction(new Control(Control.FORM_CONTINUE));
  }

  private void return_type_resolution(ASTJumpExpr node) throws CompileException {
    TContextFunc func_context = getClosestFunContext();
    AbsType func_ret_type = ((AbsFuncType) func_context).getReturnType(cpLoader);
    Debug.assertion(func_ret_type != null, "Reduce Type is not defined");
    LOG.debug("func_ret_type: {}", func_ret_type);

    if (func_ret_type.isName(CompilerLoader.UNRESOLVED_TYPE_CLASS_NAME)) {
      // return type inference
      switch (node.jjtGetNumChildren()) {
      case 0: // no return value case
        AbsType void_ret_type = (AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_VOID);
        Debug.assertion(void_ret_type != null, "inferenced_ret_type should be valid");
        func_context.getFuncSignatureDesc().initReturnType(void_ret_type);
        break;

      case 1: // it has return value
        Reduction reduce = this.popReduction();
        Debug.assertion(reduce != null, "Invalid Popped Reduction");
        Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
        AbsType ret_type = (AbsType) reduce;
        func_context.getFuncSignatureDesc().initReturnType(ret_type);
        /*
         * if( ! cpLoader.isCompatibleClass(func_ret_type, ret_type) && !
         * cpLoader.isConvertibleClass(func_ret_type, ret_type) ) throw new
         * InterpreterException("Return Type is not mismatched");
         */
        break;

      default:
        throw new CompileException("Invalid # of Return child(" + node.jjtGetNumChildren() + ")");
      }

    } else {
      // return type checking
      switch (node.jjtGetNumChildren()) {
      case 0: // no return value case
        if (!func_ret_type.isName(TPrimitiveClass.NAME_VOID)) {
          throw new CompileException("Return Type is not mismatched");
        }
        break;

      case 1: // it has return value
        Reduction reduce = this.popReduction();
        Debug.assertion(reduce != null, "Invalid Popped Reduction");
        Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
        AbsType ret_type = (AbsType) reduce;

        // if( ! cpLoader.isCompatibleClass(func_ret_type, ret_type) &&
        // ! cpLoader.isConvertibleClass(func_ret_type, ret_type) )
        if (!cpLoader.isCompatibleClass(ret_type, func_ret_type)
            && !cpLoader.isConvertibleClass(ret_type, func_ret_type)) {
          throw new CompileException("Return Type is not mismatched");
        }
        break;

      default:
        throw new CompileException("Invalid # of Return child(" + node.jjtGetNumChildren() + ")");
      }

    }

    pushReduction(new Control(Control.FORM_RETURN));
  }

  private void throw_type_resolution(ASTJumpExpr node) throws CompileException {
    switch (node.jjtGetNumChildren()) {
    case 1: // it has return value
      Reduction reduce = this.popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
      AbsType throw_type = (AbsType) reduce;
      getTopContext().isAddressableException(throw_type);
      break;

    default:
      throw new CompileException("Invalid # of Throw child(" + node.jjtGetNumChildren() + ")");
    }

    pushReduction(new Control(Control.FORM_THROW));
  }

  @Override
  public Object visit(ASTUnary node, Object data) throws CompileException {

    Token t = node.getAstToken();
    Debug.assertion((t != null), "AstToken should not be null");

    Reduction reduce = null;
    AbsType type = null;
    AbsType ret_type = null;

    switch (t.kind) {
    case ParserConstants.PLUS_PLUS:
    case ParserConstants.MINUS_MINUS:
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isType(), "Popped Reduction should be Type, but " + reduce);
      type = (AbsType) reduce;

      ret_type = type.op().get_return_type_of_single_op(t.kind, type, getTopContext());
      Debug.assertion(ret_type != null, "ret_type should be valid");
      pushReduction(ret_type);

      if (!type.op().getOpString().equals("ObjectOp")) {
        primitive_unary_additive_preprocess(node);
      }

      break;

    case ParserConstants.MINUS: // -
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isType(), "Popped Reduction should be Type, but " + reduce);
      type = (AbsType) reduce;

      ret_type = type.op().get_return_type_of_single_op(t.kind, type, getTopContext());
      Debug.assertion(ret_type != null, "ret_type should be valid");
      pushReduction(ret_type);
      break;

    case ParserConstants.UNARY_NOT: // !
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isType(), "Popped Reduction should be Type, but " + reduce);
      type = (AbsType) reduce;

      ret_type = type.op().get_return_type_of_single_op(t.kind, type, getTopContext());
      Debug.assertion(ret_type != null, "ret_type should be valid");
      pushReduction(ret_type);
      break;

    case ParserConstants.UNARY_INVERSE: // ~
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isType(), "Popped Reduction should be Type, but " + reduce);
      type = (AbsType) reduce;

      ret_type = type.op().get_return_type_of_single_op(t.kind, type, getTopContext());
      Debug.assertion(ret_type != null, "ret_type should be valid");
      pushReduction(ret_type);
      break;

    default:
      throw new CompileException("Unary(" + t.image + ") is not defined");
    }

    dump_reduction_stack();
    return null;
  }

  private void primitive_unary_additive_preprocess(ASTUnary node) throws CompileException {
    LangUnitNode ref_node = node.getChildren(0);
    Debug.assertion(ref_node != null, "ref_node should be valid");
    Debug.assertion(ref_node.isNodeId(JJTREFERENCE), "ref_node is reference");

    LangUnitNode rmost_access_node = findRightMostAccessNode(ref_node);
    Debug.assertion(rmost_access_node != null, "access_node should be valid");
    LOG.debug("rmost_access_node={}", rmost_access_node);

    LangUnitNode final_ref_node = (LangUnitNode) rmost_access_node.jjtGetParent();
    Debug.assertion(final_ref_node != null, "final_ref_node should be valid");

    if (final_ref_node.isNodeId(JJTSUBREFERENCE)) {
      LOG.debug("final_ref_node={}", final_ref_node);

      if (rmost_access_node.isNodeId(JJTSUBACCESS)) {
        LangUnitNode rmostaccess_child = rmost_access_node.getChildren(0);
        Debug.assertion(rmostaccess_child != null, "rmostaccess_child should be valid");
        Debug.assertion(rmostaccess_child.isNodeId(JJTSYMBOLNAME), "rmostaccess_child should be symbolname");

        Container final_subacess_cont = rmostaccess_child.getContainer();
        Debug.assertion(final_subacess_cont != null, "final_subacess_cont should be valid");

        LangUnitNode prev_access_node = getPrevRefAccessNode(final_ref_node);
        Debug.assertion(prev_access_node != null, "prev_access_node should be valid");

        if (final_subacess_cont.isSingleton()) {
          // do nothing
        } else {
          prev_access_node.setDup(true);
        }
      } else if (rmost_access_node.isNodeId(JJTMAPACCESS)) {
        rmost_access_node.setAssignTgt(true);
      } else {
        throw new CompileException("Invalid rmost_access_node = " + rmost_access_node);
      }
    } else {
      // final postix additive target is not sub reference. ( a++ )
      // do nothing..
    }
  }

  @Override
  public Object visit(ASTPostfix node, Object data) throws CompileException {

    Token t = node.getAstToken();
    Debug.assertion((t != null), "AstToken should not be null");

    Reduction reduce = null;
    AbsType type = null;
    AbsType ret_type = null;

    switch (t.kind) {
    case ParserConstants.PLUS_PLUS:
    case ParserConstants.MINUS_MINUS:
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isType(), "Popped Reduction should be Type, but " + reduce);
      type = (AbsType) reduce;
      ret_type = type.op().get_return_type_of_single_op(t.kind, type, getTopContext());
      Debug.assertion(ret_type != null, "ret_type should be valid");
      pushReduction(ret_type);

      if (!type.op().getOpString().equals("ObjectOp")) {
        primitive_postfix_additive_preprocess(node);
      }

      break;

    default:
      throw new CompileException("Unary(" + t.image + ") is not defined");
    }

    dump_reduction_stack();
    return null;

  }

  private void primitive_postfix_additive_preprocess(ASTPostfix node) throws CompileException {
    LangUnitNode ref_node = node.getChildren(0);
    Debug.assertion(ref_node != null, "ref_node should be valid");
    Debug.assertion(ref_node.isNodeId(JJTREFERENCE), "ref_node is reference");

    LangUnitNode rmost_access_node = findRightMostAccessNode(ref_node);
    Debug.assertion(rmost_access_node != null, "access_node should be valid");
    LOG.debug("rmost_access_node={}", rmost_access_node);

    LangUnitNode final_ref_node = (LangUnitNode) rmost_access_node.jjtGetParent();
    Debug.assertion(final_ref_node != null, "final_ref_node should be valid");

    if (final_ref_node.isNodeId(JJTSUBREFERENCE)) {
      LOG.debug("final_ref_node={}", final_ref_node);

      if (rmost_access_node.isNodeId(JJTSUBACCESS)) {
        LangUnitNode rmostaccess_child = rmost_access_node.getChildren(0);
        Debug.assertion(rmostaccess_child != null, "rmostaccess_child should be valid");
        Debug.assertion(rmostaccess_child.isNodeId(JJTSYMBOLNAME), "rmostaccess_child should be symbolname");

        Container final_subacess_cont = rmostaccess_child.getContainer();
        Debug.assertion(final_subacess_cont != null, "final_subacess_cont should be valid");

        LangUnitNode prev_access_node = getPrevRefAccessNode(final_ref_node);
        Debug.assertion(prev_access_node != null, "prev_access_node should be valid");

        if (final_subacess_cont.isSingleton()) {
          rmost_access_node.setDup(true);
        } else {
          rmost_access_node.setDupX1(true);
          prev_access_node.setDup(true);
        }
      } else if (rmost_access_node.isNodeId(JJTMAPACCESS)) {
        rmost_access_node.setAssignTgt(true);

      } else {
        throw new CompileException("Invalid rmost_access_node = " + rmost_access_node);
      }

    } else {
      // final postix additive target is not sub reference. ( a++ )
      // do nothing..
    }
  }

  @Override
  public Object visit(ASTAssignment node, Object data) throws CompileException {

    LangUnitNode assignop_node = node.getChildren(1);
    Debug.assertion(assignop_node != null, "assignop_node should be valid");
    Debug.assertion(assignop_node.isNodeId(JJTASSIGNMENTOPERATOR), "assignop_node should be assign op");

    LangUnitNode assign_rval_node = node.getChildren(2);
    Debug.assertion(assign_rval_node != null, "assignop_node should be valid");

    Token assignop_token = assignop_node.getAstToken();
    Debug.assertion(assignop_token != null, "assignop_token should be valid");

    Reduction reduce = null;

    AbsType ret_type = null;
    AbsType l_value_type = null;
    AbsType r_value_type = null;

    dump_reduction_stack();
    Debug.assertion(reduction_stack.size() >= 2, "reduction_stack.size(" + reduction_stack.size() + ") should be >=2");

    // LOG.debug("pop rvalue type");
    reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
    r_value_type = (AbsType) reduce;

    // LOG.debug("pop lvalue type/type container");
    reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");

    if (reduce.isContainer()) {
      // case for type initialization in assignment (stack variable)
      Container lval_cont = (Container) reduce;
      Debug.assertion(!lval_cont.isTypeInitialized(), "lvalue container should not be type initialized");

      if (r_value_type.isName(TPrimitiveClass.NAME_VOID)) {
        throw new CompileException("Lvalue type specifier is required in Null initialization");
      }

      if (r_value_type.isForm(AbsType.FORM_FUNC) && ((TContextFunc) r_value_type).is_closure()
          && ((AbsFuncType) r_value_type).is_apply()) {
        throw new CompileException("Apply function cannot be assigned to Unresolved Type Variable");
      }

      lval_cont.initializeType(r_value_type);
      lval_cont.bindVarIdx();
      l_value_type = lval_cont.getType();
    } else {
      Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
      l_value_type = (AbsType) reduce;
    }

    // Apply function is always function type. Apply function cannot be method
    // handle
    // refer. to the visit(ASTFunctionDef
    if (r_value_type.isForm(AbsType.FORM_FUNC) && ((TContextFunc) r_value_type).is_closure()
        && ((AbsFuncType) r_value_type).is_apply()) {
      Debug.assertion(l_value_type instanceof TMethodHandle, "l_value_type should be instanceof TMethodHandle");

      // Apply Type 1 : function apply when function assignment
      // fvar : f(int, int)->int = fn (a, b) := a + b
      LangUnitNode func_ref_node = apply(r_value_type, ((TMethodHandle) l_value_type).getFuncSignatureDesc(), node);

      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
      r_value_type = (AbsType) reduce;

      LangUnitNode assing_src_node = (LangUnitNode) node.jjtGetChild(2);
      LOG.debug("assing_src_node={}", assing_src_node);

      node.jjtSetChild(2, func_ref_node);
      func_ref_node.jjtSetParent(node);
    }

    switch (assignop_token.kind) {
    case ParserConstants.GET:
    case ParserConstants.SHIFT_LEFT:
    case ParserConstants.SHIFT_RIGHT:
    case ParserConstants.SHIFT_LEFT_ASSIGN:
    case ParserConstants.SHIFT_RIGHT_ASSIGN:
      if (!r_value_type.isName(TPrimitiveClass.NAME_INT)) {
        throw new CompileException("rvalue type(" + r_value_type + ") should be int for operator "
            + ParserConstants.tokenImage[assignop_token.kind]);
      }
      break;

    default:

      if (cpLoader.isCompatibleClass(r_value_type, l_value_type)) {
        // do nothing
      } else if (cpLoader.isConvertibleClass(r_value_type, l_value_type)) {
        assign_rval_node.setConvertingType(l_value_type);
      } else {
        throw new CompileException(
            "lvalue type(" + l_value_type + ") operator " + ParserConstants.tokenImage[assignop_token.kind]
                + " is not defined for rvalue type(" + r_value_type + ")");
      }
    }

    ret_type = l_value_type;

    pushReduction(ret_type);
    dump_reduction_stack();

    Debug.assertion(node.jjtGetNumChildren() == 3,
        "the number of node child should be 3, but " + node.jjtGetNumChildren());

    // assignment target tagging. for assignment target 'access', 'load' will
    // not apply
    LangUnitNode assign_tgt_ref_node = node.getChildren(0);
    Debug.assertion(assign_tgt_ref_node.isNodeId(JJTREFERENCE), "Assingment Lvalue should be reference");

    LangUnitNode rmost_access_node = findRightMostAccessNode(assign_tgt_ref_node);
    Debug.assertion(rmost_access_node != null, "access_node should be valid");

    if (assignop_token.kind == ParserConstants.ASSIGN) {
      // do nothing..
    } else {
      LangUnitNode final_ref_node = (LangUnitNode) rmost_access_node.jjtGetParent();
      Debug.assertion(final_ref_node != null, "final_ref_node should be valid");

      if (final_ref_node.isNodeId(JJTSUBREFERENCE)) {

        if (rmost_access_node.isNodeId(JJTSUBACCESS)) {

          LangUnitNode rmostaccess_child = rmost_access_node.getChildren(0);
          Debug.assertion(rmostaccess_child != null, "rmostaccess_child should be valid");
          Debug.assertion(rmostaccess_child.isNodeId(JJTSYMBOLNAME), "rmostaccess_child should be symbolname");

          Container final_subacess_cont = rmostaccess_child.getContainer();
          Debug.assertion(final_subacess_cont != null, "final_subacess_cont should be valid");

          LangUnitNode prev_access_node = getPrevRefAccessNode(final_ref_node);
          Debug.assertion(prev_access_node != null, "prev_access_node should be valid");

          if (final_subacess_cont.isSingleton()) {
            // do nothing
          } else {
            prev_access_node.setDup(true);
          }
        } else if (rmost_access_node.isNodeId(JJTMAPACCESS)) {
          rmost_access_node.setDup2(true);
        } else {
          throw new CompileException("Invalid rmost_access_node = " + rmost_access_node);
        }
      } else {
        // final assign tgt is not sub reference ( 'a+='
        // do nothing
      }

    }

    return ret_type;
  }

  private LangUnitNode apply(AbsType applyfunc_type, FuncSignatureDesc found_sig, LangUnitNode dbg_node)
      throws CompileException {

    this.dump_context_stack();
    this.dump_reduction_stack();

    LOG.debug("\n#\n# Start to apply in {}", dbg_node);
    LOG.debug("...........................................................\n");

    AbsType applyowner_ctx = applyfunc_type.getOwnerType();
    String node_name = CompilerLoader.getApplyNodeFileName(applyowner_ctx.getName(), applyfunc_type.getName(),
        PathUtil.mtdDscToFileName(applyfunc_type.getMthdDscStr()));

    LangUnitNode apply_ref_node = cpLoader.readNodeFile(node_name);
    Debug.assertion(apply_ref_node != null, "apply_funcdef_node should be valid");
    // apply_ref_node.dump_tree(".", 0, 0);

    // this is temporal, actual original apply function owner can be different
    // TU
    TContext tu_contxt = getTopContext().getClosestAncestor(TContext.FORM_TU);

    apply_signature_to_parameterlist_node(apply_ref_node, found_sig);

    // apply_ref_node.dump_tree(".", 0, 0);

    TContextClass class_context = (TContextClass) getTopContext().getClosestAncestor(AbsType.FORM_CLASS);
    LOG.debug("class_context={}", class_context);

    ASTTraverseVisitor visitor = null;

    LOG.debug("\n#\n# ContextBuild in Apply {}", dbg_node);
    LOG.debug("...........................................................\n");
    visitor = new ASTContextBuildVisitor(cpLoader, tu_contxt.getName());
    visitor.traverse(apply_ref_node);

    LOG.debug("\n#\n# SymbolResolving in Apply {}", dbg_node);
    LOG.debug("...........................................................\n");
    visitor = new ASTSymbolResolvingVisitor(cpLoader);
    visitor.traverse(apply_ref_node);

    LOG.debug("\n#\n# SubSymbolResolving in Apply {}", dbg_node);
    LOG.debug("...........................................................\n");
    visitor = new ASTSubSymbolResolvingVisitor(cpLoader);
    visitor.traverse(apply_ref_node);

    this.dump_reduction_stack();

    LOG.debug("\n#\n# Apply is completed in {}", dbg_node);
    LOG.debug("...........................................................\n");

    return apply_ref_node;
  }

  private void apply_signature_to_parameterlist_node(LangUnitNode apply_ref_node, FuncSignatureDesc found_sig)
      throws CompileException {
    LangUnitNode apply_access_node = (LangUnitNode) apply_ref_node.jjtGetChild(0);
    Debug.assertion(apply_access_node != null, "apply_access_node should be valid");
    Debug.assertion(apply_access_node.isNodeId(JJTACCESS),
        "apply_access_node should be access, but " + apply_access_node);

    LangUnitNode apply_funcdef_node = (LangUnitNode) apply_access_node.jjtGetChild(0);
    Debug.assertion(apply_funcdef_node != null, "apply_funcdef_node should be valid");
    Debug.assertion(apply_funcdef_node.isNodeId(JJTFUNCTIONDEF),
        "apply_funcdef_node should be function def, but " + apply_funcdef_node);

    LangUnitNode apply_funcsig_node = (LangUnitNode) apply_funcdef_node.jjtGetChild(0);
    Debug.assertion(apply_funcsig_node != null, "apply_funcsig_node should be valid");
    Debug.assertion(apply_funcsig_node.isNodeId(JJTFUNCTIONDEFSIGNATURE),
        "apply_funcsig_node should be function sig, but " + apply_funcsig_node);

    LangUnitNode apply_funchdr_node = (LangUnitNode) apply_funcsig_node.jjtGetChild(0);
    Debug.assertion(apply_funchdr_node != null, "apply_funchdr_node should be valid");
    Debug.assertion(apply_funchdr_node.isNodeId(JJTFUNCTIONDEFHDR),
        "apply_funchdr_node should be function sig, but " + apply_funcsig_node);

    apply_funchdr_node.setAppliedFunc(true);

    int paralist_idx = apply_funcsig_node.getChildIdxWithId(JJTPARAMETERLIST, 0);
    Debug.assertion(paralist_idx != -1, "funcsig_node should have paralist");

    LangUnitNode apply_paralist_node = (LangUnitNode) apply_funcsig_node.jjtGetChild(paralist_idx);
    Debug.assertion(apply_paralist_node != null, "paralist_node should be valid");
    Debug.assertion(apply_paralist_node.isNodeId(JJTPARAMETERLIST),
        "paralist_node should be paralist_node, but " + apply_paralist_node);

    AbsTypeList found_sig_paratype_list = found_sig.getParameterTypeList();
    AbsType found_sig_paratype = null;

    int sig_para_size = found_sig_paratype_list.size();
    Debug.assertion(apply_paralist_node.jjtGetNumChildren() == sig_para_size, "apply_paralist_node child num("
        + apply_paralist_node.jjtGetNumChildren() + ") sig para size(" + sig_para_size + ") should be same");

    LangUnitNode param_node = null;
    LangUnitNode symbol_node = null;
    int type_specifier_child_idx = -1;

    ASTTypeDefine typedef_node = null;
    ASTType type_node = null;
    ASTApplyType applytype_node = null;

    LangUnitNode reducetype_node = null;

    // construct parameter type node
    for (int i = 0; i < sig_para_size; i++) {
      param_node = (LangUnitNode) apply_paralist_node.jjtGetChild(i);
      Debug.assertion(param_node != null, "param_node should be valid");
      Debug.assertion(param_node.isNodeId(JJTPARAMETER), "param_node should be parameter, but " + param_node);

      symbol_node = (LangUnitNode) param_node.jjtGetChild(0);
      Debug.assertion(symbol_node != null, "symbol_node should be valid");
      Debug.assertion(symbol_node.isNodeId(JJTSYMBOLNAME), "symbol_node should be symbol, but " + param_node);

      type_specifier_child_idx = symbol_node.getChildIdxWithId(JJTTYPEDEFINE, 0);

      Debug.assertion(type_specifier_child_idx == -1, "symbol_node has type specifier");

      typedef_node = new ASTTypeDefine(JJTTYPEDEFINE);
      typedef_node.jjtSetFirstToken(symbol_node.jjtGetFirstToken());
      symbol_node.jjtAddChild(typedef_node, typedef_node.jjtGetNumChildren());
      typedef_node.jjtSetParent(symbol_node);

      type_node = new ASTType(JJTTYPE);
      type_node.jjtSetFirstToken(symbol_node.jjtGetFirstToken());
      typedef_node.jjtAddChild(type_node, 0);
      type_node.jjtSetParent(typedef_node);

      applytype_node = new ASTApplyType(JJTAPPLYTYPE);
      applytype_node.jjtSetFirstToken(symbol_node.jjtGetFirstToken());
      type_node.jjtAddChild(applytype_node, 0);
      applytype_node.jjtSetParent(type_node);

      found_sig_paratype = found_sig_paratype_list.get(i);
      Debug.assertion(found_sig_paratype != null, "found_sig_paratype should be valid");

      applytype_node.setApplyType(found_sig_paratype);

      LOG.debug("({}) applytype_node=({}:{}), found_sig_paratype={}", i, applytype_node, applytype_node.hashCode(),
          found_sig_paratype);
    }

    LOG.debug("found_sig={}", found_sig.getMthdDscStr());

    AbsType found_sig_ret_type = found_sig.getReturnType();

    if (!found_sig_ret_type.isName(CompilerLoader.UNRESOLVED_TYPE_CLASS_NAME)) {
      int reducetype_idx = apply_funcsig_node.getChildIdxWithId(JJTREDUCETYPE, 0);

      if (reducetype_idx == -1) {
        // add reduce type node & type node
        reducetype_node = new ASTReduceType(JJTREDUCETYPE);
        reducetype_node.jjtSetFirstToken(apply_funcsig_node.jjtGetFirstToken());

        apply_funcsig_node.jjtAddChild(reducetype_node, apply_funcsig_node.jjtGetNumChildren());
        reducetype_node.jjtSetParent(apply_funcsig_node);

        type_node = new ASTType(JJTTYPE);
        type_node.jjtSetFirstToken(apply_funcsig_node.jjtGetFirstToken());
        reducetype_node.jjtAddChild(type_node, 0);
        type_node.jjtSetParent(reducetype_node);

        reducetype_idx = apply_funcsig_node.getChildIdxWithId(JJTREDUCETYPE, 0);
        Debug.assertion(reducetype_idx != -1, "function sig node should have reduce type");

      }

      reducetype_node = (LangUnitNode) apply_funcsig_node.jjtGetChild(reducetype_idx);
      Debug.assertion(reducetype_node != null, "reducetype_node should be valid");
      Debug.assertion(reducetype_node.isNodeId(JJTREDUCETYPE), "reducetype_node should be valid");

      type_node = (ASTType) reducetype_node.jjtGetChild(0);
      Debug.assertion(type_node != null, "type_node should be valid");
      Debug.assertion(type_node.isNodeId(JJTTYPE), "type_node should be type");

      applytype_node = new ASTApplyType(JJTAPPLYTYPE);
      applytype_node.jjtSetFirstToken(symbol_node.jjtGetFirstToken());
      type_node.jjtAddChild(applytype_node, 0);
      applytype_node.jjtSetParent(type_node);
      applytype_node.setApplyType(found_sig.getReturnType());

      apply_funcsig_node.setHasReduceTypeNode(true);
    }
  }

  @Override
  public Object visit(ASTLogicalOR node, Object data) throws CompileException {
    AbsType ret_type = dual_child_op_typecheck(node);
    pushReduction(ret_type);
    dump_reduction_stack();
    return null;
  }

  @Override
  public Object visit(ASTLogicalAND node, Object data) throws CompileException {
    AbsType ret_type = dual_child_op_typecheck(node);
    pushReduction(ret_type);
    dump_reduction_stack();
    return null;
  }

  @Override
  public Object visit(ASTInclusiveOR node, Object data) throws CompileException {
    AbsType ret_type = dual_child_op_typecheck(node);
    pushReduction(ret_type);
    dump_reduction_stack();
    return null;
  }

  @Override
  public Object visit(ASTExclusiveOR node, Object data) throws CompileException {
    AbsType ret_type = dual_child_op_typecheck(node);
    pushReduction(ret_type);
    dump_reduction_stack();
    return null;
  }

  @Override
  public Object visit(ASTAND node, Object data) throws CompileException {
    AbsType ret_type = dual_child_op_typecheck(node);
    pushReduction(ret_type);
    dump_reduction_stack();
    return null;
  }

  @Override
  public Object visit(ASTEquality node, Object data) throws CompileException {
    AbsType ret_type = dual_child_op_typecheck(node);
    pushReduction(ret_type);
    dump_reduction_stack();
    return null;
  }

  @Override
  public Object visit(ASTRelational node, Object data) throws CompileException {
    AbsType ret_type = dual_child_op_typecheck(node);
    pushReduction(ret_type);
    dump_reduction_stack();
    return null;
  }

  @Override
  public Object visit(ASTShift node, Object data) throws CompileException {
    AbsType ret_type = dual_child_op_typecheck(node);
    pushReduction(ret_type);
    dump_reduction_stack();
    return null;
  }

  @Override
  public Object visit(ASTAdditive node, Object data) throws CompileException {
    AbsType ret_type = dual_child_op_typecheck(node);
    pushReduction(ret_type);
    dump_reduction_stack();
    return null;
  }

  @Override
  public Object visit(ASTMultiplicative node, Object data) throws CompileException {
    AbsType ret_type = dual_child_op_typecheck(node);
    pushReduction(ret_type);
    dump_reduction_stack();
    return null;
  }

  public AbsType dual_child_op_typecheck(LangUnitNode node) throws CompileException {

    Reduction reduce = null;
    AbsType ret_type = null;
    AbsType l_value_type = null;
    AbsType r_value_type = null;

    // check_numof_childnode(2, node);
    Debug.assertion(reduction_stack.size() >= 2, "Invalid reduction_stack.size(" + reduction_stack.size() + ")");

    reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
    r_value_type = (AbsType) reduce;

    reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
    l_value_type = (AbsType) reduce;

    Token t = node.getAstToken();
    Operation op = new Operation(t.kind, node, getTopContext());

    op.nonassign_dualchild_op_rval_conversion(l_value_type, r_value_type, cpLoader);

    ret_type = l_value_type.op().get_return_type_of_nonassign_dualchild_op(t.kind, l_value_type, r_value_type,
        getTopContext());

    Debug.assertion(ret_type != null, "ret_type should be valid");
    return ret_type;
  }

  @Override
  public Object visit(ASTIfExpr node, Object data) throws CompileException {

    int num_child = node.jjtGetNumChildren();

    boolean is_def_ref = node.hasDefinitionRefernce();

    switch (num_child) {
    case 1: { // it has only if case
      Reduction if_case = popReduction();
      Debug.assertion(if_case != null, "Popped Reduction should not be invalid");

      if (is_def_ref) {
        pushReduction(new Control(Control.FORM_BRANCH));
      } else {
        throw new CompileException("else case is required in expression type branch", node);
      }

    }
      break;
    case 3: { // it has both if case and else case
      Reduction else_case_reduce = popReduction();
      Debug.assertion(else_case_reduce != null, "Popped Reduction should not be invalid");

      Reduction if_case_reduce = popReduction();
      Debug.assertion(if_case_reduce != null, "Popped Reduction should not be invalid");

      if (is_def_ref) {
        pushReduction(new Control(Control.FORM_BRANCH));
      } else {
        // do reduction type checking
        Debug.assertion(else_case_reduce.isType(), "Else case popped reduction should be container");
        AbsType elsecase_type = (AbsType) else_case_reduce;
        Debug.assertion(if_case_reduce.isType(), "If case popped reduction should be container");
        AbsType ifcase_type = (AbsType) if_case_reduce;

        if (!cpLoader.isCompatibleClass(ifcase_type, elsecase_type)
            && !cpLoader.isConvertibleClass(ifcase_type, elsecase_type)) {
          throw new CompileException(
              "If case reduction(" + ifcase_type + ") is not mismatched with else case (" + elsecase_type + ")");
        }
        // is this correct ..?
        pushReduction(if_case_reduce);
      }

      branch_tree.popBranch(); // pop branch for else case

      // check all child validity
      Branch curr_branch = branch_tree.getCurrBranch();
      Debug.assertion(curr_branch != null, "curr_branch should be valid");

      if (curr_branch.isAllChildInvalid()) {
        curr_branch.setValid(false);
      }

    }
      break;
    default:
      throw new CompileException("Invalid Child Case(" + num_child + ")");
    }

    return null;
  }

  @Override
  public Object visit(ASTIfCaseExpr node, Object data) throws CompileException {

    Branch branch = branch_tree.getCurrBranch();

    if (branch != null && !branch.isValid()) {
      // Debug.stop();
      node.setAliveCtrlFlow(false);
    }

    branch_tree.popBranch();

    return null;
  }

  @Override
  public Object visit(ASTIfCondExpr node, Object data) throws CompileException {

    Reduction reduce = popReduction();
    Debug.assertion(reduce != null, "Reductin should not be invalid");
    Debug.assertion(reduce.isType(), "Reductin should not be Type");

    AbsType red_type = ((AbsType) reduce);
    Debug.assertion(red_type.isName(TPrimitiveClass.NAME_BOOL), "Reduction should be boolean type");

    branch_tree.pushBranch(new Branch(node));

    return null;
  }

  @Override
  public Object visit(ASTElseExpr node, Object data) throws CompileException {

    branch_tree.pushBranch(new Branch(node));

    return null;
  }

  @Override
  public Object visit(ASTForInit node, Object data) throws CompileException {

    if (node.jjtGetNumChildren() == 0) {
      // do nothing
    } else if (node.jjtGetNumChildren() == 1) {
      Reduction reduce = popReduction();
      Debug.assertion(reduce != null, "Reductin should not be invalid");
    } else {
      throw new CompileException("This case is not considered");
    }

    branch_tree.pushBranch(new Branch(node));

    return null;
  }

  @Override
  public Object visit(ASTForBody node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 1, "ForBody should have one child");

    Reduction reduce = popReduction();
    Debug.assertion(reduce != null, "reduction should not be invalid");
    Debug.assertion(reduce.isControl() || reduce.isType(), "reduction should be container or control(" + reduce + ")");
    dump_reduction_stack();

    return null;
  }

  @Override
  public Object visit(ASTForAction node, Object data) throws CompileException {

    if (node.jjtGetNumChildren() == 0) {
      // do nothing
    } else if (node.jjtGetNumChildren() == 1) {
      Reduction reduce = popReduction();
      Debug.assertion(reduce != null, "Reductin should not be invalid");
    } else {
      throw new CompileException("This case is not considered");
    }

    return null;
  }

  @Override
  public Object visit(ASTForCondition node, Object data) throws CompileException {

    if (node.jjtGetNumChildren() == 0) {
      // do nothing
    } else if (node.jjtGetNumChildren() == 1) { // it has for condition
      Reduction reduce = popReduction();
      Debug.assertion(reduce != null, "Reductin should not be invalid");
    } else {
      throw new CompileException("This case is not considered");
    }

    if (node.hasDefinitionRefernce()) { // this loop does not reduce instance
      pushReduction(new Control(Control.FORM_LOOP));
    } else { // this loop reduces list instance
      AbsType stream_type = (AbsType) cpLoader.findClassFull(CompilerLoader.LANGSTREAM_CLASS_NAME);
      Debug.assertion(stream_type != null, "stream_type should not be invalid");

      pushReduction(stream_type); // reduced as a object
    }

    branch_tree.popBranch();

    return null;
  }

  @Override
  public Object visit(ASTMatchExpr node, Object data) throws CompileException {

    int num_child = node.jjtGetNumChildren();

    boolean is_def_ref = node.hasDefinitionRefernce();

    LangUnitNode match_case_node = null;
    LangUnitNode match_case_head_node = null;
    boolean has_default_case = false;

    Reduction reduce = null;
    AbsType case_type = null;

    for (int i = 1; i < num_child; i++) {
      match_case_node = (LangUnitNode) node.jjtGetChild(i);
      Debug.assertion(match_case_node != null, "match_case_node should not be invalid");
      Debug.assertion(match_case_node.isNodeId(JJTMATCHCASEEXPR), "match_case_node should be match case expression");

      match_case_head_node = (LangUnitNode) match_case_node.jjtGetChild(0);
      Debug.assertion(match_case_head_node != null, "match_case_head_node should not be invalid");
      Debug.assertion(match_case_head_node.isNodeId(JJTMATCHCASEHEADEXPR),
          "match_case_head_node should be match case head expr");

      Token t = match_case_head_node.getAstToken();

      if (t.kind == ParserConstants.DFLT) {
        has_default_case = true;
      }

      reduce = popReduction(); // case expression reduction

      if (is_def_ref) {
        // it does not perform case element reduction checking
      } else {
        Debug.assertion(reduce.isType(), "Match Case poppped reduction should be type");

        if (case_type == null) {
          case_type = (AbsType) reduce;
        } else { // ISSUE: checking is done from lower child to higher child
          if (!cpLoader.isCompatibleClass(case_type, (AbsType) reduce)
              && !cpLoader.isConvertibleClass(case_type, (AbsType) reduce)) {
            throw new CompileException("match case reduction(" + reduce + ") is conflicted", match_case_node);
          }
        }
      }
    }

    if (is_def_ref) {
      pushReduction(new Control(Control.FORM_BRANCH));
    } else {
      Debug.assertion(case_type != null, "Invalid Match Case reduction type");
      pushReduction(case_type);
    }

    if (has_default_case) {
      Branch curr_branch = branch_tree.getCurrBranch();
      Debug.assertion(curr_branch != null, "curr_branch should be valid");

      if (curr_branch.isAllChildInvalid()) {
        curr_branch.setValid(false);
      }
    }

    return null;
  }

  @Override
  public Object visit(ASTMatchHeadExpr node, Object data) throws CompileException {

    Reduction reduce = popReduction();
    Debug.assertion(reduce != null, "Match Condition Reduction should not be invalid");

    return null;
  }

  @Override
  public Object visit(ASTMatchCaseHeadExpr node, Object data) throws CompileException {

    Token t = node.getAstToken();

    switch (t.kind) {
    case ParserConstants.CASE: {
      Reduction reduce = popReduction();
      Debug.assertion(reduce != null, "Match Condition Reduction should not be invalid");
    }
      break;
    default:
      // do nothing
    }

    LangUnitNode matchcase_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(matchcase_node != null, "matchcase_node should be valid");

    if (matchcase_node.getChildIdxWithId(JJTMATCHCASEBODYEXPR, 0) != -1) {
      branch_tree.pushBranch(new Branch(node));
    }

    return null;
  }

  @Override
  public Object visit(ASTMatchCaseBodyExpr node, Object data) throws CompileException {

    branch_tree.popBranch();

    return null;
  }

  @Override
  public Object visit(ASTExplicitCast node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 2, "Cast node should have 2 child");

    Container type_cont = node.getContainer();
    Debug.assertion(type_cont != null, "Cast Node should have Type Container");
    Debug.assertion(type_cont.isTypeInitialized(), "Cast Node container should have type");
    AbsType cast_type = type_cont.getType();

    Reduction reduce = (Reduction) popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
    AbsType opstack_type = (AbsType) reduce;

    // throw new InterpreterException("Stop Type
    // Casting("+opstack_type.getMthdDscStr()+")->("+cast_type.getMthdDscStr()+")");

    if (!cpLoader.isCompatibleClass(opstack_type, cast_type)
        && !cpLoader.isExplicitlyCastibleClass(opstack_type, cast_type)
        && !cpLoader.isConvertibleClass(opstack_type, cast_type)) {
      throw new CompileException(
          "(" + opstack_type.getMthdDscStr() + ") cannot be casted to (" + cast_type.getMthdDscStr() + ")");
    }

    pushReduction(cast_type);

    return null;
  }

  @Override
  public Object visit(ASTLoopHdrExpr node, Object data) throws CompileException {

    TContext context = node.getBindContext();
    Debug.assertion(context != null, "Context should not be null");
    Debug.assertion(context.isForm(AbsType.FORM_STREAM),
        "Context Type should be stream, but " + context.getFormString(context.getForm()));

    TContext upper_context = (TContext) context.getOwnerType();
    Debug.assertion(upper_context != null, "Upper Context should not be null");
    context.initChildVarIdx(upper_context.getNextChidVarIdx());

    this.pushContext(context);
    dump_context_stack();

    return null;
  }

  @Override
  public Object visit(ASTLoopExpr node, Object data) throws CompileException {

    dump_context_stack();
    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");

    return null;
  }

}
