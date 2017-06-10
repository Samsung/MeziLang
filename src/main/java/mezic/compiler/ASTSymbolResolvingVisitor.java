package mezic.compiler;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mezic.compiler.type.AbsClassType;
import mezic.compiler.type.AbsFuncType;
import mezic.compiler.type.AbsType;
import mezic.compiler.type.AbsTypeList;
import mezic.compiler.type.FuncSignatureDesc;
import mezic.compiler.type.OpObject;
import mezic.compiler.type.TContext;
import mezic.compiler.type.TContextClass;
import mezic.compiler.type.TContextClosure;
import mezic.compiler.type.TContextFunc;
import mezic.compiler.type.TContextTU;
import mezic.compiler.type.TMapType;
import mezic.compiler.type.TMethodHandle;
import mezic.compiler.type.TraverseStackNode;
import mezic.parser.ASTAccess;
import mezic.parser.ASTApplyType;
import mezic.parser.ASTAssignment;
import mezic.parser.ASTCatch;
import mezic.parser.ASTCatchExceptionList;
import mezic.parser.ASTExplicitCast;
import mezic.parser.ASTFunctionDef;
import mezic.parser.ASTFunctionDefHdr;
import mezic.parser.ASTFunctionDefSignature;
import mezic.parser.ASTFunctionType;
import mezic.parser.ASTImportClass;
import mezic.parser.ASTLoopExpr;
import mezic.parser.ASTLoopHdrExpr;
import mezic.parser.ASTMetaExtendsData;
import mezic.parser.ASTMetaImplementsData;
import mezic.parser.ASTMetaThrowsData;
import mezic.parser.ASTNamedType;
import mezic.parser.ASTParameter;
import mezic.parser.ASTStream;
import mezic.parser.ASTStreamHeader;
import mezic.parser.ASTStreamSignature;
import mezic.parser.ASTStreamType;
import mezic.parser.ASTSubAccess;
import mezic.parser.ASTSubReference;
import mezic.parser.ASTSymbolName;
import mezic.parser.ASTTranslationUnit;
import mezic.parser.ASTTranslationUnitHeader;
import mezic.parser.ASTType;
import mezic.parser.LangUnitNode;
import mezic.parser.ParseException;
import mezic.parser.ParserConstants;
import mezic.parser.Token;

/*
 * AST Context Building Action
 * 1. Build Context Dependency Tree
 * 2. register variables( class member, function parameter/local variable, list variable) to the context
 * 3. set container for top reference variable name node
 */

public class ASTSymbolResolvingVisitor extends ASTTraverseVisitor {

  private static final long serialVersionUID = -6361328601185865672L;

  private static final Logger LOG = LoggerFactory.getLogger(ASTSymbolResolvingVisitor.class);

  public ASTSymbolResolvingVisitor(CompilerLoader loader) {
    super(loader);
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
  public Object visit(ASTTranslationUnit node, Object data) throws CompileException {

    dump_context_stack();
    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");
    Debug.assertion(context.isForm(AbsType.FORM_TU), "Context Type should be Translation Unit");

    return null;
  }

  @Override
  public Object visit(ASTStreamHeader node, Object data) throws CompileException {

    TContext stream_context = node.getBindContext();
    Debug.assertion(stream_context != null, "Context should not be null");

    switch (node.getStreamForm()) {
    case LangUnitNode.STREAMFORM_DFLT:
      Debug.assertion(stream_context.isForm(AbsType.FORM_STREAM),
          "Context Type should be Stream, but " + stream_context.getFormString(stream_context.getForm()));
      // stream_context.initChildVarIdx(upper_context.getNextChidVarIdx());
      break;

    case LangUnitNode.STREAMFORM_CLASS:
      Debug.assertion(stream_context.isForm(AbsType.FORM_CLASS),
          "Context Type should be Class, but " + stream_context.getFormString(stream_context.getForm()));
      break;
    default:
      throw new CompileException("Invalid Stream Type(" + node.getStreamForm() + ")");
    }

    this.pushContext(stream_context);
    dump_context_stack();

    return null;
  }

  @Override
  public Object visit(ASTStreamSignature node, Object data) throws CompileException {

    LangUnitNode stream_hdr = node.getChildren(0);
    Debug.assertion(stream_hdr.isNodeId(JJTSTREAMHEADER),
        "first child should be Stream Type, but(" + stream_hdr.getNodeName() + ")");

    switch (stream_hdr.getStreamForm()) {

    case LangUnitNode.STREAMFORM_CLASS: {
      TContextClass class_context = (TContextClass) this.getTopContext();
      Debug.assertion(class_context != null, "top context should not be null");
      Debug.assertion(class_context.isForm(AbsType.FORM_CLASS), "top context should be class");

      AbsFuncType[] constructors = class_context.getLocalConstructors();

      if (constructors == null) {
        // if it does not have any constructor, add a default constructor
        TContextFunc dflt_constructor = new TContextFunc(class_context, "<init>", new OpObject(cpLoader), false,
            cpLoader);
        dflt_constructor.initChildVarIdx(0);

        dflt_constructor.setIsConstuctor(true);

        // register "this" variable to default constructor
        LOG.debug("register this variable to default constructor");
        Container this_cont = new Container(TContext.VAR_THIS_NAME, Container.FORM_FUNSTACK_VAR, true, false);
        this_cont.initializeType(class_context);
        this_cont.setAssigned(true);
        dflt_constructor.registerLocalChildVar(this_cont);
        this_cont.bindVarIdx();

        // dflt_constructor will have Method Descriptor as '()V'
        class_context.setGenDfltConstructor(true);
      }
    }
      break;

    default:
      // do nothing

    }

    return null;
  }

  @Override
  public Object visit(ASTMetaExtendsData node, Object data) throws CompileException {

    dump_reduction_stack();

    Reduction reduce = (Reduction) popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);

    AbsType type = (AbsType) reduce;

    Debug.assertion(type.isForm(AbsType.FORM_CLASS), "extends type should be class");

    TContextClass class_context = (TContextClass) this.getTopContext();
    Debug.assertion(class_context != null, "top context should not be null");
    Debug.assertion(class_context.isForm(AbsType.FORM_CLASS), "top context should be class");

    class_context.setSuperClass((AbsClassType) type);

    return null;
  }

  @Override
  public Object visit(ASTMetaImplementsData node, Object data) throws CompileException {

    dump_reduction_stack();

    TContextClass class_context = (TContextClass) this.getTopContext();
    Debug.assertion(class_context != null, "top context should not be null");
    Debug.assertion(class_context.isForm(AbsType.FORM_CLASS), "top context should be class");

    int num_interfaces = node.jjtGetNumChildren();
    Reduction reduce = null;
    AbsType type = null;

    for (int i = 0; i < num_interfaces; i++) {
      reduce = (Reduction) popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
      type = (AbsType) reduce;
      Debug.assertion(type.isForm(AbsType.FORM_CLASS), "extends type should be class");

      class_context.addInterface((AbsClassType) type);
    }

    return null;
  }

  public void constructParamTypeDsc(ASTFunctionDefSignature node, TContextFunc func_ctx) throws CompileException {
    // String mthdDscStr = "";
    AbsType type = null;

    int para_list_idx = node.getChildIdxWithId(JJTPARAMETERLIST, 0);

    if (para_list_idx != -1) {
      LangUnitNode para_list_node = node.getChildren(para_list_idx);
      Debug.assertion(para_list_node.isNodeId(JJTPARAMETERLIST),
          "para_list should be parameter list, but " + para_list_node);

      int num_child = para_list_node.jjtGetNumChildren();
      LangUnitNode para_node = null;
      LangUnitNode symbol_node = null;
      Container symbol_cont = null;
      for (int i = 0; i < num_child; i++) {
        para_node = para_list_node.getChildren(i);
        Debug.assertion(para_node.isNodeId(JJTPARAMETER), "para should be parameter, but " + para_node);

        symbol_node = para_node.getChildren(0);
        Debug.assertion(symbol_node.isNodeId(JJTSYMBOLNAME), "var should be Name, but " + symbol_node);

        symbol_cont = symbol_node.getContainer();
        type = symbol_cont.getType();
        Debug.assertion(type != null, "Parameter type should not be null");

        func_ctx.getFuncSignatureDesc().appendTailPrameterType(type);
      }

    }

  }

  @Override
  public Object visit(ASTStream node, Object data) throws CompileException {
    dump_context_stack();
    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");

    return null;
  }

  @Override
  public Object visit(ASTCatchExceptionList node, Object data) throws CompileException {

    TContext catch_context = node.getBindContext();
    Debug.assertion(catch_context != null, "Context should not be null");
    Debug.assertion(catch_context.isForm(AbsType.FORM_STREAM),
        "Context Type should be Stream, but " + catch_context.getFormString(catch_context.getForm()));

    TContext upperstream_ctx = (TContext) catch_context.getOwnerType();
    Debug.assertion(upperstream_ctx != null, "Stream Context should be valid");
    Debug.assertion(upperstream_ctx.isForm(TContext.FORM_STREAM), "upperstream_ctx should be Stream Context");

    AbsTypeList handling_excp_list = upperstream_ctx.getHandledExceptionList();
    AbsTypeList excp_type_list = new AbsTypeList();

    int num_exception = node.jjtGetNumChildren();
    AbsType excp_type = null;

    for (int i = 0; i < num_exception; i++) {

      excp_type = (AbsType) popReduction();
      Debug.assertion(excp_type != null, "excp_type should be valid");
      Debug.assertion(excp_type.isType(), "excp_type should be type");

      if (handling_excp_list.has(excp_type)) {
        throw new CompileException("Exception(" + excp_type + ") is already handled");
      }

      handling_excp_list.add(excp_type);
      excp_type_list.add(excp_type);

      LOG.debug("handling_excp_list({})", handling_excp_list);
    }

    LangUnitNode listcatch_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(listcatch_node != null, "listcatch_node should be valid");
    Debug.assertion(listcatch_node.isNodeId(JJTCATCH), "listcatch_node should be listcatch node");

    listcatch_node.setCatchExceptionList(excp_type_list);

    this.pushContext(catch_context);
    dump_context_stack();

    return null;
  }

  @Override
  public Object visit(ASTCatch node, Object data) throws CompileException {

    dump_context_stack();
    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");

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
      throw new CompileException("Invalid Function Form(" + node.getFuncForm() + ")");
    }

    if (((TContextFunc) func_ctx).is_closure()) { // push closure context

      TContext closure_context = (TContext) func_ctx.getOwnerType();
      Debug.assertion(closure_context != null, "Context should not be null");
      Debug.assertion(closure_context instanceof TContextClosure, "popped context should be TContextClosure");

      // static closure does not have closure context member
      if (!((TContextFunc) func_ctx).is_static()) {
        registerClosureMember(closure_context, node.isAppliedFunc());
      }

      Container dfltctx_cont = func_ctx.getLocalChildVariable(TContext.VAR_CLOSURE_NAME);
      Debug.assertion(dfltctx_cont != null, "dfltctx_cont should be valid");
      dfltctx_cont.bindVarIdx();

      pushContext(closure_context);
    } else {
      if (!((TContextFunc) func_ctx).is_static()) {
        Container this_cont = func_ctx.getLocalChildVariable(TContext.VAR_THIS_NAME);
        Debug.assertion(this_cont != null, "this_cont should be valid");
        this_cont.bindVarIdx();

        AbsType class_type = func_ctx.getOwnerType();
        Debug.assertion(class_type != null, "class_type should be valid");
        Debug.assertion(class_type instanceof AbsClassType, "class_type should be instance of AbsClassType");

        AbsClassType super_class = ((AbsClassType) class_type).getSuperClass();
        Debug.assertion(super_class != null, "super_class should be valid");
        Debug.assertion(super_class instanceof AbsClassType, "super_class should be instance of AbsClassType");

        Container super_cont = func_ctx.getLocalChildVariable(TContext.VAR_SUPER_NAME);
        Debug.assertion(super_cont != null, "super_cont should be valid");
        super_cont.initializeType((AbsType) super_class);
        super_cont.setContextVarIdx(this_cont.getContextVarIdx());
      }
    }

    this.pushContext(func_ctx);
    dump_context_stack();

    return null;
  }

  private void registerClosureMember(TContext closure_ctx, boolean is_applied_func) throws CompileException {
    TContext closureowner_ctx = (TContext) closure_ctx.getOwnerType();
    Debug.assertion(closureowner_ctx != null, "curr_ctx should be valid");

    Container var_cont = null;

    TContext closureowner_func_ctx = closureowner_ctx.getClosestAncestor(TContext.FORM_FUNC);
    Debug.assertion(closureowner_func_ctx != null, "closureowner_func_ctx should be valid");

    if (!((TContextFunc) closureowner_func_ctx).is_static()) {
      // find 'this' var
      var_cont = closureowner_ctx.getChildVariable(TContext.VAR_THIS_NAME);
      Debug.assertion(var_cont != null, "this_cont should be vallid");

      Container tgt_cont = new Container(var_cont.getName(), Container.FORM_OBJMEMBER_VAR, false, false);
      tgt_cont.initOwnerContainer(closure_ctx.getTypeContainer());
      tgt_cont.initClosureOrgFuncvarContainer(var_cont);

      closure_ctx.registerLocalChildVar(tgt_cont);
      // tgt_cont.initializeType(var_cont.getType());
      // tgt_cont.bindVarIdx();
    }

    // In case of 'applied func', 'var_cont.isAssigned()' is always true,
    // because this apply symbol resolving step is done in sub symbol resolving
    if (is_applied_func) {
      return;
    }

    // closureowner_ctx can be code strem block
    // register all local variable from function to current code stream block
    // fn foo( ) {
    // a = 0 <- this variable also should be registered.
    // { <- current closureowner_ctx
    // b = 0
    // fn(c:int, d:int) := c + d
    // }
    // }
    LinkedList<TContext> ctx_chain = closureowner_ctx.getClosestAncestorContextChain(TContext.FORM_FUNC);
    Debug.assertion(ctx_chain != null, "ctx_chain should be valid");

    int size_ctx = ctx_chain.size();
    int size_var = 0;
    LOG.debug("register Closure Member Variable ({})", closure_ctx.getName());

    TContext curr_ctx = null;
    LinkedList<Container> curr_ctx_childvar_list = null;
    for (int i = 0; i < size_ctx; i++) {
      curr_ctx = ctx_chain.get(i);
      Debug.assertion(curr_ctx != null, "curr_ctx should be valid");

      curr_ctx_childvar_list = curr_ctx.get_childvar_list();
      Debug.assertion(curr_ctx_childvar_list != null, "curr_ctx_childvar_list should be valid");

      size_var = curr_ctx_childvar_list.size();
      for (int j = 0; j < size_var; j++) {
        var_cont = curr_ctx_childvar_list.get(j);
        Debug.assertion(var_cont != null, "var_cont should be valid");
        Debug.assertion(var_cont.isForm(Container.FORM_FUNSTACK_VAR),
            "var_cont should be stack variable(" + var_cont + ")");

        if (var_cont.isAssigned() && !var_cont.isName(TContext.VAR_THIS_NAME)) {
          Container tgt_cont = new Container(var_cont.getName(), Container.FORM_OBJMEMBER_VAR, false, false);
          tgt_cont.initOwnerContainer(closure_ctx.getTypeContainer());
          tgt_cont.initClosureOrgFuncvarContainer(var_cont);

          closure_ctx.registerLocalChildVar(tgt_cont);

          // CAUTION!! type init & binding VarIdx will be done in
          // SubSymbolResolve - updateClosureMember
          // tgt_cont.initializeType(var_cont.getType());
          // tgt_cont.bindVarIdx();
        }
      }
    }

  }

  /*
  private void registerApplyClosureMember(TContext closure_ctx) throws CompileException {
    TContext closureowner_ctx = (TContext) closure_ctx.getOwnerType();
    Debug.assertion(closureowner_ctx != null, "curr_ctx should be valid");

    Container var_cont = null;

    var_cont = closureowner_ctx.getChildVariable(TContext.VAR_THIS_NAME);
    Debug.assertion(var_cont != null, "this_cont should be vallid");
    Debug.assertion(var_cont.isForm(Container.FORM_FUNSTACK_VAR),
        "var_cont should be stack variable(" + var_cont + ")");

    if (var_cont.isAssigned()) {
      Container tgt_cont = new Container(var_cont.getName(), Container.FORM_OBJMEMBER_VAR, false, false);
      tgt_cont.initOwnerContainer(closure_ctx.getTypeContainer());
      tgt_cont.initClosureOrgFuncvarContainer(var_cont);

      closure_ctx.registerLocalChildVar(tgt_cont);

      // tgt_cont.initializeType(var_cont.getType());
      // tgt_cont.bindVarIdx();
    }

  }
  */

  @Override
  public Object visit(ASTFunctionDefSignature node, Object data) throws CompileException {

    LangUnitNode func_hdr = node.getChildren(0);
    Debug.assertion(func_hdr.isNodeId(JJTFUNCTIONDEFHDR),
        "first child should be Function header, but(" + func_hdr.getNodeName() + ")");

    switch (func_hdr.getFuncForm()) {
    case LangUnitNode.FUNCFORM_MEMBER:
    case LangUnitNode.FUNCFORM_STATIC: {
      TContextFunc func_context = (TContextFunc) getTopContext();
      Debug.assertion(func_context != null, "top context should be context function");

      AbsType return_type = null;
      if (node.hasReduceTypeNode()) {
        Reduction reduce = (Reduction) popReduction();
        Debug.assertion(reduce != null, "Invalid Popped Reduction");
        Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);

        return_type = (AbsType) reduce;
        // func_context.setReturnType(return_type);
      }

      constructParamTypeDsc(node, func_context);

      if (return_type != null) {
        LOG.debug("initReturnType: {}", return_type);
        func_context.getFuncSignatureDesc().initReturnType(return_type);
      }

      LOG.debug("Function signature is completed: {}", func_context);

    }
      break;

    default:
      throw new CompileException("Invalid Function Form(" + node.getFuncForm() + ")");
    }

    return null;
  }

  @Override
  public Object visit(ASTMetaThrowsData node, Object data) throws CompileException {

    int num_type = node.jjtGetNumChildren();

    AbsType excp_type = null;

    AbsTypeList excp_type_list = new AbsTypeList();

    for (int i = 0; i < num_type; i++) {
      excp_type = (AbsType) popReduction();
      Debug.assertion(excp_type != null, "excp_type should be valid");
      Debug.assertion(excp_type.isType(), "excp_type should be type");

      excp_type_list.add(excp_type);
    }

    TContextFunc func_ctx = (TContextFunc) getTopContext().getClosestAncestor(AbsType.FORM_FUNC);
    Debug.assertion(func_ctx != null, "func meta throw should be used in function context");

    func_ctx.setThrowsList(excp_type_list);

    return null;
  }

  @Override
  public Object visit(ASTFunctionDef node, Object data) throws CompileException {
    dump_context_stack();
    TContext func_ctx = popContext();
    Debug.assertion(func_ctx != null, "Context should not be null");

    if (((TContextFunc) func_ctx).is_closure()) { // pop closure context
      TContext closure_context = popContext();
      Debug.assertion(closure_context != null, "Context should not be null");
      Debug.assertion(closure_context instanceof TContextClosure, "popped context should be TContextClosure");
    }

    return null;
  }

  @Override
  public Object visit(ASTParameter node, Object data) throws CompileException {

    LangUnitNode symbol_node = (LangUnitNode) node.jjtGetChild(0);
    Debug.assertion(symbol_node != null, "first child should not be null");
    Debug.assertion(symbol_node.isNodeId(JJTSYMBOLNAME), "first child should be symbol node");

    AbsType type = null;

    int type_specifier_child_idx = symbol_node.getChildIdxWithId(JJTTYPEDEFINE, 0);

    if (type_specifier_child_idx == -1) {
      type = (AbsType) cpLoader.findClassFull(CompilerLoader.APPLY_CLASS_NAME);
      Debug.assertion(type != null, "type should be valid");
    } else {
      Reduction reduce = (Reduction) popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
      type = (AbsType) reduce;
    }

    Container container = symbol_node.getContainer();
    // parameter Container is attached to symbol node in ContextBuildVisitor
    Debug.assertion(container != null, "Symbol node container should not be null");
    container.initializeType(type);
    container.bindVarIdx();

    return null;
  }

  @Override
  public Object visit(ASTAccess node, Object data) throws CompileException {

    LangUnitNode access_child_node = (LangUnitNode) node.jjtGetChild(0);
    Debug.assertion(access_child_node != null, "first child should not be null");

    if (access_child_node.isNodeId(JJTSYMBOLNAME)) {
      Token t = access_child_node.getAstToken();

      // Typedefine node index(if it does not have, it will be '-1')
      TContext curr_context = getTopContext();
      int type_specifier_child_idx = access_child_node.getChildIdxWithId(JJTTYPEDEFINE, 0);
      boolean is_constant = (access_child_node.getVarType() == LangUnitNode.VAR_T_CONST);

      // search variable from current context
      Container symbol_cont = getTopContext().getChildVariable(t.image);

      if (symbol_cont != null) {
        // here, access symbol_cont is valid
        if (symbol_cont.isForm(Container.FORM_OBJMEMBER_VAR)) {
          TContext cont_context = symbol_cont.getContext();
          if (cont_context.isLinealAscendent(getTopContext())) {
            // access_child_node.setIsDfltContextMember(true);
            LOG.debug("This Case");

            TContextFunc func_ctx = (TContextFunc) getTopContext().getClosestAncestor(AbsType.FORM_FUNC);

            if (func_ctx != null) {
              if (!func_ctx.is_static()) {
                add_dflt_ctx_node(node, func_ctx);
                return null;
              } else {
                throw new CompileException("static function cannot access to member", node);
              }
            }
          }
        } else {
          // do nothing...
        }
      } else { // it is not variable, but package or class
        // constant assign type initialization
        if (node.isAssignTgt()) {

          if (!isValidASTConextForStackVarRegister(node)) {
            throw new CompileException("Syntax error(variable cannot be declared in branch case one expression)", node);
          }

          // register as a stack variable
          symbol_cont = new Container(t.image, Container.FORM_FUNSTACK_VAR, is_constant, false);
          curr_context.registerLocalChildVar(symbol_cont);

          // type will be initialized in SubSymbol Resolving Assignment
        } else {
          LangUnitNode ref_node = (LangUnitNode) node.jjtGetParent();

          if (isNextSubRefChildNodeid(ref_node, JJTINVOKE)) {
            if (type_specifier_child_idx != -1) {
              throw new CompileException("Function call should not have type specifier", node);
            }
            // do nothing (Function call resolving will be done in
            // SubSymbolResolvingVisitor)
            return null;
          }

          AbsType reducible_type = cpLoader.findPackage(t.image);

          if (reducible_type == null) {
            TContext tuContext = getBottomContext(); // actually tu context has
                                                     // parent, but app stack
                                                     // bottom may be tu
            Debug.assertion(tuContext != null, "Bottom context should not be invalid");
            Debug.assertion(tuContext.isForm(TContext.FORM_TU), "Bottom context should be Translation Unit");

            // here, findClassFull should not be used, because, 't.image' can be
            // simple class package
            reducible_type = (AbsType) cpLoader.findClass(t.image, (TContextTU) tuContext);
            if (reducible_type == null) {
              getTopContext().dump_localchild_varialbe(" -");
              throw new CompileException("'" + t.image + "' is not defined", node);
            }
          }

          LOG.debug("Found Reducible Type {}", reducible_type);
          symbol_cont = new Container(reducible_type.getName(), Container.FORM_TYPE, true, false);
          symbol_cont.initializeType(reducible_type);

        }

      }

      if (type_specifier_child_idx != -1) { // if it has Typedefine node,
                                          // initialize type..
        // class member case reach here
        LOG.debug("Has Type");
        Reduction reduce = (Reduction) popReduction();
        Debug.assertion(reduce != null, "Invalid Popped Reduction");
        Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
        AbsType type = (AbsType) reduce;
        symbol_cont.initializeType(type);
        // symbol_cont.bindVarIdx(); // moved to SubSymbolResolving -
        // ASTAccess()
      }

      // Debug.assertion(symbol_cont.isTypeInitialized(), "variable type should
      // be initialized");
      access_child_node.setContainer(symbol_cont); // another node can share
                                                   // this container

    } else {
      // do nothing for other access child node types
    }

    return null;
  }

  private void add_dflt_ctx_node(LangUnitNode access_node, TContextFunc func_ctx) throws CompileException {

    Token access_first_token = access_node.jjtGetFirstToken();

    Debug.assertion(access_node.isNodeId(JJTACCESS), "access_node should be access_node");

    LangUnitNode ref_node = access_node.getClosestParent(JJTREFERENCE);
    Debug.assertion(ref_node != null, "List should have reference node");

    /*
     * make sub ref / access node for moving original symbol node
     *
     * subref_node / subaccess_node
     *
     */

    LangUnitNode subref_node = (LangUnitNode) new ASTSubReference(JJTSUBREFERENCE);
    subref_node.jjtSetFirstToken(access_first_token);

    LangUnitNode subaccess_node = (LangUnitNode) new ASTSubAccess(JJTSUBACCESS);

    subaccess_node.jjtSetFirstToken(access_first_token);

    subref_node.jjtAddChild(subaccess_node, 0);
    subaccess_node.jjtSetParent(subref_node);

    LangUnitNode next_subref_node = getNextChildSubRefeNode(ref_node);

    if (next_subref_node == null) {
      Debug.assertion(ref_node.jjtGetNumChildren() == 1, "ref_node child num should be 1");
      ref_node.jjtAddChild(subref_node, 1);
      subref_node.jjtSetParent(ref_node);
    } else {
      if (next_subref_node.jjtGetNumChildren() == 1) {

        LangUnitNode nextsubref_child_node = next_subref_node.getChildren(0);
        Debug.assertion(nextsubref_child_node != null, "nextsubref_child_node should be valid");

        next_subref_node.jjtAddChild(nextsubref_child_node, 1);

        next_subref_node.jjtSetChild(0, subref_node);
        subref_node.jjtSetParent(next_subref_node);

      } else {
        throw new CompileException("Invalid next_subref_node child num" + next_subref_node.jjtGetNumChildren());
      }
    }

    LangUnitNode org_symbol_node = (LangUnitNode) access_node.jjtGetChild(0);
    Debug.assertion(org_symbol_node != null, "first child should not be null");
    Debug.assertion(org_symbol_node.isNodeId(JJTSYMBOLNAME), "symbol_node should be symbol_node");

    subaccess_node.jjtAddChild(org_symbol_node, 0);
    org_symbol_node.jjtSetParent(subaccess_node);

    LangUnitNode this_symbol_node = (LangUnitNode) new ASTSymbolName(JJTSYMBOLNAME);

    Token dflt_ctx_token = null;
    Container dflt_ctx_symbol_cont = null;

    if (func_ctx.is_closure()) {
      dflt_ctx_token = new Token(ParserConstants.IDENTIFIER, TContext.VAR_CLOSURE_NAME);
      dflt_ctx_symbol_cont = getTopContext().getChildVariable(TContext.VAR_CLOSURE_NAME);
      Debug.assertion(dflt_ctx_symbol_cont != null, "this_symbol_cont should not be null");

    } else {
      dflt_ctx_token = new Token(ParserConstants.THIS, TContext.VAR_THIS_NAME);
      dflt_ctx_symbol_cont = getTopContext().getChildVariable(TContext.VAR_THIS_NAME);
      Debug.assertion(dflt_ctx_symbol_cont != null, "this_symbol_cont should not be null");
    }

    dflt_ctx_token.beginColumn = access_first_token.beginColumn;
    dflt_ctx_token.beginLine = access_first_token.beginLine;
    dflt_ctx_token.endColumn = access_first_token.endColumn;
    dflt_ctx_token.endLine = access_first_token.endLine;

    try {
      this_symbol_node.initAstToken(dflt_ctx_token);
    } catch (ParseException e) {
      CompileException ie = new CompileException("Parser exception");
      ie.setTargetException(e);
      throw ie;
    }

    access_node.jjtSetChild(0, this_symbol_node);
    this_symbol_node.jjtSetParent(access_node);

    this_symbol_node.setContainer(dflt_ctx_symbol_cont);

    if (access_node.isAssignTgt()) {
      access_node.setAssignTgt(false);
      subaccess_node.setAssignTgt(true);
    }

  }

  @Override
  public Object visit(ASTNamedType node, Object data) throws CompileException {

    String type_name = node.getNamedType();

    TContext tuContext = getBottomContext(); // actually tu context has parent,
                                             // but app stack bottom may be tu
    Debug.assertion(tuContext != null, "Bottom context should not be invalid");
    Debug.assertion(tuContext.isForm(TContext.FORM_TU), "Bottom context should be Translation Unit");

    AbsType named_type = (AbsType) cpLoader.findClass(type_name, (TContextTU) tuContext);

    if (named_type == null) {
      throw new CompileException("Failed to find(" + type_name + ")", node);
    }

    pushReduction(named_type);
    return null;
  }

  @Override
  public Object visit(ASTImportClass node, Object data) throws CompileException {

    String type_name = node.getNamedType();

    TContext tuContext = getBottomContext(); // actually tu context has parent,
                                             // but app stack bottom may be tu
    Debug.assertion(tuContext != null, "Bottom context should not be invalid");
    Debug.assertion(tuContext.isForm(TContext.FORM_TU), "Bottom context should be Translation Unit");

    if (type_name.endsWith("*")) {
      String pkg_name = type_name.substring(0, type_name.length() - 2);

      AbsType pkg_type = (AbsType) cpLoader.findPackage(pkg_name);

      if (pkg_type == null) {
        throw new CompileException("Failed to find(" + pkg_name + ")", node);
      }

      ((TContextTU) tuContext).addImportPath(pkg_name);

    } else {
      AbsType named_type = (AbsType) cpLoader.findClass(type_name, (TContextTU) tuContext);

      if (named_type == null) {
        throw new CompileException("Failed to find(" + type_name + ")", node);
      }

      String[] tokens = type_name.split(CompilerLoader.COMPILER_PKG_DELIMETER);

      ((TContextTU) tuContext).addImportClass(tokens[tokens.length - 1], named_type);

    }

    return null;
  }

  @Override
  public Object visit(ASTApplyType node, Object data) throws CompileException {

    AbsType apply_type = node.getApplyType();
    Debug.assertion(apply_type != null, "apply_type should be valid, node(" + node + "(" + node.hashCode() + ") )");

    pushReduction(apply_type);
    return null;
  }

  @Override
  public Object visit(ASTStreamType node, Object data) throws CompileException {

    AbsType stream_type = (AbsType) cpLoader.findClassFull(CompilerLoader.LANGSTREAM_CLASS_NAME);
    Debug.assertion(stream_type != null, "stream_type should not be invalid");

    pushReduction(stream_type);

    return null;
  }

  @Override
  public Object visit(ASTFunctionType node, Object data) throws CompileException {

    Reduction reduce = null;
    AbsType reduce_type = null;

    FuncSignatureDesc funcsig = new FuncSignatureDesc(cpLoader);

    if (node.hasReduceTypeNode()) {
      reduce = (Reduction) popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);

      reduce_type = (AbsType) reduce;

      funcsig.initReturnType(reduce_type);
    }

    if (node.jjtGetNumChildren() > 0) {
      LangUnitNode node_ptlist = (LangUnitNode) node.jjtGetChild(0);
      Debug.assertion(node_ptlist != null, "node_ptlist should be valid");

      if (node_ptlist.isNodeId(JJTPARAMETERTYPELIST)) {
        int num_pt = node_ptlist.jjtGetNumChildren();

        AbsType para_type = null;

        for (int i = 0; i < num_pt; i++) {
          reduce = (Reduction) popReduction();
          Debug.assertion(reduce != null, "Invalid Popped Reduction");
          Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);

          para_type = (AbsType) reduce;

          funcsig.pushHeadPrameterType(para_type);
        }
      }
    }

    LOG.debug("Function Signature: {}", funcsig.getMthdDscStr());

    TMethodHandle mh_type = TMethodHandle.getInstance(cpLoader);
    Debug.assertion(mh_type != null, "mh_type should not be invalid");
    mh_type.setFuncSignatureDesc(funcsig);

    pushReduction(mh_type);

    return null;
  }

  @Override
  public Object visit(ASTType node, Object data) throws CompileException {

    int map_dimension = node.getMapDimesion();

    if (map_dimension != 0) {
      Reduction reduce = popReduction();
      Debug.assertion(reduce != null, "popped reduce should be valid");
      Debug.assertion(reduce.isType(), "popped reduce should be type");

      AbsType type = (AbsType) reduce;

      TMapType map_type = cpLoader.findMapType(type, map_dimension);
      Debug.assertion(map_type != null, "map_type reduce should be valid");

      LOG.debug("new map_type: {}({})", map_type, map_type.getMthdDscStr());

      pushReduction(map_type);
    }

    return null;
  }

  @Override
  public Object visit(ASTExplicitCast node, Object data) throws CompileException {

    Reduction reduce = (Reduction) popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isType(), "Invalid Popped Reduction " + reduce);
    AbsType type = (AbsType) reduce;

    Container container = type.getTypeContainer();
    node.setContainer(container);

    return null;
  }

  @Override
  public Object visit(ASTLoopHdrExpr node, Object data) throws CompileException {

    TContext context = node.getBindContext();
    Debug.assertion(context != null, "Context should not be null");
    Debug.assertion(context.isForm(AbsType.FORM_STREAM),
        "Context Type should be Stream, but " + context.getFormString(context.getForm()));

    // TContext upper_context = (TContext)context.getOwnerType();
    // Debug.assertion(upper_context!=null, "Upper Context should not be null");
    // context.initChildVarIdx(upper_context.getNextChidVarIdx());

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

  @Override
  public Object visit(ASTAssignment node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 3,
        "the number of node child should be 3, but " + node.jjtGetNumChildren());

    // assignment target tagging. for assignment target 'access', 'load' will
    // not apply
    LangUnitNode reference_node = node.getChildren(0);
    Debug.assertion(reference_node.isNodeId(JJTREFERENCE), "Assingment Lvalue should be reference");

    LangUnitNode rmost_access_node = findRightMostAccessNode(reference_node);
    Debug.assertion(rmost_access_node != null, "access_node should be valid");

    if (rmost_access_node.isNodeId(JJTACCESS)) {
      LangUnitNode symbol_node = rmost_access_node.getChildren(0);
      Debug.assertion(symbol_node != null, "symbol_node should be valid");

      if (symbol_node.isNodeId(JJTSYMBOLNAME)) {
        Container symbol_cont = symbol_node.getContainer();

        if (symbol_cont != null && symbol_cont.isForm(Container.FORM_FUNSTACK_VAR)) {
        	
          if (symbol_cont.isConstant() && symbol_cont.isAssigned()) {
        	  throw new CompileException("constant (" + symbol_cont +") cannot be re-assigned", node);
          }
        	
          symbol_cont.setAssigned(true);
        }
      }
    }

    return null;
  }
}
