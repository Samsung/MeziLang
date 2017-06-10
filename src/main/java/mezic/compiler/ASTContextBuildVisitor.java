package mezic.compiler;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mezic.compiler.type.AbsClassType;
import mezic.compiler.type.AbsType;
import mezic.compiler.type.ObjNull;
import mezic.compiler.type.OpObject;
import mezic.compiler.type.TContext;
import mezic.compiler.type.TContextClass;
import mezic.compiler.type.TContextClosure;
import mezic.compiler.type.TContextFunc;
import mezic.compiler.type.TContextStream;
import mezic.compiler.type.TContextTU;
import mezic.compiler.type.TPrimitiveClass;
import mezic.parser.ASTAccess;
import mezic.parser.ASTAssignment;
import mezic.parser.ASTCatch;
import mezic.parser.ASTCatchExceptionList;
import mezic.parser.ASTConstant;
import mezic.parser.ASTFunctionDef;
import mezic.parser.ASTFunctionDefHdr;
import mezic.parser.ASTLoopExpr;
import mezic.parser.ASTLoopHdrExpr;
import mezic.parser.ASTOneExprFuncBody;
import mezic.parser.ASTParameter;
import mezic.parser.ASTStream;
import mezic.parser.ASTStreamHeader;
import mezic.parser.ASTSubAccess;
import mezic.parser.ASTTranslationUnit;
import mezic.parser.ASTTranslationUnitHeader;
import mezic.parser.LangUnitNode;
import mezic.parser.ParserConstants;
import mezic.parser.Token;
import mezic.util.TxtUtil;
import mezic.util.TypeUtil;

/*
 * AST Context Building Action
 * 1. Build Context Dependency Tree
 * 2. register variables( class member, function parameter/local variable, stream variable) to the context
 * 3. set container for top reference variable name node
 */

public class ASTContextBuildVisitor extends ASTTraverseVisitor {

  private static final long serialVersionUID = -4121981331287836656L;

  private static final Logger LOG = LoggerFactory.getLogger(ASTContextBuildVisitor.class);

  private String tu_name;

  public ASTContextBuildVisitor(CompilerLoader loader, String tuname) {
    // example tu_name:unit_test.Test
    super(loader);
    this.tu_name = tuname.replace('.', '/');
  }

  @Override
  public Object visit(ASTTranslationUnitHeader node, Object data) throws CompileException {

    LOG.debug("Translation Unit Name: {}", tu_name);

    // it does not have parent context
    TContextTU tu_context = new TContextTU(tu_name, new OpObject(cpLoader), cpLoader);
    pushContext(tu_context);

    cpLoader.register_tu_context(tu_context, tu_name);

    if (Debug.enable_print_dbg) {
      cpLoader.dumpContextPackageTree();
    }
    return null;
  }

  @Override
  public Object visit(ASTTranslationUnit node, Object data) throws CompileException {
    dump_context_stack();

    TContext context = popContext();
    if (context == null || !context.isForm(AbsType.FORM_TU)) {
      throw new CompileException("Invalid Top Context");
    }

    LangUnitNode tuHead = node.getChildren(0);
    Debug.assertion(tuHead != null, "child node should not be null");
    Debug.assertion(tuHead.isNodeId(JJTTRANSLATIONUNITHEADER), "child should be translation unit header");

    tuHead.bindContext(context);

    return null;
  }

  @Override
  public Object visit(ASTStreamHeader node, Object data) throws CompileException {

    Token t = node.getAstToken();

    String stream_name = (t != null ? t.image : "anoymous");

    TContext upper_context = this.getTopContext();
    Debug.assertion(upper_context != null, "upper context should not be null");

    LangUnitNode ref_node = node.getClosestParent(JJTREFERENCE);
    Debug.assertion(ref_node != null, "Stream should have reference node");

    switch (node.getStreamForm()) {
    case LangUnitNode.STREAMFORM_CLASS: {
      TContextTU context_tu = (TContextTU) getBottomContext();
      Debug.assertion(context_tu != null, "Translation Unit context should not be invalid");

      String pkg_name = context_tu.getPackageName();
      String full_class_name = pkg_name + CompilerLoader.COMPILER_PKG_DELIMETER + stream_name;

      TContextClass class_context = new TContextClass(upper_context, full_class_name, new OpObject(cpLoader), cpLoader);
      this.pushContext(class_context);

      LOG.debug("New Class Context is added: {}", class_context);

    }
      break;

    case LangUnitNode.STREAMFORM_DFLT: {
      TContextStream stream_context = new TContextStream(upper_context, stream_name, new OpObject(cpLoader), cpLoader);
      this.pushContext(stream_context);
      LOG.debug("New Stream Context is added: {}", stream_context);

    }
      break;

    default:
      throw new CompileException("Undefined Stream Type(" + node.getStreamForm() + ")");
    }

    return null;
  }

  @Override
  public Object visit(ASTStream node, Object data) throws CompileException {
    dump_context_stack();

    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");

    LangUnitNode stream_sig_node = node.getChildren(0);
    Debug.assertion(stream_sig_node != null, "child node should not be null");
    Debug.assertion(stream_sig_node.isNodeId(JJTSTREAMSIGNATURE), "child should be signature, but " + stream_sig_node);

    LangUnitNode stream_head_node = stream_sig_node.getChildren(0);
    Debug.assertion(stream_head_node != null, "child node should not be null");
    Debug.assertion(stream_head_node.isNodeId(JJTSTREAMHEADER), "grand child should be stream type");

    stream_head_node.bindContext(context);

    return null;
  }

  @Override
  public Object visit(ASTCatchExceptionList node, Object data) throws CompileException {

    TContext upper_context = this.getTopContext();
    Debug.assertion(upper_context != null, "upper context should not be null");

    TContextStream catch_context = new TContextStream(upper_context, "catch_context", new OpObject(cpLoader), cpLoader);

    this.pushContext(catch_context);

    LOG.debug("New Stream Context is added: {}", catch_context);

    // register default 'excp' variable
    Container dflt_excp_cont = new Container(TContext.VAR_EXCEPTION_NAME, Container.FORM_FUNSTACK_VAR, true, false);
    dflt_excp_cont.initializeType((AbsType) cpLoader.getExceptionClass());
    dflt_excp_cont.setAssigned(true);

    catch_context.registerLocalChildVar(dflt_excp_cont);

    return null;
  }

  @Override
  public Object visit(ASTCatch node, Object data) throws CompileException {

    dump_context_stack();

    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");

    LangUnitNode catchexcplist_node = node.getChildren(0);
    Debug.assertion(catchexcplist_node != null, "child node should not be null");
    Debug.assertion(catchexcplist_node.isNodeId(JJTCATCHEXCEPTIONLIST),
        "1st child should be catch exception list node");

    catchexcplist_node.bindContext(context);

    return null;
  }

  @Override
  public Object visit(ASTFunctionDefHdr node, Object data) throws CompileException {

    Token t = node.getAstToken();

    LangUnitNode ref_node = node.getClosestParent(JJTREFERENCE);
    Debug.assertion(ref_node != null, "Function Def should have reference node");

    boolean is_static = false;
    boolean is_closure = false;

    switch (node.getFuncForm()) {
    case LangUnitNode.FUNCFORM_MEMBER:
      is_static = false;
      break;

    case LangUnitNode.FUNCFORM_STATIC:
      is_static = true;
      break;
    default:
      throw new CompileException("Undefined Function Type(" + node.getFuncForm() + ")");
    }

    TContextClass class_context = (TContextClass) getTopContext().getClosestAncestor(AbsType.FORM_CLASS);
    Debug.assertion(class_context != null, "Class context should not be null");

    TContext owner_func_context = getTopContext().getClosestAncestor(AbsType.FORM_FUNC);

    if (owner_func_context != null) {
      // new closure class context is added here
      class_context = createClosureContext(getTopContext(), node); // overwrite
                                                                   // class
                                                                   // context
                                                                   // with
                                                                   // closure
                                                                   // context
      Debug.assertion(class_context != null, "class_context should be valid");

      pushContext(class_context); // add closure context

      is_closure = true;

      // closure can be static
      // is_static = false; // closure is not static function
    }

    String func_name = null;
    if (t != null) {
      if (t.image.startsWith(TContextClass.ANONYFUN_PREFIX)) {
        throw new CompileException(
            "Function Name cannot start with '" + TContextClass.ANONYFUN_PREFIX + "' explicityly", node);
      }
      func_name = t.image;
    } else {
      func_name = class_context.allocAnonyFuncName();
    }

    boolean is_constructor = false;
    if (class_context.getSimpleName().equals(func_name)) {
      if (is_closure) {
        throw new CompileException("Constructor cannot be closure");
      }

      is_constructor = true;
      func_name = "<init>";
    }

    TContextFunc func_context = new TContextFunc(class_context, func_name, new OpObject(cpLoader), is_closure,
        cpLoader);
    this.pushContext(func_context);

    func_context.setIsConstuctor(is_constructor);
    func_context.set_is_static(is_static);

    Container dflt_ctx_cont = null;

    if (is_closure) {
      dflt_ctx_cont = new Container(TContext.VAR_CLOSURE_NAME, Container.FORM_FUNSTACK_VAR, true, false);
      dflt_ctx_cont.initializeType(class_context);
      dflt_ctx_cont.setAssigned(true);
      func_context.registerLocalChildVar(dflt_ctx_cont);
    } else {
      if (!is_static) {
        dflt_ctx_cont = new Container(TContext.VAR_THIS_NAME, Container.FORM_FUNSTACK_VAR, true, false);
        dflt_ctx_cont.initializeType(class_context);
        dflt_ctx_cont.setAssigned(true);
        func_context.registerLocalChildVar(dflt_ctx_cont);

        Container super_cont = new Container(TContext.VAR_SUPER_NAME, Container.FORM_FUNSTACK_VAR, true, false);
        super_cont.setAssigned(true);
        func_context.registerLocalChildVar(super_cont);
      }
    }

    LOG.debug("New Function Context is added: {}", func_context);

    return null;
  }

  private TContextClosure createClosureContext(TContext top_context, ASTFunctionDefHdr node) throws CompileException {
    TContextClass class_context = (TContextClass) top_context.getClosestAncestor(AbsType.FORM_CLASS);
    Debug.assertion(class_context != null, "Class context should not be null");

    // modified !! nested closure is allowed
    // temporal - closure in closure is not allowed !!
    // if( class_context instanceof TContextClosure )
    // throw new InterpreterException("It does not support Closure in
    // Closure("+class_context+")");

    TContext owner_func_context = top_context.getClosestAncestor(AbsType.FORM_FUNC);
    Debug.assertion(owner_func_context != null, "owner_func_context should not be null");

    String closure_name = class_context.getName() + "_"
        + (owner_func_context.getName().equals("<init>") ? "init" : owner_func_context.getName())
        + ((TContextFunc) owner_func_context).allocAnonyFuncName();

    LOG.debug("Adding Closure ({})", closure_name);

    TContextClosure closure_context = new TContextClosure(top_context, closure_name, new OpObject(cpLoader), cpLoader);

    return closure_context;
  }

  @Override
  public Object visit(ASTFunctionDef node, Object data) throws CompileException {

    dump_context_stack();

    TContext func_ctx = popContext();
    Debug.assertion(func_ctx != null, "Context should not be null");
    Debug.assertion(func_ctx instanceof TContextFunc, "popped context should be TContextFunc");

    LangUnitNode func_sig_node = node.getChildren(0);
    Debug.assertion(func_sig_node != null, "child node should not be null");
    Debug.assertion(func_sig_node.isNodeId(JJTFUNCTIONDEFSIGNATURE), "child should be function signature");

    LangUnitNode func_head_node = func_sig_node.getChildren(0);
    Debug.assertion(func_head_node != null, "child node should not be null");
    Debug.assertion(func_head_node.isNodeId(JJTFUNCTIONDEFHDR), "grand child should be function expr type");

    func_head_node.bindContext(func_ctx);

    if (((TContextFunc) func_ctx).is_closure()) {
      TContext closure_context = popContext(); // pop closure context
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

    Token t = symbol_node.getAstToken();
    Container parameter_cont = getTopContext().getLocalChildVariable(t.image);

    // parameter duplication checking
    if (parameter_cont != null) {
      throw new CompileException("Parameter(" + t.image + ") is duplicated", node);
    }

    int type_specifier_child_idx = symbol_node.getChildIdxWithId(JJTTYPEDEFINE, 0);
    // type specification checking
    if (type_specifier_child_idx == -1) {
      // throw new InterpreterException("Type is not defined for parameter
      // "+t.image, node);
      LOG.debug("Parameter does not have type specifier - apply type will be applied");

      TContext func_ctx = getTopContext().getClosestAncestor(AbsType.FORM_FUNC);
      LOG.debug("apply function: {}", func_ctx);
      ((TContextFunc) func_ctx).set_is_apply(true);
    }

    parameter_cont = new Container(t.image, Container.FORM_FUNSTACK_VAR, false, false);
    parameter_cont.setAssigned(true);
    getTopContext().registerLocalChildVar(parameter_cont);

    // Temporal: This Container resolving should be moved to
    // SymbolResolvingVisitor
    symbol_node.setContainer(parameter_cont);
    return null;
  }

  @Override
  public Object visit(ASTOneExprFuncBody node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 1, "One Expression Function Body should have one child");

    TContext func_ctx = getTopContext();
    Debug.assertion(func_ctx != null, "Function Context should be valid");
    Debug.assertion(func_ctx.isForm(AbsType.FORM_FUNC), "Function Context should be Function Form");

    if (((TContextFunc) func_ctx).is_constructor()) {
      throw new CompileException("Constructor can not be one expression function form");
    }

    return null;
  }

  @Override
  public Object visit(ASTAccess node, Object data) throws CompileException {

    LangUnitNode symbol_node = (LangUnitNode) node.jjtGetChild(0);
    Debug.assertion(symbol_node != null, "first child should not be null");

    // constant case is processed in 'ASTConstant'
    if (!symbol_node.isNodeId(JJTSYMBOLNAME)) {
      return null;
    }

    LangUnitNode ref_node = (LangUnitNode) node.jjtGetParent();

    if (isNextSubRefChildNodeid(ref_node, JJTINVOKE)) {
      // if( node.isFunCall() ) {
      /* do nothing - SubSymbolResolvingVisitor will process function call */
      return null;
    } else {
      TContext curr_context = getTopContext();
      Debug.assertion(curr_context != null, "Current context should not be null");

      Token t = symbol_node.getAstToken();
      int type_specifier_child_idx = symbol_node.getChildIdxWithId(JJTTYPEDEFINE, 0);
      boolean is_constant = (symbol_node.getVarType() == LangUnitNode.VAR_T_CONST);
      boolean is_singleton = (symbol_node.getVarType() == LangUnitNode.VAR_T_SINGLETON);

      Container container = null;

      switch (curr_context.getForm()) {
      case AbsType.FORM_CLASS:
        if (type_specifier_child_idx != -1) {
          // if it has type specifier
          // duplication checking
          container = curr_context.getLocalChildVariable(t.image);
          if (container != null) {
            throw new CompileException("Type is duplicately defined for " + t.image, node);
          }

          // register as a class member variable
          // TODO : is_singleton param should be implemented !!
          container = new Container(t.image, Container.FORM_OBJMEMBER_VAR, is_constant, is_singleton);

          TContextClass curr_class_context = (TContextClass) curr_context;
          container.initOwnerContainer(curr_class_context.getTypeContainer());

          curr_context.registerLocalChildVar(container);
        }
        break;

      case AbsType.FORM_FUNC:
      case AbsType.FORM_STREAM:

        if (type_specifier_child_idx != -1) {
          // it has type specifier
          // duplicated stack variable checking on local context
          container = curr_context.getLocalChildVariable(t.image);
          if (container != null) {
            throw new CompileException("Type is duplicately defined for " + t.image, node);
          }

          if (!isValidASTConextForStackVarRegister(node)) {
            throw new CompileException("Syntax error(variable cannot be declared in branch case one expression)", node);
          }

          // register as a stack variable
          container = new Container(t.image, Container.FORM_FUNSTACK_VAR, is_constant, false);
          curr_context.registerLocalChildVar(container);
        } else {
          // no type specifier
          /*
           * // find container on context hierarchy( !! not local context )
           * container = curr_context.getChildVariable(t.image);
           *
           * if( container == null ){ if( !
           * isValidASTConextForStackVarRegister(node) ) { throw new
           * InterpreterException(
           * "Syntax error(variable cannot be declared in branch case one expression)"
           * , node); }
           *
           * // register as a stack variable container = new Container(t.image,
           * Container.FORM_FUNSTACK_VAR, is_constant, false);
           * curr_context.registerLocalChildVar(container); }
           */
        }

        break;

      case AbsType.FORM_TU:
        throw new CompileException("Translation Unit Access is not implemented");
      default:
        throw new CompileException("Undefined Context Type(" + curr_context.getForm() + ")");
      }

      return null;
    }

  }

  @Override
  public Object visit(ASTSubAccess node, Object data) throws CompileException {

    LangUnitNode symbol_node = (LangUnitNode) node.jjtGetChild(0);
    Debug.assertion(symbol_node != null, "first child should not be null");

    // constant case is processed in 'ASTConstant'
    if (!symbol_node.isNodeId(JJTSYMBOLNAME)) {
      return null;
    }

    int type_specifier_child_idx = symbol_node.getChildIdxWithId(JJTTYPEDEFINE, 0);

    if (type_specifier_child_idx != -1) {
      throw new CompileException("member symbol cannot have type specifier");
    }

    return null;
  }

  @Override
  public Object visit(ASTAssignment node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 3,
        "the number of node child should be 3, but " + node.jjtGetNumChildren());

    // assignment target tagging. for assignment target 'access', 'load' will
    // not apply
    LangUnitNode assign_tgt_refnode = node.getChildren(0);
    Debug.assertion(assign_tgt_refnode.isNodeId(JJTREFERENCE), "Assingment target Lvalue should be reference");

    LangUnitNode rmost_access_node = findRightMostAccessNode(assign_tgt_refnode);
    Debug.assertion(rmost_access_node != null, "access_node should be valid");

    LangUnitNode assignop_node = node.getChildren(1);
    Debug.assertion(assignop_node != null, "assignop_node should be valid");
    Debug.assertion(assignop_node.isNodeId(JJTASSIGNMENTOPERATOR), "assignop_node should be assign op");

    Token assignop_token = assignop_node.getAstToken();
    Debug.assertion(assignop_token != null, "assignop_token should be valid");

    switch(assignop_token.kind) {
    case ParserConstants.DEF_ASSIGN:
    case ParserConstants.ASSIGN:
    	rmost_access_node.setAssignTgt(true);
    }

    return null;
  }

  @Override
  public Object visit(ASTLoopHdrExpr node, Object data) throws CompileException {

    TContext upper_context = this.getTopContext();
    Debug.assertion(upper_context != null, "upper context should not be null");

    TContextStream loopstream_context = new TContextStream(upper_context, "loopstream_context", new OpObject(cpLoader),
        cpLoader);

    this.pushContext(loopstream_context);

    LOG.debug("New Stream Context is added: {}", loopstream_context);

    return null;
  }

  @Override
  public Object visit(ASTLoopExpr node, Object data) throws CompileException {

    dump_context_stack();

    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");

    LangUnitNode loop_hdr_node = node.getChildren(0);
    Debug.assertion(loop_hdr_node != null, "child node should not be null");
    Debug.assertion(loop_hdr_node.isNodeId(JJTLOOPHDREXPR), "1st child should be loophdr");

    loop_hdr_node.bindContext(context);

    return null;
  }

  @Override
  public Object visit(ASTConstant node, Object data) throws CompileException {

    Container constant_cont = null;

    Token t = node.getAstToken();
    Debug.assertion((t != null), "AstToken should not be null");

    TContext tu_context = getBottomContext();
    Debug.assertion((tu_context != null), "tu_context should not be null");

    AbsClassType constant_class = null;
    BigInteger big_int = null;

    switch (t.kind) {
    case ParserConstants.INTEGER_LITERAL:

      int int_val;

      if (!TypeUtil.isPositiveInteger(t.image)) {
        throw new CompileException("Invalid Parse Int Exception(" + t.image + ") is not integer", node);
      }

      big_int = TypeUtil.parsePositiveInteger(t.image);
      int_val = big_int.intValue();

      constant_cont = new Container("anonymous", Container.FORM_CONSTANT_VAR, true, false);
      constant_cont.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_INT));
      constant_cont.setContainerObject(new Integer(int_val));
      constant_cont.setAssigned(true);
      break;

    case ParserConstants.CHARACTER_LITERAL:

      String chr_str = getStringImage(t.image);

      if (!TypeUtil.isCharacter(chr_str)) {
        throw new CompileException("Invalid Parse char Exception(" + t.image + ") is not integer", node);
      }

      char char_val = TypeUtil.parseCharacter(chr_str);

      constant_cont = new Container("anonymous", Container.FORM_CONSTANT_VAR, true, false);
      constant_cont.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_CHAR));
      constant_cont.setContainerObject(new Character(char_val));
      constant_cont.setAssigned(true);
      break;

    case ParserConstants.SHORT_LITERAL:

      short short_val;

      if (!TypeUtil.isPositiveShort(t.image)) {
        throw new CompileException("Invalid Parse Long Exception(" + t.image + ") is not integer", node);
      }

      big_int = TypeUtil.parsePositiveShort(t.image);
      short_val = big_int.shortValue();

      constant_cont = new Container("anonymous", Container.FORM_CONSTANT_VAR, true, false);
      constant_cont.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_SHORT));
      constant_cont.setContainerObject(new Short(short_val));
      constant_cont.setAssigned(true);
      break;

    case ParserConstants.BYTE_LITERAL:

      byte byte_val;

      if (!TypeUtil.isPositiveByte(t.image)) {
        throw new CompileException("Invalid Parse Long Exception(" + t.image + ") is not integer", node);
      }

      big_int = TypeUtil.parsePositiveByte(t.image);
      byte_val = big_int.byteValue();

      constant_cont = new Container("anonymous", Container.FORM_CONSTANT_VAR, true, false);
      constant_cont.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BYTE));
      constant_cont.setContainerObject(new Byte(byte_val));
      constant_cont.setAssigned(true);
      break;

    case ParserConstants.LONG_INTEGER_LITERAL:

      long long_val;

      if (!TypeUtil.isPositiveLongInteger(t.image)) {
        throw new CompileException("Invalid Parse Long Exception(" + t.image + ") is not integer", node);
      }

      big_int = TypeUtil.parsePositiveLongInteger(t.image);
      long_val = big_int.longValue();

      constant_cont = new Container("anonymous", Container.FORM_CONSTANT_VAR, true, false);
      constant_cont.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_LONG));
      constant_cont.setContainerObject(new Long(long_val));
      constant_cont.setAssigned(true);
      break;

    case ParserConstants.FLOAT_LITERAL:

      float float_val;

      if (!TypeUtil.isPositiveFloat(t.image)) {
        throw new CompileException("Invalid Parse Float Exception.(" + t.image + ") ", node);
      }

      float_val = TypeUtil.parsePositiveFloat(t.image);

      constant_cont = new Container("anonymous", Container.FORM_CONSTANT_VAR, true, false);
      constant_cont.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_FLOAT));
      constant_cont.setContainerObject(new Float(float_val));
      constant_cont.setAssigned(true);
      break;

    case ParserConstants.DOUBLE_LITERAL:

      double double_val;

      if (!TypeUtil.isPositiveDouble(t.image)) {
        throw new CompileException("Invalid Parse Float Exception.(" + t.image + ") ", node);
      }

      double_val = TypeUtil.parsePositiveDouble(t.image);

      constant_cont = new Container("anonymous", Container.FORM_CONSTANT_VAR, true, false);
      constant_cont.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_DOUBLE));
      constant_cont.setContainerObject(new Double(double_val));
      constant_cont.setAssigned(true);
      break;

    case ParserConstants.STRING_LITERAL:

      String str_val = getStringImage(t.image);

      constant_cont = new Container("anonymous", Container.FORM_CONSTANT_VAR, true, false);

      constant_class = cpLoader.findClassFull("java/lang/String");
      Debug.assertion(constant_class != null, "It can not find java/lang/String");
      constant_cont.initializeType((AbsType) constant_class);
      constant_cont.setContainerObject(str_val);
      constant_cont.setAssigned(true);
      break;

    case ParserConstants.BOOLEAN_LITERAL:

      Boolean bool_val;
      if (t.image.equals("true")) {
        bool_val = true;
      } else if (t.image.equals("false")) {
        bool_val = false;
      } else {
        throw new CompileException("Invalid boolean literal(" + t.image + ")", node);
      }

      constant_cont = new Container("anonymous", Container.FORM_CONSTANT_VAR, true, false);
      constant_cont.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_BOOL));
      constant_cont.setContainerObject(bool_val);
      constant_cont.setAssigned(true);
      break;

    case ParserConstants.NULL:

      constant_cont = new Container("anonymous", Container.FORM_CONSTANT_VAR, true, false);
      constant_cont.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_VOID));
      constant_cont.setContainerObject(new ObjNull());
      constant_cont.setAssigned(true);
      break;

    default:
      throw new CompileException("AstToken Kind(" + t.kind + ") is Invalid", node);

    }

    node.setContainer(constant_cont);

    return null;
  }

  private String getStringImage(String image) {
    String str_val = "";

    if (image.length() > 2) {
      // remove double quote or quote
      str_val = image.substring(1, image.length() - 1);
    }

    str_val = TxtUtil.alternateEscapeSequence(str_val, "\\t", '\t');
    str_val = TxtUtil.alternateEscapeSequence(str_val, "\\b", '\b');
    str_val = TxtUtil.alternateEscapeSequence(str_val, "\\n", '\n');
    str_val = TxtUtil.alternateEscapeSequence(str_val, "\\r", '\r');
    str_val = TxtUtil.alternateEscapeSequence(str_val, "\\f", '\f');
    str_val = TxtUtil.alternateEscapeSequence(str_val, "\\'", '\'');
    str_val = TxtUtil.alternateEscapeSequence(str_val, "\\\"", '\"');
    str_val = TxtUtil.alternateEscapeSequence(str_val, "\\\\", '\\');

    return str_val;
  }

}
