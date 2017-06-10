package mezic.compiler;

import java.util.LinkedList;
import java.util.List;

import mezic.compiler.type.AbsClassType;
import mezic.compiler.type.AbsFuncType;
import mezic.compiler.type.AbsType;
import mezic.compiler.type.AbsTypeList;
import mezic.compiler.type.FuncSignatureDesc;
import mezic.compiler.type.OpInfo;
import mezic.compiler.type.Operation;
import mezic.compiler.type.TContext;
import mezic.compiler.type.TContextClass;
import mezic.compiler.type.TContextClosure;
import mezic.compiler.type.TContextFunc;
import mezic.compiler.type.TMapType;
import mezic.compiler.type.TMethodHandle;
import mezic.compiler.type.TPrimitiveClass;
import mezic.compiler.type.TraverseStackNode;
import mezic.parser.ASTAND;
import mezic.parser.ASTAccess;
import mezic.parser.ASTAdditive;
import mezic.parser.ASTArgument;
import mezic.parser.ASTArgumentHdr;
import mezic.parser.ASTArgumentList;
import mezic.parser.ASTAssignment;
import mezic.parser.ASTAssignmentOperator;
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
import mezic.parser.ASTFunctionDefSignature;
import mezic.parser.ASTIfCaseExpr;
import mezic.parser.ASTIfCondExpr;
import mezic.parser.ASTIfExpr;
import mezic.parser.ASTInclusiveOR;
import mezic.parser.ASTInvoke;
import mezic.parser.ASTJumpExpr;
import mezic.parser.ASTLogicalAND;
import mezic.parser.ASTLogicalANDHdr;
import mezic.parser.ASTLogicalOR;
import mezic.parser.ASTLogicalORHdr;
import mezic.parser.ASTLoopExpr;
import mezic.parser.ASTLoopHdrExpr;
import mezic.parser.ASTMapAccess;
import mezic.parser.ASTMatchCaseExpr;
import mezic.parser.ASTMatchCaseHeadExpr;
import mezic.parser.ASTMatchExpr;
import mezic.parser.ASTMatchHeadExpr;
import mezic.parser.ASTMultiplicative;
import mezic.parser.ASTOneExprFuncBody;
import mezic.parser.ASTParameter;
import mezic.parser.ASTPostfix;
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
import mezic.parser.ASTSubAccess;
import mezic.parser.ASTSymbolName;
import mezic.parser.ASTTranslationUnit;
import mezic.parser.ASTTranslationUnitElement;
import mezic.parser.ASTTranslationUnitHeader;
import mezic.parser.ASTUnary;
import mezic.parser.LangUnitNode;
import mezic.parser.ParserConstants;
import mezic.parser.Token;
import mezic.util.TypeUtil;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ASTCompileVisitor extends ASTTraverseVisitor {

  private static final long serialVersionUID = 903318641530201946L;

  private static final Logger LOG = LoggerFactory.getLogger(ASTCompileVisitor.class);

  public ASTCompileVisitor(CompilerLoader loader) {
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
  public void after_visit(LangUnitNode node) throws CompileException {

    AbsType converting_type = node.getConvertingType();
    if (converting_type != null) {
      node.clearConvertingType();
      nodecontainer_type_convert(converting_type);
    }

    LangUnitNode parent_node = (LangUnitNode) node.jjtGetParent();
    if (parent_node != null && parent_node.isNodeId(JJTLOGICALAND) && !node.isNodeId(JJTLOGICALANDHDR)) {
      prepareLogicalAnd(node);
    } else if (parent_node != null && parent_node.isNodeId(JJTLOGICALOR) && !node.isNodeId(JJTLOGICALORHDR)) {
      prepareLogicalOr(node);
    }

  }

  private void nodecontainer_type_convert(AbsType converting_type) throws CompileException {
    Reduction reduce = topReduction(); // Consumes argument from reduction stack
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);

    OpInfo opinfo = new OpInfo(getTopContext());

    Container cont = (Container) reduce;
    AbsType type = cont.getType();
    Debug.assertion(type != null, "Type should not be invalid");
    LOG.debug("Auto Cast " + type.getMthdDscStr() + "->" + converting_type.getMthdDscStr());

    cont.op().type_convert(cont, converting_type, opinfo);

    cont.initializeType(converting_type);
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
    dump_reduction_stack();

    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");
    Debug.assertion(context.isForm(AbsType.FORM_TU), "Context Type should be Translation Unit");

    return null;
  }

  @Override
  public Object visit(ASTTranslationUnitElement node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 1, "TU element should have one child");

    LangUnitNode child = (LangUnitNode) node.jjtGetChild(0);
    Debug.assertion(child != null, "TU element should not be invalid");

    if (!child.isNodeId(JJTIMPORT)) {
      Reduction reduce = popReduction();
      Debug.assertion(reduce != null, "popped TU element should not be invalid");
    }

    return null;
  }

  @Override
  public Object visit(ASTStreamHeader node, Object data) throws CompileException {

    TContext context = node.getBindContext();
    Debug.assertion(context != null, "Context should not be null");

    switch (node.getStreamForm()) {
    case LangUnitNode.STREAMFORM_DFLT:
      Debug.assertion(context.isForm(AbsType.FORM_STREAM),
          "Context Type should be Stream, but " + context.getFormString(context.getForm()));

      if (!node.hasDefinitionRefernce()) { // this stream is not code block, but
                                           // stream instance
        OpInfo opinfo = new OpInfo(getTopContext());
        langstream_init(opinfo);
      }

      LangUnitNode streamsig_node = (LangUnitNode) node.jjtGetParent();
      Debug.assertion(streamsig_node != null, "streamsig_node should be valid");
      Debug.assertion(streamsig_node.isNodeId(JJTSTREAMSIGNATURE), "streamsig_node should be stream signature");

      LangUnitNode stream_node = (LangUnitNode) streamsig_node.jjtGetParent();
      Debug.assertion(stream_node != null, "stream_node should be valid");
      Debug.assertion(stream_node.isNodeId(JJTSTREAM), "stream_node should be stream");

      Label stream_end_label = new Label();
      stream_node.setStreamEndLabel(stream_end_label); // this stream_end_label
                                                       // label will be visited
                                                       // in ASTStream

      break;

    case LangUnitNode.STREAMFORM_CLASS:
      Debug.assertion(context.isForm(AbsType.FORM_CLASS),
          "Context Type should be Class, but " + context.getFormString(context.getForm()));
      break;
    default:
      throw new CompileException("Invalid Stream Type(" + node.getStreamForm() + ")");
    }

    this.pushContext(context);

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

      String class_name = class_context.getName();

      String super_class_name = null;
      AbsClassType super_class = class_context.getSuperClass();

      if (super_class != null) {
        super_class_name = ((AbsType) super_class).getName();
        Debug.assertion(super_class_name != null, "Super Class Name should not be invalid");
      }

      String[] interfaces = check_interface_impl_and_get(class_context);

      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
      class_context.setClassWriter(cw);

      LOG.info("Creating Public Class...({}) super({})", class_name, super_class_name);

      //// Writing Byte Code
      cw.visit(Compiler.java_version, Opcodes.ACC_PUBLIC, class_name, null, super_class_name, interfaces);

      if (class_context.isGenDfltConstructor()) {
        // if it does not have any constructor, add a dummy constructor
        // default constructor
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();

        // super class default constructor call
        if (super_class != null) {
          LOG.info("ALOAD 0");
          LOG.info("INVOKESPECIAL " + super_class_name + " <init> ()V");

          mv.visitVarInsn(Opcodes.ALOAD, 0); // this
          mv.visitMethodInsn(Opcodes.INVOKESPECIAL, super_class_name, "<init>", "()V", false);
        }

        if (Debug.enable_compile_debug_print) {
          // LOG.info("GETSTATIC java/lang/System.out");
          // LOG.info("LDC "+class_name+" was instantiated");
          // LOG.info("INVOKEVIRTUAL java/io/PrintStream.println");
          mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
          mv.visitLdcInsn(class_name + " was instantiated");
          mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        //// end Byte Code
      }

    }
      break;

    default:
      // do nothing for other stream form ...
    }

    return null;
  }

  private String[] check_interface_impl_and_get(TContextClass class_ctx) throws CompileException {
    List<AbsClassType> if_list = class_ctx.getInterfaceList();

    if (if_list.size() == 0) {
      return null;
    }

    String[] if_str_arr = new String[if_list.size()];

    for (int i = 0; i < if_list.size(); i++) {

      AbsClassType if_type = if_list.get(i);

      List<AbsFuncType> func_list = if_type.getLocalFunctions();

      for (AbsFuncType func_type : func_list) {

        if (!func_type.is_abstract()) {
          continue;
        }
        // if( func_type.has_inst_body()) continue; // may be default method in
        // Java 8
        // (interface can have function body for default method)

        // checking interface implementation
        AbsTypeList para_typelist = func_type.getFuncSignatureDesc().getParameterTypeList();

        if (class_ctx.getLocalFunction(((AbsType) func_type).getName(), para_typelist) == null) {
          throw new CompileException(((AbsType) func_type).getName() + func_type.getFuncSignatureDesc().getMthdDscStr()
              + " should be implmented in " + class_ctx);
        }
      }

      if_str_arr[i] = ((AbsType) if_type).getName();
    }

    return if_str_arr;
  }

  @Override
  public Object visit(ASTStream node, Object data) throws CompileException {

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
          "Context Form should be Stream, but " + stream_ctx.getFormString(stream_ctx.getForm()));

      OpInfo opinfo = new OpInfo(getTopContext());

      Label stream_end_label = node.getStreamEndLabel();
      Debug.assertion(stream_end_label != null, "stream_end_label should be valid");
      opinfo.mv.visitLabel(stream_end_label);

      if (node.hasDefinitionRefernce()) { // this stream is code block
        pushReduction(stream_ctx); // reduced as a type
      } else { // this stream is not code block, this is stream instance
        AbsType stream_type = langstream_finish(opinfo);

        Container stream_obj = new Container("anonymous stream", Container.FORM_OPSTACK_VAR, true, false);
        stream_obj.initializeType(stream_type);
        pushReduction(stream_obj); // reduced as a object
      }

      break;

    case LangUnitNode.STREAMFORM_CLASS:
      Debug.assertion(stream_ctx.isForm(AbsType.FORM_CLASS),
          "Context Form should be Class, but " + stream_ctx.getFormString(stream_ctx.getForm()));

      TContextClass class_ctx = (TContextClass) stream_ctx;

      ClassWriter cw = class_ctx.getClassWriter();
      String class_name = class_ctx.getName();

      try {
        LOG.debug("Writing Class File(" + class_name + ")");
        //byte[] code = cpLoader.writeClassFile(cw, class_name);
        cpLoader.writeClassFile(cw, class_name);

      } catch (Exception e) {
        // e.printStackTrace();
        CompileException excp = new CompileException("Exception occurred in writing class file", node);
        excp.setTargetException(e);
        throw excp;
      }

      pushReduction(stream_ctx); // reduced as a type

      break;

    default:
      throw new CompileException("Invalid Stream Form(" + node.getStreamForm() + ")");
    }

    return null;
  }

  @Override
  public Object visit(ASTStreamBlockHdr node, Object data) throws CompileException {

    TContextFunc func_ctx = (TContextFunc) getTopContext().getClosestAncestor(AbsType.FORM_FUNC);
    if (func_ctx != null) {
      MethodVisitor mv = func_ctx.getMethodVisitor();
      Label streamblock_start_label = new Label();
      mv.visitLabel(streamblock_start_label);

      LangUnitNode stremblock_node = (LangUnitNode) node.jjtGetParent();
      Debug.assertion(stremblock_node != null, "stremblock_node should be valid");
      Debug.assertion(stremblock_node.isNodeId(JJTSTREAMBLOCK), "stremblock_node should be Stream Block node");

      stremblock_node.setStreamBlockStartLabel(streamblock_start_label);
    }

    return null;
  }

  @Override
  public Object visit(ASTStreamBlock node, Object data) throws CompileException {

    TContextFunc func_ctx = (TContextFunc) getTopContext().getClosestAncestor(AbsType.FORM_FUNC);
    if (func_ctx != null) {
      MethodVisitor mv = func_ctx.getMethodVisitor();
      Label streamblock_end_label = new Label();
      mv.visitLabel(streamblock_end_label);
      node.setStreamBlockEndLabel(streamblock_end_label);

      LangUnitNode stream_node = (LangUnitNode) node.jjtGetParent();
      Debug.assertion(stream_node != null, "stream_node should be valid");

      if (stream_node.isNodeId(JJTSTREAM)) {
        int catchnode_child_idx = stream_node.getChildIdxWithId(JJTCATCH, 0);
        if (catchnode_child_idx != -1) {
          Label stream_end_label = stream_node.getStreamEndLabel();
          Debug.assertion(stream_end_label != null, "stream_end_label should be valid");

          LOG.info("GOTO stream_end_label({}) in stream block end", stream_end_label);
          mv.visitJumpInsn(GOTO, stream_end_label);
        }
      }

    }

    return null;
  }

  @Override
  public Object visit(ASTCatchExceptionList node, Object data) throws CompileException {

    TContext context = node.getBindContext();
    Debug.assertion(context != null, "Context should not be null");

    Debug.assertion(context.isForm(AbsType.FORM_STREAM),
        "Context Type should be stream, but " + context.getFormString(context.getForm()));

    this.pushContext(context);
    dump_context_stack();

    LangUnitNode catch_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(catch_node != null, "catch_node should be valid");
    Debug.assertion(catch_node.isNodeId(JJTCATCH), "catch_node should be listcatch node");

    TContextFunc func_ctx = (TContextFunc) getTopContext().getClosestAncestor(AbsType.FORM_FUNC);
    Debug.assertion(func_ctx != null, "catch should be used in function context");

    MethodVisitor mv = func_ctx.getMethodVisitor();
    Label catch_label = new Label();
    mv.visitLabel(catch_label);
    catch_node.setCatchLabel(catch_label);

    Container dflt_excp_var = context.getLocalChildVariable(TContext.VAR_EXCEPTION_NAME);
    Debug.assertion(dflt_excp_var != null, "dflt_excp_var should be valid");

    // mv.visitInsn(Opcodes.POP);

    LOG.info("ASTORE {}", dflt_excp_var.getContextVarIdx());
    mv.visitVarInsn(Opcodes.ASTORE, dflt_excp_var.getContextVarIdx());

    return null;
  }

  @Override
  public Object visit(ASTCatch node, Object data) throws CompileException {

    dump_context_stack();
    TContext context = popContext();
    Debug.assertion(context != null, "Context should not be null");

    LangUnitNode stream_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(stream_node != null, "stream_node should be valid");
    Debug.assertion(stream_node.isNodeId(JJTSTREAM), "stream_node should be stream node");

    int streamblock_idx = stream_node.getChildIdxWithId(JJTSTREAMBLOCK, 0);
    LangUnitNode streamblock_node = (LangUnitNode) stream_node.getChildren(streamblock_idx);
    Debug.assertion(streamblock_node != null, "streamblock_node should be valid");
    Debug.assertion(streamblock_node.isNodeId(JJTSTREAMBLOCK), "streamblock_node should be streamblock node");

    Label stream_end_label = stream_node.getStreamEndLabel();
    Debug.assertion(stream_end_label != null, "stream_end_label should be valid");

    Label streamblock_start_label = streamblock_node.getStreamBlockStartLabel();
    Debug.assertion(streamblock_start_label != null, "streamblock_start_label should be valid");

    Label streamblock_end_label = streamblock_node.getStreamBlockEndLabel();
    Debug.assertion(streamblock_end_label != null, "streamblock_end_label should be valid");

    Label catch_label = node.getCatchLabel();
    Debug.assertion(catch_label != null, "catch_label should be valid");

    TContextFunc func_ctx = (TContextFunc) getTopContext().getClosestAncestor(AbsType.FORM_FUNC);
    Debug.assertion(func_ctx != null, "catch should be used in function context");

    MethodVisitor mv = func_ctx.getMethodVisitor();

    /*
     * public void visitCode() { super.visitCode(); visitTryCatchBlock(start,
     * end, handler, "java/lang/Exception"); visitLabel(start); }
     * mv.visitTryCatchBlock(arg0, arg1, arg2, arg3);
     */

    AbsTypeList excptype_list = node.getCatchExceptionList();
    Debug.assertion(excptype_list != null, "excptype_list should be valid");

    AbsType excptype = null;

    int size = excptype_list.size();
    for (int i = 0; i < size; i++) {
      excptype = excptype_list.get(i);
      LOG.debug("trycatch block for exception " + excptype);
      mv.visitTryCatchBlock(streamblock_start_label, streamblock_end_label, catch_label, excptype.getName());
    }

    LOG.info("GOTO stream_end_label({})", stream_end_label);
    mv.visitJumpInsn(GOTO, stream_end_label);

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

      buildClosureClass(closure_context);

      pushContext(closure_context);
    }

    this.pushContext(func_ctx);
    dump_context_stack();

    return null;
  }

  private void buildClosureClass(TContext closure_ctx) throws CompileException {
    Debug.assertion(closure_ctx instanceof TContextClosure, "closure_ctx should be closure ctx");

    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    ((TContextClosure) closure_ctx).setClassWriter(cw);

    cw.visit(Compiler.java_version, Opcodes.ACC_PUBLIC, closure_ctx.getName(), null, "java/lang/Object", null);

    // default constructor
    MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    mv.visitCode();

    LOG.info("ALOAD 0 for this");
    LOG.info("INVOKESPECIAL java/lang/Object");

    mv.visitVarInsn(Opcodes.ALOAD, 0); // this
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    if (Debug.enable_compile_debug_print) {
      mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
      mv.visitLdcInsn(closure_ctx.getName() + " was instantiated");
      mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
    // end default constructor

    TContext parent_ctx = (TContext) closure_ctx.getOwnerType();
    Debug.assertion(parent_ctx != null, "parent_ctx should be valid");

    // add closure class member field
    Container var_cont = null;
    AbsType var_type = null;

    LinkedList<Container> var_list = closure_ctx.get_childvar_list();
    int var_size = var_list.size();
    Container closure_member_var = null;

    for (int i = 0; i < var_size; i++) {
      closure_member_var = var_list.get(i);
      var_cont = closure_member_var.getClosureOrgFuncvarContainer();

      Debug.assertion(var_cont != null, "var_cont should be valid");
      // Debug.assertion(var_cont.isForm(Container.FORM_FUNSTACK_VAR), "var_cont
      // should be stack variable");
      Debug.assertion(var_cont.isTypeInitialized(), "var_cont type should be initialized");

      if (var_cont.isForm(Container.FORM_FUNSTACK_VAR)) {
        // only assigned stack variable will be copied
        if (var_cont.isAssigned()) {
          LOG.debug("  (" + var_cont.getContextVarIdx() + ") " + var_cont);
          var_type = var_cont.getType();
          cw.visitField(Opcodes.ACC_PUBLIC, var_cont.getName(), var_type.getMthdDscStr(), null, null).visitEnd();
        }
      } else if (var_cont.isForm(Container.FORM_OBJMEMBER_VAR)) {
        LOG.debug("  (" + var_cont.getContextVarIdx() + ") " + var_cont);
        var_type = var_cont.getType();
        cw.visitField(Opcodes.ACC_PUBLIC, var_cont.getName(), var_type.getMthdDscStr(), null, null).visitEnd();
      } else {
        throw new CompileException("Invalid var_cont form(" + var_cont + ")");
      }

    }

  }

  @Override
  public Object visit(ASTFunctionDefSignature node, Object data) throws CompileException {

    LangUnitNode func_hdr = node.getChildren(0);
    Debug.assertion(func_hdr.isNodeId(JJTFUNCTIONDEFHDR),
        "first child should be Func Header, but(" + func_hdr.getNodeName() + ")");

    switch (func_hdr.getFuncForm()) {

    case LangUnitNode.FUNCFORM_MEMBER:
    case LangUnitNode.FUNCFORM_STATIC: {
      TContextFunc func_context = (TContextFunc) getTopContext();
      Debug.assertion(func_context != null, "top context should not be null");
      Debug.assertion(func_context.isForm(AbsType.FORM_FUNC), "Top Context should be function");

      TContextClass class_context = (TContextClass) func_context.getOwnerType();
      Debug.assertion(class_context != null, "Class Context should not be null");
      Debug.assertion(class_context.isForm(AbsType.FORM_CLASS), "parent type should be ContextClass");

      ClassWriter cw = class_context.getClassWriter();
      Debug.assertion(cw != null, "ClassWriter should not be null");

      String func_name = func_context.getName();
      String func_desc_str = func_context.getMthdDscStr();

      String[] exceptions = null;

      if (func_context.getThrowsList() != null) {
        exceptions = func_context.getThrowsList().toStringArray();
      }

      if (func_context.is_constructor()) {
        LOG.debug(" Creating Constructor...(" + func_name + ":" + func_desc_str + ")");

        String super_class_name = null;
        AbsClassType super_class = class_context.getSuperClass();

        if (super_class != null) {
          super_class_name = ((AbsType) super_class).getName();
          Debug.assertion(super_class_name != null, "Super Class Name should not be invalid");
        }

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, func_name, func_desc_str, null, exceptions);
        func_context.setMethodVisitor(mv);

        mv.visitCode();

        if (super_class != null && !func_context.has_constructor_call()) {

          LOG.info("ALOAD 0 for this");
          LOG.info("INVOKESPECIAL {}", super_class_name);

          mv.visitVarInsn(Opcodes.ALOAD, 0); // this
          mv.visitMethodInsn(Opcodes.INVOKESPECIAL, super_class_name, "<init>", "()V", false);
        }

        if (Debug.enable_compile_debug_print) {
          mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
          mv.visitLdcInsn(class_context.getName() + " was instantiated");
          mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }

        addLineLable(mv, node.jjtGetFirstToken().beginLine);
      } else {
        LOG.debug(" Creating Function...(" + func_name + ":" + func_desc_str + ")");

        int access = 0;
        access |= Opcodes.ACC_PUBLIC;
        // temporal for closure call
        if (!func_context.is_closure() && func_context.is_static()) {
          access |= Opcodes.ACC_STATIC;
        }

        MethodVisitor mv = cw.visitMethod(access, func_name, func_desc_str, null, exceptions);
        func_context.setMethodVisitor(mv);
        mv.visitCode();

        if (Debug.enable_compile_debug_print) {
          // printing function name
          mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
          mv.visitLdcInsn(func_name + " is called");
          mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }

        addLineLable(mv, node.jjtGetFirstToken().beginLine);
      }

    }
      break;

    default:
      // do nothing for list ...
      throw new CompileException("Invalid Function Form(" + func_hdr.getFuncForm() + ")", node);
    }

    return null;
  }

  private void addLineLable(MethodVisitor mv, int line) {
    Label linelabel = new Label();
    mv.visitLabel(linelabel);
    mv.visitLineNumber(line, linelabel);
  }

  @Override
  public Object visit(ASTFunctionDef node, Object data) throws CompileException {

    dump_context_stack();

    TContext func_ctx = popContext();
    Debug.assertion(func_ctx != null, "Context should not be null");

    LangUnitNode funcsignature_node = node.getChildren(0);
    Debug.assertion(funcsignature_node != null, "child node should not be null");
    Debug.assertion(funcsignature_node.isNodeId(JJTFUNCTIONDEFSIGNATURE), "child should be signature");

    LangUnitNode funcheader_node = funcsignature_node.getChildren(0);
    Debug.assertion(funcheader_node != null, "child node should not be null");
    Debug.assertion(funcheader_node.isNodeId(JJTFUNCTIONDEFHDR), "grand child should be func head");

    switch (funcheader_node.getFuncForm()) {
    case LangUnitNode.FUNCFORM_MEMBER:
    case LangUnitNode.FUNCFORM_STATIC:
      Debug.assertion(func_ctx.isForm(AbsType.FORM_FUNC),
          "Context Type should be Function, but " + func_ctx.getFormString(func_ctx.getForm()));
      LOG.debug(" Building Function...(" + func_ctx.getName() + ")");

      AbsType func_ret_type = ((AbsFuncType) func_ctx).getReturnType(cpLoader);

      OpInfo opinfo = new OpInfo(func_ctx);

      opinfo.mv = ((TContextFunc) func_ctx).getMethodVisitor();

      if (func_ret_type.isName(TPrimitiveClass.NAME_VOID) && branch_tree.getCurrBranch().isValid()) {
        opinfo.mv.visitInsn(RETURN);
      }

      // Apply function body will not be compiled(does not traverse), it makes a
      // dummy body.
      if (((TContextFunc) func_ctx).is_apply()) {
        func_ret_type.op().return_dummy_variable(opinfo); // return some value
      }

      opinfo.mv.visitMaxs(0, 0);
      opinfo.mv.visitEnd();

      Container func_type_cont = func_ctx.getTypeContainer();
      Debug.assertion(func_type_cont != null, "func_type_cont should not be invalid");
      // node.setContainer(func_type_cont);

      if (((TContextFunc) func_ctx).is_closure()) {
        boolean is_applied_func = funcheader_node.isAppliedFunc();

        Container loaded_mh = construct_methodhandle(func_type_cont, is_applied_func);
        pushReduction(loaded_mh);

        TContext closure_context = popContext(); // pop closure context
        Debug.assertion(closure_context != null, "Context should not be null");
        Debug.assertion(closure_context instanceof TContextClosure, "popped context should be TContextClosure");

        generateClosureClass(closure_context);

      } else {
        pushReduction(func_type_cont); // reduced as a type Container
      }

      break;

    default:
      throw new CompileException("Invalid Func Form(" + node.getFuncForm() + ")");
    }

    return null;
  }


  private void generateClosureClass(TContext closure_ctx) throws CompileException {
    ClassWriter cw = ((TContextClosure) closure_ctx).getClassWriter();
    Debug.assertion(cw != null, "cw should be valid");

    // write class file
    try {
      LOG.debug("Writing Class File(" + closure_ctx.getName() + ")");
      //byte[] code = cpLoader.writeClassFile(cw, closure_ctx.getName());
      cpLoader.writeClassFile(cw, closure_ctx.getName());

    } catch (Exception e) {
      // e.printStackTrace();
      CompileException excp = new CompileException("Exception occurred in writing class file");
      excp.setTargetException(e);
      throw excp;
    }
  }

  private void generateClosureContextCopyInst(TContext closure_ctx, OpInfo opinfo) throws CompileException {
    LOG.debug("generateClosureContextCopyInst");

    Debug.assertion(closure_ctx instanceof TContextClosure, "closure_ctx should be closure ctx");

    TContext parent_ctx = (TContext) closure_ctx.getOwnerType();
    Debug.assertion(parent_ctx != null, "parent_ctx should be valid");

    TContext owner_func_ctx = parent_ctx.getClosestAncestor(AbsType.FORM_FUNC);
    Debug.assertion(owner_func_ctx != null, "owner func_ctx should be valid");

    Container var_cont = null;
    AbsType var_type = null;

    LinkedList<Container> var_list = closure_ctx.get_childvar_list();
    int var_size = var_list.size();
    Container closure_member_var = null;

    this.dump_reduction_stack();

    for (int i = 0; i < var_size; i++) {
      closure_member_var = var_list.get(i);
      var_cont = closure_member_var.getClosureOrgFuncvarContainer();

      Debug.assertion(var_cont != null, "var_cont should be valid");
      Debug.assertion(var_cont.isTypeInitialized(), "var_cont type should be initialized");

      if (var_cont.isForm(Container.FORM_FUNSTACK_VAR)) {
        if (var_cont.isAssigned()) {
          LOG.debug(" Add Copy Instruction (" + var_cont.getContextVarIdx() + ") " + var_cont);

          var_type = var_cont.getType();
          opinfo.mv.visitInsn(DUP); // duplicate closure class instances
                                    // (category 1)
          var_cont.op().load_funcstack_variable(var_cont, opinfo, var_cont.getContextVarIdx());
          closure_member_var.op().assign(closure_member_var, var_cont, opinfo);
        }
      } else if (var_cont.isForm(Container.FORM_OBJMEMBER_VAR)) { // for nested
                                                                  // closure
                                                                  // case

        LOG.debug(" Add Copy Instruction (" + var_cont.getContextVarIdx() + ") " + var_cont);

        var_type = var_cont.getType();

        opinfo.mv.visitInsn(DUP); // duplicate closure class instances (category
                                  // 1)

        Container owner_cont = var_cont.getOwnerContainer();
        Debug.assertion(owner_cont != null, "owner_cont should be valid");
        Debug.assertion(owner_cont.isTypeInitialized(), "owner_cont should be type initialized");
        AbsType owner_type = owner_cont.getType();

        opinfo.mv.visitVarInsn(Opcodes.ALOAD, 0); // load closure variable(for
                                                  // loading closure member var)

        LOG.info("GETFIELD {}:{}({})", owner_type.getName(), var_cont.getName(), var_type.getMthdDscStr());
        opinfo.mv.visitFieldInsn(Opcodes.GETFIELD, owner_type.getName(), var_cont.getName(), var_type.getMthdDscStr());

        closure_member_var.op().assign(closure_member_var, var_cont, opinfo);

      } else {
        throw new CompileException("Invalid var_cont form(" + var_cont + ")");
      }

    }

  }

  @Override
  public Object visit(ASTOneExprFuncBody node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 1, "List element should have one child");
    // SpringUnitNode child = (SpringUnitNode)node.jjtGetChild(0);

    Reduction reduce = popReduction();
    Debug.assertion(reduce != null, "reduction should not be invalid");
    Debug.assertion(reduce.isContainer(), "reduction should be container (" + reduce + ")");

    Container ret_cont = (Container) reduce;
    Debug.assertion(ret_cont.isTypeInitialized(), "Reducing Type is not initialized in the function signature");

    dump_reduction_stack();

    // function or list is in here
    // MethodVisitor mv = getClosestFunContext().getMethodVisitor();
    OpInfo opinfo = new OpInfo(getTopContext());
    ret_cont.getType().op().return_variable(ret_cont, opinfo);

    return null;
  }

  private void langstream_init(OpInfo opinfo) throws CompileException {

    // temporal for stream
    /*
     * opinfo.mv.visitTypeInsn(NEW, CompilerLoader.LANGLIST_CLASS_NAME);
     * opinfo.mv.visitInsn(DUP); // category 1
     * opinfo.mv.visitMethodInsn(INVOKESPECIAL,
     * CompilerLoader.LANGLIST_CLASS_NAME, "<init>", "()V", false);
     * opinfo.mv.visitInsn(DUP); // category 1
     */

    opinfo.mv.visitTypeInsn(NEW, CompilerLoader.LANGSTREAM_CLASS_NAME);
    opinfo.mv.visitInsn(DUP); // category 1 -> this dup is for 'initstream( )'
                              // method
    opinfo.mv.visitInsn(DUP); // category 1 -> this dup is for stream reducing

    opinfo.mv.visitMethodInsn(INVOKESPECIAL, CompilerLoader.LANGSTREAM_CLASS_NAME, "<init>", "()V", false);

    opinfo.mv.visitTypeInsn(NEW, "java/util/LinkedList");
    opinfo.mv.visitInsn(DUP); // category 1
    opinfo.mv.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedList", "<init>", "()V", false);
    opinfo.mv.visitInsn(DUP); // category 1

  }

  private void langstream_ele_add(OpInfo opinfo, Reduction reduce, LangUnitNode ref_node) throws CompileException {
    Container cont = null;
    AbsType type = null;

    // stream is code block
    if (ref_node.hasDefinitionRefernce()) {
      LOG.debug("langstream_ele_add(" + reduce + ")");

      if (!reduce.isContainer()) {
        return; // if it is not container, do nothing
      }
      cont = (Container) reduce;
      type = cont.getType();

      if (cont.isForm(Container.FORM_OPSTACK_VAR) && !type.isName(TPrimitiveClass.NAME_VOID)) {
        LOG.debug("Call POP for type(" + type + ")");
        type.op().pop(cont, new OpInfo(getTopContext()));
      }
    } else { // this stream is not code block, but stream instance

      // add element to stream instance
      if (reduce.isContainer()) {
        cont = (Container) reduce;
        type = cont.getType();

        if (type.isName(TPrimitiveClass.NAME_VOID)) {
          opinfo.mv.visitLdcInsn("Void Type");
        } else {
          AbsType objtype = (AbsType) cpLoader.findClassFull("java/lang/Object");
          Debug.assertion(objtype != null, "java/lang/Object class should be valid");

          if (cpLoader.isCompatibleClass(type, objtype)) {
            // do nothing
          } else if (cpLoader.isConvertibleClass(type, objtype)) {
            AbsType convertedType = cont.op().type_convert(cont, objtype, opinfo);
            cont.initializeType(convertedType);
          } else {
            throw new CompileException("This object cannot be assigned to stream");
          }

        }
        LOG.debug("Add Container(" + cont + ") to AreAM");

        // temporal for stream
        // opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
        // CompilerLoader.LANGLIST_CLASS_NAME, "add", "(Ljava/lang/Object;)Z",
        // false);
        opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/LinkedList", "add", "(Ljava/lang/Object;)Z", false);

        opinfo.mv.visitInsn(POP); // category 1 pop - boolean (result of 'add'
                                  // method)
        opinfo.mv.visitInsn(DUP); // category 1 dup - lang stream
      }
    }
  }

  private AbsType langstream_finish(OpInfo opinfo) throws CompileException {
    opinfo.mv.visitInsn(POP);

    // temporal for stream
    opinfo.mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedList", "stream", "()Ljava/util/stream/Stream;", false);

    opinfo.mv.visitMethodInsn(INVOKEVIRTUAL, CompilerLoader.LANGSTREAM_CLASS_NAME, "initstream",
        "(Ljava/util/stream/Stream;)V", false);
    // opinfo.mv.visitMethodInsn(INVOKESPECIAL,
    // CompilerLoader.LANGSTREAM_CLASS_NAME, "<init>",
    // "(Ljava/util/stream/Stream;)V", false);

    AbsType stream_type = (AbsType) cpLoader.findClassFull(CompilerLoader.LANGSTREAM_CLASS_NAME);
    Debug.assertion(stream_type != null, "stream_type should not be invalid");

    return stream_type;
  }

  @Override
  public Object visit(ASTStreamBlockElementHdr node, Object data) throws CompileException {

    // function or stream is in here
    TContext func_context = getTopContext().getClosestAncestor(AbsType.FORM_FUNC);
    if (func_context != null) {
      MethodVisitor mv = ((TContextFunc) func_context).getMethodVisitor();
      Debug.assertion(mv != null, "mv should be valid");

      /*
       * Label linelabel = new Label();
       *
       * // line label for debugging mv.visitLabel(linelabel);
       * mv.visitLineNumber(node.jjtGetFirstToken().beginLine, linelabel);
       */

      addLineLable(mv, node.jjtGetFirstToken().beginLine);
    }

    return null;
  }

  @Override
  public Object visit(ASTStreamBlockElement node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 2, "stream block element should have one child");

    Reduction reduce = popReduction();
    Debug.assertion(reduce != null, "reduction should be valid");
    Debug.assertion(reduce.isContainer() || reduce.isControl() || reduce.isType(),
        "reduction should be container or control(" + reduce + ")");
    dump_reduction_stack();

    switch (getTopContext().getForm()) {
    case TContext.FORM_FUNC:
    case TContext.FORM_STREAM:
      break;
    default:
      return null;
    }

    // function or stream is in here
    OpInfo opinfo = new OpInfo(getTopContext());
    langstream_ele_add(opinfo, reduce, node);

    return null;
  }

  @Override
  public Object visit(ASTParameter node, Object data) throws CompileException {

    Reduction reduce = null;

    reduce = popReduction(); // Consume pushed container...
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);

    return null;
  }

  @Override
  public Object visit(ASTArgumentList node, Object data) throws CompileException {

    // for making dummy var arg array
    // Note !!
    // * invoke_argtype_list can have no var arg list
    // example) public static java.nio.file.Path get(String first, String...
    // more)
    // but, 'Path.get("./a") is possible( invoking parameter does not have var
    // arg list ).
    // Compiler automatically add String[0] following the "./a"

    Container maptype_cont = node.getVarArgMapTypeContainer();
    int map_size = node.getVarArgMapSize();

    // creating dummy var arg array
    if (maptype_cont != null && map_size == 0) {
      Debug.assertion(maptype_cont.isTypeInitialized(), "maptype_cont should be type initialized");

      OpInfo opinfo = new OpInfo(getTopContext());

      // load_int(map_size, opinfo);
      load_int(0, opinfo);

      //Container ret_obj = maptype_cont.op().map_create(maptype_cont, null, opinfo);
      maptype_cont.op().map_create(maptype_cont, null, opinfo);

    }

    return null;
  }

  @Override
  public Object visit(ASTArgumentHdr node, Object data) throws CompileException {

    Container maptype_cont = node.getVarArgMapTypeContainer();
    int map_idx = node.getVarArgMapIdx();
    int map_size = node.getVarArgMapSize();

    if (maptype_cont != null && map_idx != -1) {
      Debug.assertion(maptype_cont.isTypeInitialized(), "maptype_cont should be type initialized");

      OpInfo opinfo = new OpInfo(getTopContext());

      if (map_idx == 0) {
        load_int(map_size, opinfo);
        //Container ret_obj = maptype_cont.op().map_create(maptype_cont, null, opinfo);
        maptype_cont.op().map_create(maptype_cont, null, opinfo);
      }

      maptype_cont.op().dup(maptype_cont, opinfo);

      load_int(map_idx, opinfo);

    }

    return null;
  }

  @Override
  public Object visit(ASTArgument node, Object data) throws CompileException {

    Reduction reduce = popReduction(); // Consumes argument from reduction stack
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);

    OpInfo opinfo = new OpInfo(getTopContext());

    Container maptype_cont = node.getVarArgMapTypeContainer();
    int map_idx = node.getVarArgMapIdx();

    if (maptype_cont != null && map_idx != -1) {
      AbsType maptype = maptype_cont.getType();
      Debug.assertion(maptype != null, "maptype should be valid");

      AbsType mapele_type = ((TMapType) maptype).getElementType();
      Debug.assertion(mapele_type != null, "mapele_type should be valid");

      mapele_type.op().store_map_element(opinfo);
    }

    return null;
  }

  @Override
  public Object visit(ASTReference node, Object data) throws CompileException {

    return null;
  }

  private void checkDup(LangUnitNode node, Container cont) throws CompileException {
    AbsType type = null;

    if (node.isSetDup()) {
      Debug.assertion(cont.isForm(Container.FORM_OPSTACK_VAR), "cont should be opstack value");
      type = cont.getType();
      Debug.assertion(type != null, "type should be valid");

      //MethodVisitor mv = getClosestFunContext().getMethodVisitor();
      getClosestFunContext().getMethodVisitor();
      type.op().dup(cont, new OpInfo(getTopContext()));
    }

    if (node.isSetDupX1()) {
      Debug.assertion(cont.isForm(Container.FORM_OPSTACK_VAR), "cont should be opstack value");
      type = cont.getType();
      Debug.assertion(type != null, "type should be valid");

      //MethodVisitor mv = getClosestFunContext().getMethodVisitor();
      getClosestFunContext().getMethodVisitor();

      type.op().dup_x1(cont, new OpInfo(getTopContext()));
    }
  }

  @Override
  public Object visit(ASTAccess node, Object data) throws CompileException {

    dump_reduction_stack();

    Reduction reduce = null;
    Container access_cont = null;

    LangUnitNode ref_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(ref_node != null, "ref_node is invalid");
    Debug.assertion(ref_node.isNodeId(JJTREFERENCE), "ref_node is reference node");

    LangUnitNode child_node = node.getChildren(0);
    Debug.assertion(child_node != null, "Child Node is invalid");

    if (child_node.getContainer() == null) {// Child Node might not push any
                                            // reduction.
      LOG.debug("Access child Node does not have container and it is not function call");

      if (topReduction() != null && topReduction().isContainer()) {
        checkDup(node, (Container) topReduction());
      }

      return null;
    }

    if (getTopContext().isForm(AbsType.FORM_CLASS) || getTopContext().isForm(AbsType.FORM_TU)) {
      // do nothing
      return null;
    }

    reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);
    access_cont = (Container) reduce;
    Debug.assertion(access_cont.isTypeInitialized(), "Container should be type initialized");

    Container owner_cont = null;

    switch (access_cont.getForm()) {
    case Container.FORM_TYPE: {
      AbsType type = access_cont.getType();
      Debug.assertion(type != null, "Type should not be null");

      switch (type.getForm()) {
      case AbsType.FORM_PKG:
        LOG.debug("Package(" + type + ")");
        // package container is not used any more.(sub-access elements was
        // already resolved)
        break;

      case AbsType.FORM_CLASS:
      case AbsType.FORM_FUNC:
        // preparation for creating multi-dimension map in one time
        // - int[10][10][10] ( int[10][][] style does not necessary this work)
        int map_dimension = cntSubRefChildNodeIdChain(ref_node, JJTMAPACCESS);
        LOG.debug("map_dimension=" + map_dimension);
        if (map_dimension >= 2) {
          prepare_multidimension_map_creation(type, map_dimension, new OpInfo(getTopContext()));
        }

        if (child_node.isDfltContextMember()) {
          reduce = (Container) popReduction();
          Debug.assertion(reduce != null, "Invalid Popped Reduction");
          Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);
          owner_cont = (Container) reduce;
        }

        pushReduction(access_cont);
        break;

      default:
        throw new CompileException("Invalid Form(" + type.getFormString(type.getForm()) + ")");
      }
    }
      return null;

    case Container.FORM_CONSTANT_VAR:
    case Container.FORM_FUNSTACK_VAR:
    case Container.FORM_OPSTACK_VAR:
    case Container.FORM_OBJMEMBER_VAR: {
      switch (child_node.getNodeId()) {
      case JJTFUNCTIONDEF: {
        if (access_cont.getType() instanceof TMethodHandle) {
          // MethodHandle has been already loaded in FunctionDef
          pushReduction(access_cont);
        } else { // only methodhandle funciontdef can be variable
          throw new CompileException("Not Supported access_cont type(" + access_cont + ")");
        }

      }
        break;

      case JJTSYMBOLNAME:
      case JJTCONSTANT: {
        if (access_cont.isSingleton()) {
          owner_cont = access_cont.getOwnerContainer();
        } else {
          if (child_node.isDfltContextMember()) {
            reduce = (Container) popReduction();
            Debug.assertion(reduce != null, "Invalid Popped Reduction");
            Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);
            owner_cont = (Container) reduce;
          }
          // owner_cont can be null...
        }

        if (node.isAssignTgt()) {
          LOG.debug("node is assign target, do not load");
          pushReduction(access_cont);
        } else {
          Container loaded_obj = access_cont.op().load_variable(access_cont, owner_cont, new OpInfo(getTopContext()));
          Debug.assertion(loaded_obj.isForm(Container.FORM_OPSTACK_VAR), "loaded obj should be op stack var");
          pushReduction(loaded_obj);
          checkDup(node, loaded_obj);
        }

      }
        break;

      default:
        // list(class) def, if, match, loop cannot be variable
        throw new CompileException("Not Supported child node(" + child_node + ")");
      }
    }
      return null;

    default:
      throw new CompileException("Invalid access_cont Form(" + access_cont + ")");
    }

  }

  private void prepare_multidimension_map_creation(AbsType ele_type, int dimension, OpInfo opinfo)
      throws CompileException {
    LOG.info("LOAD Class({})", ele_type);
    load_class(ele_type, opinfo);
    LOG.info("LOAD INT CONSTANT({})", dimension);
    load_int(dimension, opinfo);
    LOG.info("NEWARRAY T_INT");
    opinfo.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);

    LOG.info("DUP");
    opinfo.mv.visitInsn(Opcodes.DUP); // dup class arry (cat. 1)
    load_int(0, opinfo); // first array index
  }

  @Override
  public Object visit(ASTSubAccess node, Object data) throws CompileException {

    dump_reduction_stack();

    Reduction reduce = null;
    Container subaccess_cont = null;
    LangUnitNode ref_node = (LangUnitNode) node.jjtGetParent();

    LangUnitNode child_node = node.getChildren(0);
    Debug.assertion(child_node != null, "Child Node is invalid");

    if (child_node.getContainer() == null) { // Child Node might not push any
                                             // reduction.
      LOG.debug("Sub Access child Node does not have container in " + child_node);

      dump_reduction_stack();
      Debug.stop();
      return null;
    }

    // class member registering symbol will not reach here
    if (getTopContext().isForm(AbsType.FORM_CLASS) || getTopContext().isForm(AbsType.FORM_TU)) {
      // do nothing
      return null;
    }

    reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);
    subaccess_cont = (Container) reduce;
    Debug.assertion(subaccess_cont.isTypeInitialized(), "Container should be type initialized");
    AbsType subaccess_type = subaccess_cont.getType();
    Debug.assertion(subaccess_type != null, "Type should not be null");

    switch (subaccess_cont.getForm()) {
    case Container.FORM_TYPE: {
      switch (subaccess_type.getForm()) {
      case AbsType.FORM_PKG: {
        LOG.debug("Package(" + subaccess_type + ")");
        // package container is not used any more.(underlying element was
        // already resolved)
      }
        break;

      case AbsType.FORM_CLASS: {

        int map_dimension = cntSubRefChildNodeIdChain(ref_node, JJTMAPACCESS);
        LOG.debug("map_dimension=" + map_dimension);
        if (map_dimension >= 2) {
          prepare_multidimension_map_creation(subaccess_type, map_dimension, new OpInfo(getTopContext()));
        }
        pushReduction(subaccess_cont);
      }
        break;

      case AbsType.FORM_FUNC: {
        Debug.assertion(isNextSubRefChildNodeid(ref_node, JJTINVOKE), "Next Child Sub Ref should be Function Call.");

        AbsFuncType func_type = (AbsFuncType) subaccess_type;
        Container owner_cont = null;

        if (func_type.is_constructor()) {
          // owner access(Package) did not push its container(see 'case
          // AbsType.FORM_PKG:' in Access/SubAccess )
          this.dump_reduction_stack();
        } else {
          owner_cont = (Container) popReduction();
          Debug.assertion(owner_cont != null, "Invalid source container");

          if (func_type.is_static()) {
            if (owner_cont.isForm(Container.FORM_OPSTACK_VAR)) {
              /*
               * a.b.c.static_func() : before calling 'static_func' it needs to
               * pop 'c'
               */
              //MethodVisitor mv = getClosestFunContext().getMethodVisitor();
              getClosestFunContext().getMethodVisitor();

              LOG.info("POP OPSTACK_VAR for INVOKESTATIC");
              owner_cont.getType().op().pop(owner_cont, new OpInfo(getTopContext()));
            }

          } else {
            // func_type is not static
            if (owner_cont.isForm(Container.FORM_TYPE)) {
              throw new CompileException("Cannot make a non static function call in a static method");
            }
          }

        }

        pushReduction(subaccess_cont);
      }
        break;

      default:
        throw new CompileException("Invalid Form(" + subaccess_type + ")");
      }
    }
      return null;

    case Container.FORM_OPSTACK_VAR:
    case Container.FORM_OBJMEMBER_VAR: {
      Reduction owner_reduce = popReduction(); // pop owner container of
                                               // subaccess
      Debug.assertion(owner_reduce != null, "Invalid Popped Reduction");
      Debug.assertion(owner_reduce.isContainer(), "Invalid Popped Reduction " + reduce);

      LOG.debug("owner_reduce=" + owner_reduce);

      Container owner_cont = (Container) owner_reduce;
      Debug.assertion(owner_cont.isTypeInitialized(), "Owner Container should be type initialized");

      if (subaccess_cont.getOwnerContainer() == null) {
        // member variable of Resolved Class will not have owner container
        LOG.debug("owner_cont: " + owner_cont);
        LOG.debug("subaccess_cont: " + subaccess_cont);
        subaccess_cont.initOwnerContainer(owner_cont);
      }

      if (subaccess_cont.isForm(Container.FORM_OBJMEMBER_VAR) && !subaccess_cont.isSingleton()) {
        if (owner_cont.isForm(Container.FORM_TYPE)) {
          throw new CompileException("Accessing non singleton member through class name is not allowed", node);
        }
      }

      if (child_node.isNodeId(JJTFUNCTIONDEF) && subaccess_type instanceof TMethodHandle) {
        // MethodHandle object has been already loaded in SymboleName or
        // Function
        LOG.debug("sub access child is closure");

        // funcdef_cont container is configured by SubSymboResolver-ASTInvoke.
        // 'apply_at_invoke_sub'
        Container funcdef_cont = child_node.getContainer();
        Debug.assertion(funcdef_cont != null, "funcdef_cont should be valid");
        Debug.assertion(funcdef_cont.isForm(Container.FORM_TYPE), "funcdef_cont should be type container");

        AbsType funcdef_cont_type = funcdef_cont.getType();
        Debug.assertion(funcdef_cont_type != null, "funcdef_cont_type should be valid");
        Debug.assertion(funcdef_cont_type instanceof AbsFuncType, "funcdef_cont_type should be AbsFuncType");

        if (!((AbsFuncType) funcdef_cont_type).is_static()) {
          if (owner_cont.isForm(Container.FORM_TYPE)) {
            throw new CompileException("Cannot call a non static function through type");
          }
        }

        pushReduction(subaccess_cont);
      } else if (node.isAssignTgt()) {
        LOG.debug("sub access is assign target, do not load");
        pushReduction(subaccess_cont);
      } else {
        if (!owner_cont.isForm(Container.FORM_TYPE)) {
          Debug.assertion(owner_cont.isForm(Container.FORM_OPSTACK_VAR), "owner_cont is opstack var");
        }

        Container loaded_obj = subaccess_cont.op().load_variable(subaccess_cont, owner_cont,
            new OpInfo(getTopContext()));
        Debug.assertion(loaded_obj.isForm(Container.FORM_OPSTACK_VAR), "loaded obj should be op stack var");
        // cpLoader.loadContainerChain(loaded_obj, 0);
        pushReduction(loaded_obj);

        checkDup(node, loaded_obj);
      }
    }
      return null;

    case Container.FORM_SPECIALTOKEN: {
      Reduction owner_reduce = popReduction(); // pop owner container of
                                               // subaccess
      Debug.assertion(owner_reduce != null, "Invalid Popped Reduction");
      Debug.assertion(owner_reduce.isContainer(), "Invalid Popped Reduction " + reduce);

      Container owner_cont = (Container) owner_reduce;
      Debug.assertion(owner_cont.isTypeInitialized(), "Owner Container should be type initialized");
      Debug.assertion(owner_cont.getType() instanceof TMapType, "owner_cont should be Map Type");

      Token sptoken = subaccess_cont.getSpecialToken();
      Debug.assertion(sptoken != null, "sptoken should be valid");

      if (sptoken.image.equals("length")) {

        MethodVisitor mv = getClosestFunContext().getMethodVisitor();
        mv.visitInsn(Opcodes.ARRAYLENGTH);

        Container op_cont = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
        op_cont.initializeType(subaccess_cont.getType());
        op_cont.setAssigned(true);
        pushReduction(op_cont);

      } else {
        throw new CompileException("Invalid Special token(" + sptoken.image + ")");
      }

    }
      return null;

    default:
      throw new CompileException("Invalid subaccess_cont Form(" + subaccess_cont + ")");
    }

  }

  public Container construct_methodhandle(Container func_type_cont, boolean is_applied_func) throws CompileException {
    OpInfo opinfo = new OpInfo(getTopContext());

    Container loaded_obj = null;

    Debug.assertion(func_type_cont.isTypeInitialized(), "Constant Container should be type initialized");

    switch (func_type_cont.getForm()) {
    case Container.FORM_TYPE:

      AbsType func_type = func_type_cont.getType();
      Debug.assertion(func_type.isForm(AbsType.FORM_FUNC), "func_type should be function form");
      Debug.assertion(func_type instanceof TContextFunc, "func_type should be instanceof TContextFunc");
      Debug.assertion(((TContextFunc) func_type).is_closure(), "func_type should be closure");

      AbsType closure_class_type = func_type.getOwnerType();
      Debug.assertion(closure_class_type != null, "Function parent should be valid");
      Debug.assertion(closure_class_type.isForm(AbsType.FORM_CLASS), "Function parent should be class");

      // MethodHandles.Lookup lookup = MethodHandles.lookup();
      LOG.info("INVOKESTATIC java/lang/invoke/MethodHandles/lookup");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup",
          "()Ljava/lang/invoke/MethodHandles$Lookup;", false);

      LOG.info("Class Mthd Dsc Str: {}", closure_class_type.getMthdDscStr());
      opinfo.mv.visitLdcInsn(org.objectweb.asm.Type.getType(closure_class_type.getMthdDscStr()));

      LOG.info("Function Name: {}", func_type.getName());
      opinfo.mv.visitLdcInsn(func_type.getName());

      // construct MethodType
      // return type
      AbsType reduce_type = ((AbsFuncType) func_type).getReturnType(cpLoader);
      load_class(reduce_type, opinfo);

      // parameters
      String arg_type_dsc = TypeUtil.getArgTypeDsc(func_type.getMthdDscStr());
      String[] arg_type_split = TypeUtil.splitArgTypeDsc(arg_type_dsc);
      Debug.assertion(arg_type_split != null, "arg_type_split should be valid");

      // create class array
      load_int(arg_type_split.length, opinfo);

      LOG.info("ANEWARRAY java/lang/Class");
      opinfo.mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");

      AbsType arg_type = null;
      for (int i = 0; i < arg_type_split.length; i++) {
        // arg_type =
        // (AbsType)cpLoader.findClassFull(TypeUtil.dsc2name(arg_type_split[i]));
        arg_type = (AbsType) cpLoader.findNamedTypeFull(TypeUtil.dsc2name(arg_type_split[i]));

        Debug.assertion(arg_type != null, "class for " + arg_type_split[i] + " should be exist");

        LOG.info("DUP");
        opinfo.mv.visitInsn(Opcodes.DUP); // dup class arry (cat. 1)
        load_int(i, opinfo);
        load_class(arg_type, opinfo);
        LOG.info("AASTORE");
        opinfo.mv.visitInsn(Opcodes.AASTORE);
      }

      LOG.info("INVOKESTATIC java/lang/invoke/MethodType/methodType");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType", "methodType",
          "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false);
          // completed constructing MethodType

      // create Method Handle
      // mh = lookup.findVirtual( ..)
      LOG.info("INVOKEVIRTUAL java/lang/invoke/MethodHandles$Lookup/findVirtual");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual",
          "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);

      // currently, operand stack top is Method Handle

      // bind to closure context
      // create closure class intacne
      LOG.info("Opcodes.NEW {}", closure_class_type.getName());
      LOG.info("DUP");
      LOG.info("INVOKESPECIAL {}", closure_class_type.getName());
      opinfo.mv.visitTypeInsn(Opcodes.NEW, closure_class_type.getName());
      opinfo.mv.visitInsn(DUP); // dup closure class (cat. 1)
      opinfo.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, closure_class_type.getName(), "<init>", "()V", false);

      // copy context variables to closure class
      generateClosureContextCopyInst((TContext) closure_class_type, opinfo);

      LOG.info("INVOKEVIRTUAL java/lang/invoke/MethodHandle/bindTo");
      opinfo.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "bindTo",
          "(Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);

      TMethodHandle mh_type = TMethodHandle.getInstance(cpLoader);
      Debug.assertion(mh_type != null, "mh_type should not be invalid");
      mh_type.setFuncSignatureDesc(((TContextFunc) func_type).getFuncSignatureDesc());

      loaded_obj = new Container("anonymous func", Container.FORM_OPSTACK_VAR, true, false);
      loaded_obj.initializeType((AbsType) mh_type);

      break;

    default:
      throw new CompileException(
          "This Container Type(" + func_type_cont.getFormString() + ") does not support load variable");

    }

    return loaded_obj;
  }

  public Container invoke_methodhandle(TMethodHandle mh_type) throws CompileException {
    MethodVisitor mv = getClosestFunContext().getMethodVisitor();

    Container loaded_obj = null;

    LOG.debug("Method Handle Call : " + mh_type);

    FuncSignatureDesc funcsig = mh_type.getFuncSignatureDesc();
    Debug.assertion(funcsig != null, "fucsig should be valid");

    LOG.info("INVOKEVIRTUAL java/lang/invoke/MethodHandle/invoke");
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", funcsig.getMthdDscStr(),
        false);

    AbsType rettype = funcsig.getReturnType();
    Debug.assertion(rettype != null, "rettype should be valid");

    loaded_obj = new Container("anonymous", Container.FORM_OPSTACK_VAR, true, false);
    loaded_obj.initializeType(rettype);
    return loaded_obj;
  }

  private void load_class(AbsType class_type, OpInfo opinfo) throws CompileException {
    Debug.assertion(class_type.isForm(AbsType.FORM_CLASS), "class_type should be Class");

    if (class_type instanceof TPrimitiveClass) {
      TPrimitiveClass primtype = (TPrimitiveClass) class_type;

      LOG.info("Load Primitive Type({} from {})", primtype.getName(), primtype.getClassName());
      opinfo.mv.visitFieldInsn(Opcodes.GETSTATIC, primtype.getClassName(), "TYPE", "Ljava/lang/Class;");
    } else {
      LOG.info("Load Class({})", class_type.getMthdDscStr());
      opinfo.mv.visitLdcInsn(org.objectweb.asm.Type.getType(class_type.getMthdDscStr()));
    }
  }

  private void load_int(int val, OpInfo opinfo) throws CompileException {
    Container param_size_cont = new Container("anonymous", Container.FORM_CONSTANT_VAR, true, false);
    param_size_cont.initializeType((AbsType) cpLoader.findClassFull(TPrimitiveClass.NAME_INT));
    param_size_cont.setContainerObject(new Integer(val));
    param_size_cont.setAssigned(true);
    param_size_cont.op().load_constant(param_size_cont, opinfo);
  }

  public void invoke_functiondef(AbsType functiontype) throws CompileException {
    MethodVisitor mv = getClosestFunContext().getMethodVisitor();

    Debug.assertion(functiontype.isForm(AbsType.FORM_FUNC),
        "Type form should be function(" + functiontype.getForm() + ")");

    AbsType classType = functiontype.getOwnerType();
    Debug.assertion(classType != null, "Parent Class Type should not be null");

    String mthdDsc = functiontype.getMthdDscStr();
    Debug.assertion(mthdDsc != null, "Invalid Method Descriptor");

    if (((AbsFuncType) functiontype).is_constructor()) {
      LOG.debug("Instantiate Class   :" + classType.getName());
      LOG.debug("Instantiate Constructor:" + functiontype);

      // 'New' Instruction is added in ASTSymbolName

      //// Compiled Instruction
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classType.getName(), functiontype.getName(), mthdDsc, false);
      //// End
      Container class_obj = new Container("anonymous", Container.FORM_OPSTACK_VAR, false, false);
      class_obj.initializeType(classType);
      pushReduction(class_obj);

    } else {
      LOG.debug("Function Call Class   :" + classType.getName());
      LOG.debug(
          "Function Call Function:" + functiontype + " is_static:" + ((AbsFuncType) functiontype).is_static());

      if (((AbsFuncType) functiontype).is_static()) {
        //// Compiled Instruction
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, classType.getName(), functiontype.getName(), mthdDsc, false);
        //// End
      } else if (((AbsClassType) classType).isInterface()) {
        //// Compiled Instruction
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, classType.getName(), functiontype.getName(), mthdDsc, true);
        //// End
      } else {
        //// Compiled Instruction
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classType.getName(), functiontype.getName(), mthdDsc, false);
        //// End
      }

      AbsType reduce_type = ((AbsFuncType) functiontype).getReturnType(cpLoader);

      Debug.assertion(reduce_type != null, "reduce_type should not be invalid");

      // push return type
      Container func_ret = new Container("ret_" + functiontype.getName(), Container.FORM_OPSTACK_VAR, false, false);
      func_ret.initializeType(reduce_type);
      pushReduction(func_ret);
    }

  }

  public void invoke_variablecall(AbsType functiontype) throws CompileException {
    MethodVisitor mv = getClosestFunContext().getMethodVisitor();

    Debug.assertion(functiontype.isForm(AbsType.FORM_FUNC),
        "Type form should be function(" + functiontype.getForm() + ")");

    AbsType classType = functiontype.getOwnerType();
    Debug.assertion(classType != null, "Parent Class Type should not be null");

    String mthdDsc = functiontype.getMthdDscStr();
    Debug.assertion(mthdDsc != null, "Invalid Method Descriptor");

    LOG.debug("Invoking Constructor:" + functiontype);

    //// Compiled Instruction
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classType.getName(), functiontype.getName(), mthdDsc, false);

    AbsType reduce_type = ((AbsFuncType) functiontype).getReturnType(cpLoader);

    Debug.assertion(reduce_type != null, "reduce_type should not be invalid");

    // push return type
    Container func_ret = new Container("ret_" + functiontype.getName(), Container.FORM_OPSTACK_VAR, false, false);
    func_ret.initializeType(reduce_type);
    pushReduction(func_ret);

  }

  @Override
  public Object visit(ASTInvoke node, Object data) throws CompileException {

    dump_reduction_stack();

    Reduction reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Reduction should be container");

    Container cont = (Container) reduce;
    AbsType type = cont.getType();

    switch (cont.getForm()) {
    case Container.FORM_TYPE:
      Debug.assertion(type.isForm(AbsType.FORM_FUNC), "type form should be Function, but " + type);
      invoke_functiondef(type);
      break;
    case Container.FORM_CONSTANT_VAR:
    case Container.FORM_FUNSTACK_VAR:
    case Container.FORM_OPSTACK_VAR:
    case Container.FORM_OBJMEMBER_VAR:
      if (type instanceof TMethodHandle) {
        Container loaded_obj = invoke_methodhandle((TMethodHandle) type);
        pushReduction(loaded_obj);
      } else if (type instanceof AbsClassType && (cont.getName().equals("this") || cont.getName().equals("super"))) {
        Container node_cont = node.getContainer();
        Debug.assertion(node_cont != null, "node_cont should be valid");
        Debug.assertion(node_cont.isForm(Container.FORM_TYPE), "node_cont should be type container, but " + node_cont);

        AbsType const_type = node_cont.getType();
        Debug.assertion(const_type != null, "const_type should be valid");

        invoke_variablecall(const_type);
      } else {
        throw new CompileException("Invalid invoking Form(" + cont + ")");
      }
      break;
    default:
      throw new CompileException("Invalid access_cont Form(" + cont + ")");
    }

    return null;
  }

  @Override
  public Object visit(ASTMapAccess node, Object data) throws CompileException {

    Reduction reduce = popReduction(); // pop key type
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Popped Reduction should be Container, but " + reduce);

    Container key_cont = (Container) reduce;

    reduce = popReduction(); // pop src type
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Popped Reduction should be Container, but " + reduce);

    Container src_cont = (Container) reduce;
    Debug.assertion(src_cont.isTypeInitialized(), "src_cont should be type initialized");
    AbsType src_type = src_cont.getType();

    OpInfo opinfo = new OpInfo(getTopContext());
    Container ret_obj = null;
    AbsType map_type = null;
    Container maptype_cont = null;

    if (src_cont.isForm(Container.FORM_TYPE)) {

      LangUnitNode sub_ref_node = (LangUnitNode) node.jjtGetParent();
      Debug.assertion(sub_ref_node.isNodeId(JJTSUBREFERENCE), "ref_node should be subref node");

      LOG.debug("src_type=" + src_type);
      if (isNextSubRefChildNodeid(sub_ref_node, JJTMAPACCESS)) {
        // [case-01]
        // map creation - int[10][10][10] : creating 'int[10][10][10]' in one
        // time
        // v v
        // int[10][10][10] or int[10][10][10] : Non Last case

        map_type = cpLoader.findMapType(src_type, 1);
        Debug.assertion(map_type != null, "map_type should be valid");

        // array preparation is done in 'prepare_multidimension_map_creation()'

        // length value loading instruction was already added
        LOG.debug("IASTORE");
        opinfo.mv.visitInsn(Opcodes.IASTORE);

        LOG.info("DUP");
        opinfo.mv.visitInsn(Opcodes.DUP); // dup class arry (cat. 1)

        // index for next array element( 1 ~ )
        int idx = ((TMapType) map_type).getDimension();
        LOG.info("Load Map Dimension Index:{}", idx);
        load_int(idx, opinfo);

        maptype_cont = map_type.getTypeContainer();
        pushReduction(maptype_cont);
      } else {
        maptype_cont = node.getContainer();
        Debug.assertion(maptype_cont != null, "maptype_cont should be valid");
        Debug.assertion(maptype_cont.isTypeInitialized(), "maptype_cont should be type initialized");

        map_type = maptype_cont.getType();
        Debug.assertion(map_type != null, "map_type should be valid");

        if (!(src_type instanceof TMapType)) {
          // [case-02]
          // map creation - int[10][][] : creating 10 * int[][][]
          // * int[][10]-> is not allowed grammar
          ret_obj = map_type.op().map_create(maptype_cont, key_cont, opinfo);
          pushReduction(ret_obj);
        } else {
          // [case-03] : 'case-01' ends here.
          // V
          // map creation-int[10][10][10] : final map dim

          LOG.debug("IASTORE");
          opinfo.mv.visitInsn(Opcodes.IASTORE);

          LOG.info("INVOKESTATIC  java/lang/reflect/Array/newInstance");
          opinfo.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/reflect/Array", "newInstance",
              "(Ljava/lang/Class;[I)Ljava/lang/Object;", false);

          LOG.info("CAST to {}", map_type.getName());
          // ... sometims checkcast use class name, some times it use method
          // descriptor format
          // .. which one is correct ?
          opinfo.mv.visitTypeInsn(Opcodes.CHECKCAST, map_type.getMthdDscStr());

          Container anony_map = new Container("anonymous", Container.FORM_OPSTACK_VAR, false, false);
          anony_map.initializeType(map_type);
          anony_map.setAssigned(true);

          pushReduction(anony_map);
        }
      }
    } else {

      if (node.isAssignTgt()) {

        if (!(src_type instanceof TMapType)) {
          throw new CompileException("Map Assign is allowed only for map type, but " + src_type);
        }

        LOG.debug("MapAccess for Assign Target(src_cont:" + src_cont + ", key_cont: " + key_cont + ")");

        AbsType map_ele_type = ((TMapType) src_type).getElementType();
        Debug.assertion(map_ele_type != null, "map_ele_type should be valid");

        Container map_ele_cont = new Container("anonymous", Container.FORM_MAPELEMENT_VAR, false, false);
        map_ele_cont.initializeType(map_ele_type);
        map_ele_cont.initOwnerContainer(src_cont);

        ret_obj = map_ele_cont;

      } else {
        LOG.debug("MapAccess for LOAD(src_cont:" + src_cont + ", key_cont: " + key_cont + ")");

        if (node.isSetDup2()) {
          opinfo.mv.visitInsn(Opcodes.DUP2);
        }

        ret_obj = src_cont.op().map_access(src_cont, key_cont, opinfo);
      }

      pushReduction(ret_obj);

      checkDup(node, ret_obj);
    }

    return null;
  }

  private int cntSubRefChildNodeIdChain(LangUnitNode node, int node_id) throws CompileException {
    Debug.assertion(node.isNodeId(JJTREFERENCE) || node.isNodeId(JJTSUBREFERENCE),
        "node should be reference or sub reference, but(" + node + ")");

    int cnt = 0;

    LangUnitNode subref_node = node;
    LangUnitNode subaccess_node = null;

    for (int i = 0; i < CompilerLoader.MAX_SUB_REF_LEVEL; i++) {
      subref_node = getNextChildSubRefeNode(subref_node);
      LOG.debug("getNextChildSubRefeNode:" + subref_node);
      if (subref_node == null) {
        return cnt;
      }

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

      if (!subaccess_node.isNodeId(node_id)) {
        return cnt;
      }

      cnt++;
    }

    throw new CompileException("Maximum Sub Referencing Level(" + CompilerLoader.MAX_SUB_REF_LEVEL + " is excceeded");

  }

  @Override
  public Object visit(ASTSymbolName node, Object data) throws CompileException {

    TContext top_context = getTopContext();
    Debug.assertion(top_context != null, "Top Context should not be null");

    Container symbol_cont = node.getContainer();
    Debug.assertion(symbol_cont != null, "SymbolName Container should not be null ");

    AbsType symbolcont_type = symbol_cont.getType();

    LangUnitNode parent_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(parent_node != null, "parent_symbol should be valid");
    Debug.assertion(
        parent_node.isNodeId(JJTACCESS) || parent_node.isNodeId(JJTSUBACCESS) || parent_node.isNodeId(JJTPARAMETER),
        "parent_symbol should be access/subaccess/parameter(" + parent_node + ")");

    switch (top_context.getForm()) {
    case AbsType.FORM_CLASS: {
      registerClassMemberVar((TContextClass) top_context, symbol_cont);
    }
      break;

    case AbsType.FORM_FUNC:
    case AbsType.FORM_STREAM: {

      TContextFunc func_ctx = getClosestFunContext();
      MethodVisitor mv = func_ctx.getMethodVisitor();

      switch (symbol_cont.getForm()) {
      case Container.FORM_TYPE: {

        if (symbolcont_type.isForm(AbsType.FORM_FUNC)) {

          // this member function call
          if (node.isDfltContextMember()) {
            LOG.info("ALOAD 0 for This Object");
            mv.visitVarInsn(Opcodes.ALOAD, 0);

            pushDfltContextMemberContainer();
          }

          // constructor
          if (((AbsFuncType) symbolcont_type).is_constructor()) {
            // New for constructor call
            AbsType classType = symbolcont_type.getOwnerType();
            Debug.assertion(classType != null, "class type should not be invalid");
            Debug.assertion(classType.isForm(AbsType.FORM_CLASS), "class type should Class Form");

            //// Compiled Instruction
            mv.visitTypeInsn(Opcodes.NEW, classType.getName());
            LOG.info("NEW {}", classType.getName());
            mv.visitInsn(DUP); // duplicating class (cat. 1)
            LOG.info("DUP");
            //// End
          }
        } else {
          // do nothing for package, class
          Debug.assertion(symbolcont_type.isForm(AbsType.FORM_PKG) || symbolcont_type.isForm(AbsType.FORM_CLASS),
              "symbolcont_type is package/class/function");
        }

      }
        break;

      case Container.FORM_OBJMEMBER_VAR:
      case Container.FORM_FUNSTACK_VAR: {

        if (node.isDfltContextMember() && !symbol_cont.isSingleton()) {
          LOG.info("ALOAD 0 for This Object");
          mv.visitVarInsn(Opcodes.ALOAD, 0);

          pushDfltContextMemberContainer();
        }

      }
        break;

      case Container.FORM_OPSTACK_VAR:
      case Container.FORM_CONSTANT_VAR:
      case Container.FORM_SPECIALTOKEN: {
        // do nothing - simply push symbol container
      }
        break;

      default:
        throw new CompileException("Not Supported symbol_cont form " + symbol_cont);
      }

    }
      break;

    default: {
      throw new CompileException("symbol node does not have container in " + top_context);
    }
    }

    pushReduction(symbol_cont); // symbol node always push container
    return null;
  }

  private void pushDfltContextMemberContainer() throws CompileException {
    Container dfltctx_cont = getTopContext().getChildVariableWithVarIdx(0);
    Debug.assertion(dfltctx_cont != null, "dfltctx_cont should be valid");
    Container opstack_cont = dfltctx_cont.getOpStackClone(dfltctx_cont.getName());
    Debug.assertion(opstack_cont != null, "opstack_cont should be valid");
    pushReduction(opstack_cont);
  }

  private void registerClassMemberVar(TContextClass class_ctx, Container container) throws CompileException {
    ClassWriter cw = class_ctx.getClassWriter();
    Debug.assertion(cw != null, "ClassWriter should not be null");

    AbsType field_type = container.getType();
    Debug.assertion(field_type != null, "Container Type should not be null");

    //// Compiled Instruction
    LOG.debug(
        "Adding Field(" + container.name + ":" + field_type.getMthdDscStr() + ") in (" + class_ctx.getName() + ")");

    int access = 0;
    access |= Opcodes.ACC_PUBLIC;
    // access |= Opcodes.ACC_PROTECTED;

    if (container.isSingleton()) {
      access |= Opcodes.ACC_STATIC;
    }

    cw.visitField(access, container.name, field_type.getMthdDscStr(), null, null).visitEnd();
    //// End
  }

  @Override
  public Object visit(ASTConstant node, Object data) throws CompileException {

    Container cont = node.getContainer();
    LOG.debug("AST Constant: " + cont);
    pushReduction(cont);

    return null; // constant node always push container
  }

  @Override
  public Object visit(ASTJumpExpr node, Object data) throws CompileException {

    Token t = node.getAstToken();
    Debug.assertion((t != null), "AstToken should not be null");

    switch (t.kind) {
    case ParserConstants.RETURN:
      return_compilation(node);
      break;

    case ParserConstants.THROW:
      throw_compilation(node);
      break;

    case ParserConstants.CONTINUE:
      continue_compilation(node);
      break;

    case ParserConstants.BREAK:
      break_compilation(node);
      break;

    default:
      throw new CompileException("JumpExpr(" + t.image + ") is not defined");

    }

    return null;
  }

  private void break_compilation(ASTJumpExpr node) throws CompileException {
    LangUnitNode func_node = node.getClosestParent(JJTFUNCTIONDEF);
    Debug.assertion(func_node != null, "func_node should be valid");

    LangUnitNode breakrange_node = node.getClosestParentsUntil(new int[] { JJTFORBODY }, func_node);

    if (breakrange_node == null) {
      throw new CompileException("'continue' cannot be used outside of a loop", node);
    }

    if (breakrange_node.isNodeId(JJTFORBODY)) {
      LangUnitNode loopimpl_node = (LangUnitNode) breakrange_node.jjtGetParent();
      Debug.assertion(loopimpl_node != null, "loopimpl_node should be valid");

      Label loop_end_label = loopimpl_node.getLoopEndLabel();
      Debug.assertion(loop_end_label != null, "loop_end_label should be valid");

      OpInfo opinfo = new OpInfo(getTopContext());
      opinfo.mv.visitJumpInsn(Opcodes.GOTO, loop_end_label);

      pushReduction(new Control(Control.FORM_BREAK));

    } else {
      throw new CompileException("Invalid Node(" + ParserConstants.tokenImage[breakrange_node.getNodeId()] + ")");
    }
  }

  private void continue_compilation(ASTJumpExpr node) throws CompileException {
    LangUnitNode func_node = node.getClosestParent(JJTFUNCTIONDEF);
    Debug.assertion(func_node != null, "func_node should be valid");

    LangUnitNode forbody_node = node.getClosestParentUntil(JJTFORBODY, func_node);
    if (forbody_node == null) {
      throw new CompileException("continue cannot be used outside of a loop", node);
    }

    LangUnitNode loopimpl_node = (LangUnitNode) forbody_node.jjtGetParent();
    Debug.assertion(loopimpl_node != null, "loopimpl_node should be valid");

    Label loop_action_label = loopimpl_node.getLoopActionLabel();
    Debug.assertion(loop_action_label != null, "loop_action_label should be valid");

    OpInfo opinfo = new OpInfo(getTopContext());
    opinfo.mv.visitJumpInsn(Opcodes.GOTO, loop_action_label);

    pushReduction(new Control(Control.FORM_CONTINUE));
  }

  private void return_compilation(ASTJumpExpr node) throws CompileException {
    TContextFunc func_context = getClosestFunContext();
    AbsType func_ret_type = ((AbsFuncType) func_context).getReturnType(cpLoader);
    Debug.assertion(func_ret_type != null, "Reduce Type is not defined");
    LOG.debug("REDUCE TYPE:" + func_ret_type);

    OpInfo opinfo = new OpInfo(getTopContext());

    switch (node.jjtGetNumChildren()) {
    case 0:
      LOG.info("RETURN with no return value");
      opinfo.mv.visitInsn(RETURN);
      break;

    case 1:
      Reduction reduce = this.popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);
      Container ret_cont = (Container) reduce;
      Debug.assertion(ret_cont.isTypeInitialized(), "Reducing Type is not initialized in the function signature");

      LOG.debug("CONT TYPE:" + ret_cont.getType());

      Debug.assertion(!ret_cont.isForm(Container.FORM_TYPE), "ret_cont should not be type container");

      ret_cont.getType().op().return_variable(ret_cont, opinfo);
      break;

    default:
      throw new CompileException("Invalid Return Expression child");
    }

    pushReduction(new Control(Control.FORM_RETURN));
  }

  private void throw_compilation(ASTJumpExpr node) throws CompileException {
    TContextFunc func_context = getClosestFunContext();
    MethodVisitor mv = func_context.getMethodVisitor();

    switch (node.jjtGetNumChildren()) {
    case 1:
      Reduction reduce = this.popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);
      Container throw_cont = (Container) reduce;
      Debug.assertion(throw_cont.isTypeInitialized(), "Throw Type is not initialized in the function signature");

      LOG.debug("CONT TYPE:" + throw_cont.getType());
      Debug.assertion(!throw_cont.isForm(Container.FORM_TYPE), "throw_cont should not be type container");

      LOG.debug("ATHROW");
      mv.visitInsn(Opcodes.ATHROW);
      break;

    default:
      throw new CompileException("Invalid Throw Expression child");
    }

    pushReduction(new Control(Control.FORM_THROW));
  }

  @Override
  public Object visit(ASTUnary node, Object data) throws CompileException {

    Token t = node.getAstToken();
    Debug.assertion((t != null), "AstToken should not be null");

    Reduction reduce = null;
    Container cont = null;
    Container opstack_cont = null;

    AbsType type = null;

    switch (t.kind) {
    case ParserConstants.PLUS_PLUS:
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isContainer(), "Popped Reduction should be Container, but " + reduce);
      cont = (Container) reduce;
      Debug.assertion(cont.isTypeInitialized(), "type should be initialized");
      type = cont.getType();

      opstack_cont = type.op().unary_plusplus(cont, new OpInfo(getTopContext()));
      Debug.assertion(opstack_cont != null, "opstack_cont should be valid");
      pushReduction(opstack_cont);
      break;

    case ParserConstants.MINUS_MINUS:
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isContainer(), "Popped Reduction should be Container, but " + reduce);
      cont = (Container) reduce;
      Debug.assertion(cont.isTypeInitialized(), "type should be initialized");
      type = cont.getType();

      opstack_cont = type.op().unary_minusminus(cont, new OpInfo(getTopContext()));
      Debug.assertion(opstack_cont != null, "opstack_cont should be valid");
      pushReduction(opstack_cont);
      break;

    case ParserConstants.MINUS:
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isContainer(), "Popped Reduction should be Container, but " + reduce);
      cont = (Container) reduce;
      Debug.assertion(cont.isTypeInitialized(), "type should be initialized");
      type = cont.getType();

      opstack_cont = type.op().unary_negative(cont, new OpInfo(getTopContext()));
      Debug.assertion(opstack_cont != null, "opstack_cont should be valid");
      pushReduction(opstack_cont);
      break;

    case ParserConstants.UNARY_NOT:
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isContainer(), "Popped Reduction should be Container, but " + reduce);
      cont = (Container) reduce;
      Debug.assertion(cont.isTypeInitialized(), "type should be initialized");
      type = cont.getType();

      opstack_cont = type.op().not(cont, new OpInfo(getTopContext()));
      Debug.assertion(opstack_cont != null, "opstack_cont should be valid");
      pushReduction(opstack_cont);
      break;

    case ParserConstants.UNARY_INVERSE:
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isContainer(), "Popped Reduction should be Container, but " + reduce);
      cont = (Container) reduce;
      Debug.assertion(cont.isTypeInitialized(), "type should be initialized");
      type = cont.getType();

      opstack_cont = type.op().inverse(cont, new OpInfo(getTopContext()));
      Debug.assertion(opstack_cont != null, "opstack_cont should be valid");
      pushReduction(opstack_cont);
      break;

    default:
      throw new CompileException("Unary(" + t.image + ") is not defined");
    }

    dump_reduction_stack();
    return null;
  }

  @Override
  public Object visit(ASTPostfix node, Object data) throws CompileException {

    Token t = node.getAstToken();
    Debug.assertion((t != null), "AstToken should not be null");

    Reduction reduce = null;
    AbsType type = null;
    Container cont = null;
    Container opstack_cont = null;

    switch (t.kind) {
    case ParserConstants.PLUS_PLUS:
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isContainer(), "Popped Reduction should be Container, but " + reduce);
      cont = (Container) reduce;
      Debug.assertion(cont.isTypeInitialized(), "type should be initialized");
      type = cont.getType();

      opstack_cont = type.op().postfix_plusplus(cont, new OpInfo(getTopContext()));

      Debug.assertion(opstack_cont != null, "opstack_cont should be valid");
      pushReduction(opstack_cont);
      break;

    case ParserConstants.MINUS_MINUS:
      reduce = popReduction();
      Debug.assertion(reduce != null, "Invalid Popped Reduction");
      Debug.assertion(reduce.isContainer(), "Popped Reduction should be Container, but " + reduce);
      cont = (Container) reduce;
      Debug.assertion(cont.isTypeInitialized(), "type should be initialized");
      type = cont.getType();

      opstack_cont = type.op().postfix_minusminus(cont, new OpInfo(getTopContext()));

      Debug.assertion(opstack_cont != null, "opstack_cont should be valid");
      pushReduction(opstack_cont);
      break;

    default:
      throw new CompileException("Unary(" + t.image + ") is not defined");
    }

    dump_reduction_stack();
    return null;

  }

  @Override
  public Object visit(ASTAssignment node, Object data) throws CompileException {

    Reduction reduce = null;

    Container l_value = null;
    Container r_value = null;
    Operation op = null;

    // check_numof_childnode(3, node);
    Debug.assertion(reduction_stack.size() >= 3, "reduction_stack.size(" + reduction_stack.size() + ") should be >=3");

    dump_reduction_stack();

    reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);
    r_value = (Container) reduce;
    Debug.assertion(r_value.isForm(Container.FORM_OPSTACK_VAR), "r_value should be opstack var(" + r_value + ")");

    reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isOperation(), "Popped Reduction (" + reduce + ") should be operation");
    op = (Operation) reduce;

    reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);
    l_value = (Container) reduce;
    // Debug.assertion(!l_value.isForm(Container.FORM_OPSTACK_VAR), "l_value
    // should not be opstack var("+l_value+")");

    OpInfo opinfo = new OpInfo(getTopContext());

    Container assign_op_ret = op.assign_op(l_value, r_value, opinfo, cpLoader);
    Debug.assertion(reduce != null, "Invalid assign_op( ) ret");

    pushReduction(assign_op_ret);

    dump_reduction_stack();

    return null;
  }

  @Override
  public Object visit(ASTAssignmentOperator node, Object data) throws CompileException {

    Operation retObj = null;
    Token t;
    try {
      t = node.getAstToken();
      retObj = new Operation(t.kind, node, getTopContext());
    } catch (CompileException e) {
      if (!e.node_initialized()) {
        e.set_node(node);
      }
      throw e;
    }

    pushReduction(retObj);
    return null;
  }

  @Override
  public Object visit(ASTLogicalOR node, Object data) throws CompileException {
    Container retObj = dual_child_op(node, data, cpLoader);
    pushReduction(retObj);
    return null;
  }

  private void prepareLogicalOr(LangUnitNode node) throws CompileException {

    LOG.debug("prepareLogicalOr");

    Reduction reduce = topReduction();
    Debug.assertion(reduce != null, "reduce should be valid");
    Debug.assertion(reduce.isContainer(), "reduce should be Container");

    Container cont = (Container) reduce;
    Debug.assertion(cont.isTypeInitialized(), "cont should be Type initialized");

    AbsType type = cont.getType();
    Debug.assertion(type.isName(TPrimitiveClass.NAME_BOOL), "type should be bool, but " + cont, node);

    OpInfo opinfo = new OpInfo(getTopContext());
    opinfo.op_node = node;
    type.op().prepare_logical_or(opinfo);

    return;
  }

  @Override
  public Object visit(ASTLogicalORHdr node, Object data) throws CompileException {

    LangUnitNode logicalor_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(logicalor_node != null, "logicalor_node should be valid");
    Debug.assertion(logicalor_node.isNodeId(JJTLOGICALOR), "logicalor_node should be valid");

    Label logicalop_br_label = new Label();
    logicalor_node.setLogicalOpBrLabel(logicalop_br_label);

    return null;
  }

  @Override
  public Object visit(ASTLogicalAND node, Object data) throws CompileException {

    Container retObj = dual_child_op(node, data, cpLoader);

    pushReduction(retObj);
    return null;
  }

  @Override
  public Object visit(ASTLogicalANDHdr node, Object data) throws CompileException {

    LangUnitNode logicaland_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(logicaland_node != null, "logicaland_node should be valid");
    Debug.assertion(logicaland_node.isNodeId(JJTLOGICALAND), "logicaland_node should be valid");

    Label logicalop_br_label = new Label();
    logicaland_node.setLogicalOpBrLabel(logicalop_br_label);

    return null;
  }

  private void prepareLogicalAnd(LangUnitNode node) throws CompileException {

    LOG.debug("prepareLogicalAnd");

    Reduction reduce = topReduction();
    Debug.assertion(reduce != null, "reduce should be valid");
    Debug.assertion(reduce.isContainer(), "reduce should be Container");

    Container cont = (Container) reduce;
    Debug.assertion(cont.isTypeInitialized(), "cont should be Type initialized");

    AbsType type = cont.getType();
    Debug.assertion(type.isName(TPrimitiveClass.NAME_BOOL), "type should be bool, but " + cont, node);

    OpInfo opinfo = new OpInfo(getTopContext());
    opinfo.op_node = node;
    type.op().prepare_logical_and(opinfo);

    return;
  }

  @Override
  public Object visit(ASTInclusiveOR node, Object data) throws CompileException {
    Container retObj = dual_child_op(node, data, cpLoader);
    pushReduction(retObj);
    return null;
  }

  @Override
  public Object visit(ASTExclusiveOR node, Object data) throws CompileException {
    Container retObj = dual_child_op(node, data, cpLoader);
    pushReduction(retObj);
    return null;
  }

  @Override
  public Object visit(ASTAND node, Object data) throws CompileException {
    Container retObj = dual_child_op(node, data, cpLoader);
    pushReduction(retObj);
    return null;
  }

  @Override
  public Object visit(ASTEquality node, Object data) throws CompileException {
    Container retObj = dual_child_op(node, data, cpLoader);
    pushReduction(retObj);
    return null;
  }

  @Override
  public Object visit(ASTRelational node, Object data) throws CompileException {
    Container retObj = dual_child_op(node, data, cpLoader);
    pushReduction(retObj);
    return null;
  }

  @Override
  public Object visit(ASTShift node, Object data) throws CompileException {
    Container retObj = dual_child_op(node, data, cpLoader);
    pushReduction(retObj);
    return null;
  }

  @Override
  public Object visit(ASTAdditive node, Object data) throws CompileException {

    Container retObj = dual_child_op(node, data, cpLoader);
    pushReduction(retObj);

    return null;
  }

  @Override
  public Object visit(ASTMultiplicative node, Object data) throws CompileException {

    Container retObj = dual_child_op(node, data, cpLoader);
    pushReduction(retObj);

    return null;
  }

  private Container dual_child_op(LangUnitNode node, Object data, CompilerLoader cpLoader) throws CompileException {
    Reduction reduce = null;

    Container ret = null;
    Container l_value = null;
    Container r_value = null;

    // check_numof_childnode(2, node);
    Debug.assertion(reduction_stack.size() >= 2, "Invalid reduction_stack.size(" + reduction_stack.size() + ")");

    reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);
    r_value = (Container) reduce;

    reduce = popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);
    l_value = (Container) reduce;

    Token t = node.getAstToken();
    Operation op = new Operation(t.kind, node, getTopContext());

    OpInfo opinfo = new OpInfo(getTopContext());
    opinfo.op_node = node;

    ret = op.non_assign_op(l_value, r_value, opinfo, cpLoader);

    Debug.assertion(ret != null, "Invalid non_assign_op ret");
    return ret;
  }

  @Override
  public Object visit(ASTIfExpr node, Object data) throws CompileException {

    MethodVisitor mv = getClosestFunContext().getMethodVisitor();
    Label ifend_label = node.getBrLabel();
    int num_child = node.jjtGetNumChildren();

    boolean is_def_ref = node.hasDefinitionRefernce();

    switch (num_child) {
    case 1: {
      Reduction if_case = popReduction();
      Debug.assertion(if_case != null, "Popped Reduction should not be invalid");

      if (is_def_ref) {
        pushReduction(new Control(Control.FORM_BRANCH));
      } else {
        throw new CompileException("else case is required", node);
      }

    }
      break;
    case 3: {
      Reduction else_case = popReduction();
      Debug.assertion(else_case != null, "Popped Reduction should not be invalid");

      Reduction if_case = popReduction();
      Debug.assertion(if_case != null, "Popped Reduction should not be invalid");

      if (is_def_ref) {

        if (else_case.isContainer()) {
          Container else_cont = (Container) else_case;
          AbsType else_type = else_cont.getType();
          Debug.assertion(else_type != null, "else_type should be valid");

          if (else_cont.isForm(Container.FORM_OPSTACK_VAR) && !else_type.isName(TPrimitiveClass.NAME_VOID)) {
            else_type.op().pop(else_cont, new OpInfo(getTopContext()));
          }
        }

        pushReduction(new Control(Control.FORM_BRANCH));
      } else {
        // do reduction type checking
        Debug.assertion(else_case.isContainer(), "Else case popped reduction should be container");
        AbsType elsecase_type = ((Container) else_case).getType();
        Debug.assertion(if_case.isContainer(), "If case popped reduction should be container");
        AbsType ifcase_type = ((Container) if_case).getType();

        if (!cpLoader.isCompatibleClass(ifcase_type, elsecase_type)
            && !cpLoader.isConvertibleClass(ifcase_type, elsecase_type)) {
          throw new CompileException(
              "If case reduction(" + ifcase_type + ") is not mismatched with else case (" + elsecase_type + ")");
        }
        // is this correct ..?
        Container ifelse_ret = new Container("if_else_reduce", Container.FORM_OPSTACK_VAR, false, false);
        ifelse_ret.initializeType(ifcase_type);
        pushReduction(ifelse_ret);
      }

    }
      break;
    default:
      throw new CompileException("Invalid Child Case(" + num_child + ")");
    }

    if (ifend_label != null) {
      mv.visitLabel(ifend_label);
    }

    return null;
  }

  @Override
  public Object visit(ASTElseExpr node, Object data) throws CompileException {

    MethodVisitor mv = getClosestFunContext().getMethodVisitor();

    Label else_label = node.getBrLabel();

    if (else_label != null) {
      mv.visitLabel(else_label);
    }

    return null;
  }

  @Override
  public Object visit(ASTIfCaseExpr node, Object data) throws CompileException {

    MethodVisitor mv = getClosestFunContext().getMethodVisitor();
    Label ifend_label = node.getBrLabel();

    if (node.hasDefinitionRefernce()) {
      Reduction reduce = topReduction();
      Debug.assertion(reduce != null, "Reduction should not be invalid");
      if (reduce.isContainer()) {
        Container cont = (Container) reduce;
        AbsType type = cont.getType();
        Debug.assertion(type != null, "type should be valid");

        if (cont.isForm(Container.FORM_OPSTACK_VAR) && !type.isName(TPrimitiveClass.NAME_VOID)) {
          type.op().pop(cont, new OpInfo(getTopContext()));

          popReduction(); // consume op stack var non void container
          pushReduction(new Control(Control.FORM_BRANCH));
        }
      }
    }

    if (ifend_label != null && node.isAliveCtrlFlow()) {
      mv.visitJumpInsn(GOTO, ifend_label);
    }

    return null;
  }

  @Override
  public Object visit(ASTIfCondExpr node, Object data) throws CompileException {

    Reduction reduce = popReduction();
    Debug.assertion(reduce != null, "Reductin should not be invalid");
    Debug.assertion(reduce.isContainer(), "Reductin should not be Container");

    AbsType red_type = ((Container) reduce).getType();
    Debug.assertion(red_type.isName(TPrimitiveClass.NAME_BOOL), "Reduction should be boolean type");

    MethodVisitor mv = getClosestFunContext().getMethodVisitor();

    LangUnitNode ifend_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(ifend_node != null, "ifend_node should not be invalid");
    Debug.assertion(ifend_node.isNodeId(JJTIFCASEEXPR), "parent node should be ifend expression");

    LangUnitNode ifexpr_node = (LangUnitNode) ifend_node.jjtGetParent();
    Debug.assertion(ifexpr_node != null, "ifexpr_node should not be invalid");
    Debug.assertion(ifexpr_node.isNodeId(JJTIFEXPR), "parent node should be ifexpr expression");

    int num_child = ifexpr_node.jjtGetNumChildren();

    Label else_label = null;
    Label ifend_label = null;

    if (num_child == 3) {
      // it has else case
      else_label = new Label();
      ifend_label = new Label();

      LangUnitNode else_node = (LangUnitNode) ifexpr_node.jjtGetChild(1);
      Debug.assertion(else_node != null, "ElseExpression node should not be invalid");
      Debug.assertion(else_node.isNodeId(JJTELSEEXPR), "parent node should be else expression");

      else_node.setBrLabel(else_label);
      ifexpr_node.setBrLabel(ifend_label);
      ifend_node.setBrLabel(ifend_label);

      //// Compiled Instruction
      mv.visitJumpInsn(Opcodes.IFEQ, else_label);
      LOG.debug("IFEQ");
    } else if (num_child == 1) {
      // it has 'no' else case
      ifend_label = new Label();
      ifexpr_node.setBrLabel(ifend_label);

      //// Compiled Instruction
      mv.visitJumpInsn(Opcodes.IFEQ, ifend_label);
      LOG.debug("IFEQ");

    } else {
      throw new CompileException("IFexpr child should be 2 or 4, but(" + num_child + ")");
    }

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

    LangUnitNode parent = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(parent != null, "parent should not be invalid");
    Debug.assertion(parent.isNodeId(JJTCSTYLELOOPEXPR), "parent should be c style loop");

    LangUnitNode for_body_node = parent.getChildren(1);
    Debug.assertion(for_body_node != null, "for_body_node should not be invalid");
    Debug.assertion(for_body_node.isNodeId(JJTFORBODY), "parent child 1  should be For Body");

    LangUnitNode for_action_node = parent.getChildren(2);
    Debug.assertion(for_action_node != null, "for_action_node should not be invalid");
    Debug.assertion(for_action_node.isNodeId(JJTFORACTION), "parent child 2  should be For Action");

    LangUnitNode for_cond_node = parent.getChildren(3);
    Debug.assertion(for_cond_node != null, "for_cond_node should not be invalid");
    Debug.assertion(for_cond_node.isNodeId(JJTFORCONDITION), "parent child 3  should be For Condition");

    OpInfo opinfo = new OpInfo(getTopContext());

    if (!node.hasDefinitionRefernce()) {
      // this loop generate list
      langstream_init(opinfo);
    }

    Label loop_body_label = new Label();
    Label loop_cond_label = new Label();
    Label loop_action_label = new Label();
    Label loop_end_label = new Label();

    parent.setLoopBodyLabel(loop_body_label);
    parent.setLoopCondLabel(loop_cond_label);
    parent.setLoopActionLabel(loop_action_label);
    parent.setLoopEndLabel(loop_end_label);

    opinfo.mv.visitJumpInsn(Opcodes.GOTO, loop_cond_label);
    opinfo.mv.visitLabel(loop_body_label);

    return null;
  }

  @Override
  public Object visit(ASTForBody node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 1, "ForBody should have one child");

    Reduction reduce = popReduction();
    Debug.assertion(reduce != null, "reduction should not be invalid");
    Debug.assertion(reduce.isContainer() || reduce.isControl() || reduce.isType(),
        "reduction should be container or control(" + reduce + ")");
    dump_reduction_stack();

    OpInfo opinfo = new OpInfo(getTopContext());
    langstream_ele_add(opinfo, reduce, node);

    LangUnitNode parent = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(parent != null, "parent should not be invalid");
    Debug.assertion(parent.isNodeId(JJTCSTYLELOOPEXPR), "parent should be c style loop");

    Label loop_action_label = parent.getLoopActionLabel();
    Debug.assertion(loop_action_label != null, "loop_action_label should be valid");

    // next node is loop action
    opinfo.mv.visitLabel(loop_action_label);
    return null;
  }

  @Override
  public Object visit(ASTForAction node, Object data) throws CompileException {

    MethodVisitor mv = getClosestFunContext().getMethodVisitor();

    if (node.jjtGetNumChildren() == 0) {
      // do nothing
    } else if (node.jjtGetNumChildren() == 1) {
      Reduction reduce = popReduction();
      Debug.assertion(reduce != null, "Reductin should not be invalid");
      Debug.assertion(reduce.isContainer(), "Reductin should be container");

      Container cont = (Container) reduce;
      Debug.assertion(cont.isTypeInitialized(), "cont should be type initialized");

      cont.getType().op().pop(cont, new OpInfo(getTopContext()));
    } else {
      throw new CompileException("This case is not considered");
    }

    LangUnitNode parent = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(parent != null, "parent should not be invalid");
    Debug.assertion(parent.isNodeId(JJTCSTYLELOOPEXPR), "parent should be c style loop");

    Label loop_cond_label = parent.getLoopCondLabel();
    Debug.assertion(loop_cond_label != null, "loop condition label should not be invalid");

    mv.visitLabel(loop_cond_label);

    return null;
  }

  @Override
  public Object visit(ASTForCondition node, Object data) throws CompileException {

    LangUnitNode parent = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(parent != null, "parent should not be invalid");
    Debug.assertion(parent.isNodeId(JJTCSTYLELOOPEXPR), "parent should be c style loop");

    Label loop_body_label = parent.getLoopBodyLabel();
    Debug.assertion(loop_body_label != null, "loop action label should not be invalid");

    OpInfo opinfo = new OpInfo(getTopContext());

    if (node.jjtGetNumChildren() == 0) {
      opinfo.mv.visitJumpInsn(Opcodes.GOTO, loop_body_label);

    } else if (node.jjtGetNumChildren() == 1) {
      Reduction reduce = popReduction();
      Debug.assertion(reduce != null, "Reductin should not be invalid");

      opinfo.mv.visitJumpInsn(Opcodes.IFNE, loop_body_label);
    } else {
      throw new CompileException("This case is not considered");
    }

    Label loop_end_label = parent.getLoopEndLabel();
    Debug.assertion(loop_end_label != null, "loop end label should not be invalid");
    opinfo.mv.visitLabel(loop_end_label);

    if (node.hasDefinitionRefernce()) { // this loop does not reduce instance
      pushReduction(new Control(Control.FORM_LOOP));
    } else { // this loop reduces list instance
      AbsType stream_type = langstream_finish(opinfo);

      Container stream_obj = new Container("anonymous stream", Container.FORM_OPSTACK_VAR, true, false);
      stream_obj.initializeType(stream_type);
      pushReduction(stream_obj); // reduced as a object
    }

    return null;
  }

  @Override
  public Object visit(ASTMatchExpr node, Object data) throws CompileException {

    Label match_end_label = node.getMatchEndLabel();
    Debug.assertion(match_end_label != null, "match_end_label should not be invalid");

    MethodVisitor mv = getClosestFunContext().getMethodVisitor();

    mv.visitLabel(match_end_label);

    int num_child = node.jjtGetNumChildren();

    boolean is_def_ref = node.hasDefinitionRefernce();

    LangUnitNode match_case_node = null;
    Reduction reduce = null;
    Container case_cont = null;
    AbsType case_type = null;

    for (int i = 1; i < num_child; i++) {
      match_case_node = (LangUnitNode) node.jjtGetChild(i);
      Debug.assertion(match_case_node != null, "match_case_node should not be invalid");
      Debug.assertion(match_case_node.isNodeId(JJTMATCHCASEEXPR), "match_case_node should be match case expression");

      reduce = popReduction();

      if (is_def_ref) {
        // it does not perform case element reduction checking
      } else {
        Debug.assertion(reduce.isContainer(), "Match Case poppped reduction should be Container");

        if (case_cont == null) {
          case_cont = (Container) reduce;
          Debug.assertion(case_cont.isTypeInitialized(), "Match Case Popped container should have type");
          case_type = case_cont.getType();
        } else {
          Debug.assertion(((Container) reduce).isTypeInitialized(), "Match Case Popped container should have type");
          if (!cpLoader.isCompatibleClass(case_type, ((Container) reduce).getType())
              && !cpLoader.isConvertibleClass(case_type, ((Container) reduce).getType())) {
            throw new CompileException("match case reduction(" + reduce + ") is conflicted", match_case_node);
          }
        }
      }
    }

    if (is_def_ref) {
      pushReduction(new Control(Control.FORM_BRANCH));
    } else {
      Debug.assertion(case_type != null, "Invalid Match Case reduction type");

      // is this correct ..?
      Container matchcase_ret = new Container("match_case_reduce", Container.FORM_OPSTACK_VAR, false, false);
      matchcase_ret.initializeType(case_type);
      pushReduction(matchcase_ret);
    }

    return null;
  }

  @Override
  public Object visit(ASTMatchHeadExpr node, Object data) throws CompileException {

    Debug.assertion(node.jjtGetNumChildren() == 1, "Match should have 1 child");
    Reduction reduce = popReduction();
    Debug.assertion(reduce.isContainer(), "In match( expr ), 'expr' should be container");
    Container tgt_cont = (Container) reduce;
    AbsType match_tgt_type = tgt_cont.getType();

    // ISSUE !! supporting other types..
    if (!match_tgt_type.isName(TPrimitiveClass.NAME_INT)) {
      throw new CompileException(
          "Not supported match parameter Type(" + match_tgt_type.getName() + "). Currently only support int");
    }

    LangUnitNode match_node = (LangUnitNode) node.jjtGetParent();
    Debug.assertion(match_node != null, "match_node should not be invalid");
    Debug.assertion(match_node.isNodeId(JJTMATCHEXPR), "match_node should be match expr");

    int child_num = match_node.jjtGetNumChildren();
    Debug.assertion(child_num > 1, "Invalid match_node child count(" + child_num + ")");

    MatchCaseList match_case_list = new MatchCaseList();

    Label case_label = null;
    Label dflt_label = null;
    Label end_label = new Label();

    match_node.setMatchEndLabel(end_label);

    LangUnitNode match_case_node = null;
    LangUnitNode match_case_head_node = null;
    LangUnitNode constant_node = null;
    Container case_const_container = null;
    int casse_const_value = 0;

    boolean is_dflt_defined = false;

    for (int i = 1; i < child_num; i++) {
      match_case_node = (LangUnitNode) match_node.jjtGetChild(i);
      Debug.assertion(match_case_node != null, "match_case_node should not be invalid");
      Debug.assertion(match_case_node.isNodeId(JJTMATCHCASEEXPR), "match_case_node should be match case expr");

      match_case_head_node = (LangUnitNode) match_case_node.jjtGetChild(0);
      Debug.assertion(match_case_head_node != null, "match_case_head_node should not be invalid");
      Debug.assertion(match_case_head_node.isNodeId(JJTMATCHCASEHEADEXPR),
          "match_case_head_node should be match case head expr");

      Token t = match_case_head_node.getAstToken();

      if (t.kind == ParserConstants.CASE) {

        Debug.assertion(match_case_head_node.jjtGetNumChildren() == 1,
            "Invalid match_case_head_node child num(" + match_case_head_node.jjtGetNumChildren() + ")");
        constant_node = (LangUnitNode) match_case_head_node.jjtGetChild(0);
        Debug.assertion(constant_node != null, "constant_node should not be invalid");
        Debug.assertion(constant_node.isNodeId(JJTCONSTANT), "constant_node should constant");

        case_const_container = constant_node.getContainer();
        Debug.assertion(case_const_container != null, "constant_node should not be invalid");

        if (!cpLoader.isCompatibleClass(case_const_container.getType(), match_tgt_type)
            && !cpLoader.isConvertibleClass(case_const_container.getType(), match_tgt_type)) {
          throw new CompileException("Invalid Case Contant Type(" + case_const_container.getType() + ")",
              constant_node);
        }

        casse_const_value = (Integer) case_const_container.getContainerObject();

        if (match_case_list.has(casse_const_value)) {
          throw new CompileException("case const value(" + casse_const_value + ") is duplicated", constant_node);
        }

        case_label = new Label();

        match_case_list.add(casse_const_value, case_label);

        match_case_head_node.setMatchCaseLabel(case_label);

      } else if (t.kind == ParserConstants.DFLT) {
        Debug.assertion(match_case_head_node.jjtGetNumChildren() == 0,
            "Invalid match_case_head_node child num(" + match_case_head_node.jjtGetNumChildren() + ")");

        if (dflt_label != null) {
          throw new CompileException("Match default is duplicated", match_case_head_node);
        }

        dflt_label = new Label();
        match_case_head_node.setMatchCaseLabel(dflt_label);
        match_case_head_node.setMatchEndLabel(end_label);

        is_dflt_defined = true;

      } else {
        throw new CompileException("Invalid match case token(" + t.image + ")");
      }

      match_case_node.setMatchEndLabel(end_label);

    }

    if (!node.hasDefinitionRefernce() && !is_dflt_defined) {
      throw new CompileException("default case is required for reducing match expression");
    }

    // match_case_list.dump();
    match_case_list.sort();

    // LOG.debug("-- after sorting--");
    // match_case_list.dump();
    int[] case_const_arr = match_case_list.getConstValList();
    Label[] case_label_arr = match_case_list.getLabelList();

    MethodVisitor mv = getClosestFunContext().getMethodVisitor();

    if (dflt_label != null) {
      mv.visitLookupSwitchInsn(dflt_label, case_const_arr, case_label_arr);
    } else {
      mv.visitLookupSwitchInsn(end_label, case_const_arr, case_label_arr);
    }

    return null;
  }

  /*
  private boolean isInCaseConstList(LinkedList<Integer> case_const_list, int v) {
    int size = case_const_list.size();

    for (int i = 0; i < size; i++) {
      if ((Integer) case_const_list.get(i) == v) {
        return true;
      }
    }

    return false;

  }
  */

  @Override
  public Object visit(ASTMatchCaseExpr node, Object data) throws CompileException {

    int matchcasebody_idx = node.getChildIdxWithId(JJTMATCHCASEBODYEXPR, 0);

    if (matchcasebody_idx != -1) {
      Label match_end_label = node.getMatchEndLabel();
      Debug.assertion(match_end_label != null, "match_end_label should not be invalid");

      MethodVisitor mv = getClosestFunContext().getMethodVisitor();

      if (node.hasDefinitionRefernce()) {
        Reduction reduce = topReduction();
        Debug.assertion(reduce != null, "Reduction should not be invalid");
        if (reduce.isContainer()) {
          Container cont = (Container) reduce;
          AbsType type = cont.getType();
          Debug.assertion(type != null, "type should be valid");

          if (cont.isForm(Container.FORM_OPSTACK_VAR) && !type.isName(TPrimitiveClass.NAME_VOID)) {
            type.op().pop(cont, new OpInfo(getTopContext()));

            popReduction(); // consume op stack var non void container
            pushReduction(new Control(Control.FORM_BRANCH));
          }
        }
      }

      mv.visitJumpInsn(GOTO, match_end_label);

    }

    return null;
  }

  @Override
  public Object visit(ASTMatchCaseHeadExpr node, Object data) throws CompileException {

    Label match_case_label = node.getMatchCaseLabel();
    Debug.assertion(match_case_label != null, "match_case_label should not be invalid");

    MethodVisitor mv = getClosestFunContext().getMethodVisitor();

    mv.visitLabel(match_case_label);

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

    return null;
  }

  @Override
  public Object visit(ASTExplicitCast node, Object data) throws CompileException {
    Container type_cont = node.getContainer();
    Debug.assertion(type_cont != null, "Cast Node should have Type Container");
    Debug.assertion(type_cont.isTypeInitialized(), "Cast Node container should have type");
    AbsType cast_type = type_cont.getType();

    // CAUTION: clear cast container(If we do not clear, Upper Access Node will
    // process..)
    node.setContainer(null); // clear container

    Reduction reduce = (Reduction) popReduction();
    Debug.assertion(reduce != null, "Invalid Popped Reduction");
    Debug.assertion(reduce.isContainer(), "Invalid Popped Reduction " + reduce);
    Container opstack_cont = (Container) reduce;
    Debug.assertion(opstack_cont.isForm(Container.FORM_OPSTACK_VAR), "Casting container should be opstack var");
    Debug.assertion(opstack_cont.isTypeInitialized(), "Casting container should be type initialized");

    if (!cast_type.equals(opstack_cont.getType())) { // do casting
      OpInfo opinfo = new OpInfo(getTopContext());
      cast_type.op().explicit_casting(type_cont, opstack_cont, opinfo);
      opstack_cont.initializeType(cast_type);
    }
    pushReduction(opstack_cont);

    return null;
  }

  @Override
  public Object visit(ASTLoopHdrExpr node, Object data) throws CompileException {

    TContext context = node.getBindContext();
    Debug.assertion(context != null, "Context should not be null");

    Debug.assertion(context.isForm(AbsType.FORM_STREAM),
        "Context Type should be stream, but " + context.getFormString(context.getForm()));

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
