package mezic.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;

import mezic.compiler.type.AbsClassType;
import mezic.compiler.type.AbsFuncType;
import mezic.compiler.type.AbsType;
import mezic.compiler.type.AbsTypeList;
import mezic.compiler.type.FuncSignatureDesc;
import mezic.compiler.type.OpBool;
import mezic.compiler.type.OpBooleanObj;
import mezic.compiler.type.OpByte;
import mezic.compiler.type.OpByteObj;
import mezic.compiler.type.OpChar;
import mezic.compiler.type.OpCharacterObj;
import mezic.compiler.type.OpDouble;
import mezic.compiler.type.OpDoubleObj;
import mezic.compiler.type.OpFloat;
import mezic.compiler.type.OpFloatObj;
import mezic.compiler.type.OpInt;
import mezic.compiler.type.OpIntegerObj;
import mezic.compiler.type.OpLong;
import mezic.compiler.type.OpLongObj;
import mezic.compiler.type.OpLvalue;
import mezic.compiler.type.OpObject;
import mezic.compiler.type.OpObjectArray;
import mezic.compiler.type.OpShort;
import mezic.compiler.type.OpShortObj;
import mezic.compiler.type.OpString;
import mezic.compiler.type.OpVoid;
import mezic.compiler.type.TContext;
import mezic.compiler.type.TContextClass;
import mezic.compiler.type.TContextPkg;
import mezic.compiler.type.TContextTU;
import mezic.compiler.type.TFilesystemPkg;
import mezic.compiler.type.TMapType;
import mezic.compiler.type.TMethodHandle;
import mezic.compiler.type.TPrimitiveClass;
import mezic.compiler.type.TResolvedClass;
import mezic.parser.ASTTranslationUnit;
import mezic.parser.LangUnitNode;
import mezic.parser.ParseException;
import mezic.parser.Parser;
import mezic.parser.TokenMgrError;
import mezic.util.PathUtil;
import mezic.util.TypeUtil;
import mezic.util.Util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class CompilerLoader extends ClassLoader implements java.io.Serializable {

  private static final long serialVersionUID = -6955413858511262492L;

  public final static String TU_FILENAME_EXTENSION = ".mz";
  public final static String JAR_FILENAME_EXTENSION = ".jar";
  public final static String CLASS_FILENAME_EXTENSION = ".class";
  public final static String NODE_FILENAME_EXTENSION = ".node";
  public final static String COMPILER_PKG_DELIMETER = "/";
  public final static char DIR_DELIMETER_CHAR = '/';

  public final static String LANGSTREAM_CLASS_NAME = "mezi/util/StreamWrap";

  // public final static String LANGLIST_CLASS_NAME = "mezi/util/LangList";
  // public final static String LANGLIST_CLASS_NAME = "java/util/List";
  // public final static String LANGLIST_CLASS_NAME = "mezi/util/StreamWrap";

  public final static String APPLY_CLASS_NAME = "mezi/lang/Apply";
  public final static String UNRESOLVED_TYPE_CLASS_NAME = "mezi/lang/UnresolvedType";
  public final static String FUNCTIONAL_INTERFACE_DESC = "Ljava/lang/FunctionalInterface;";

  public final static int MAX_ARG_SIZE = 255;
  public final static int MAX_INHERITANCE_LEVEL = 6;
  public final static int MAX_CODE_BLOCK_LEVEL = 100;
  public final static int MAX_SUB_REF_LEVEL = 25;

  private TContextPkg root_pkg = null;

  private String source_base;
  private String target_base;
  private String[] class_pathes = null;
  private String[] dflt_import_pkgs = null;

  private HashMap<String, ClassNode> classnode_hash = null;
  private HashMap<String, TFilesystemPkg> fspkg_hash = null;
  private HashMap<String, TResolvedClass> resolvedclass_hash = null;
  private HashMap<String, String> opfuncmap_hash = null;
  private HashMap<String, JarFileCache> jarcache_hash = null;

  /* Context of Calling function */
  protected LinkedList<TContext> context_stack = null;
  /* Reduction Stack */
  protected LinkedList<Reduction> reduction_stack = null;
  /* Branch tree */
  protected BranchTree branch_tree = null;

  public CompilerLoader(String source_base, String target_base, String[] class_pathes, String[] dflt_import_pkgs)
      throws CompileException {
    this.source_base = source_base;
    this.target_base = target_base;
    this.class_pathes = class_pathes;
    this.dflt_import_pkgs = dflt_import_pkgs;
    this.classnode_hash = new HashMap<String, ClassNode>();
    this.fspkg_hash = new HashMap<String, TFilesystemPkg>();
    this.resolvedclass_hash = new HashMap<String, TResolvedClass>();
    this.opfuncmap_hash = new HashMap<String, String>();
    init_opfuncmap_hash();

    this.jarcache_hash = new HashMap<String, JarFileCache>();
    for (String cl_path : class_pathes) {
      if (cl_path.endsWith(JAR_FILENAME_EXTENSION)) {
        try {
          jarcache_hash.put(cl_path, new JarFileCache(cl_path));
        } catch (IOException e) {
          throw new CompileException("IOException Occurred(" + e.getMessage() + ")");
        }
      }
    }

    this.root_pkg = new TContextPkg("root", this);

    this.context_stack = new LinkedList<TContext>();
    this.reduction_stack = new LinkedList<Reduction>();
    this.branch_tree = new BranchTree();
  }

  public LinkedList<TContext> getContextStack() {
    return this.context_stack;
  }

  public LinkedList<Reduction> getReductionStack() {
    return this.reduction_stack;
  }

  public BranchTree getBranchTree() {
    return this.branch_tree;
  }

  private void init_opfuncmap_hash() {
    this.opfuncmap_hash.put("+", "__plus__");
    this.opfuncmap_hash.put("-", "__minus__");
    this.opfuncmap_hash.put("[]", "__map__");
  }

  protected void check_condition(boolean condition, String statement) throws CompileException {
    if (!condition) {
      throw new CompileException(statement + " condition is invalid");
    }
  }

  public String[] getDfltImportPkgs() {
    return dflt_import_pkgs;
  }

  protected InputStream getCodeStream(String name)
      throws ParseException /* , InterpreterException */ {
    return getFileCodeStream(name);
  }

  protected InputStream getFileCodeStream(String name) throws ParseException {
    String path_name = name.replace('.', '/');
    String path = "";
    File fPath;

    path = source_base + path_name + TU_FILENAME_EXTENSION;
    fPath = new File(path);

    if (!fPath.exists()) {
      throw new ParseException("can not find " + name + " on spring bases");
    }

    InputStream stream = null;

    try {
      stream = new java.io.FileInputStream(path);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new ParseException(e.getMessage());
    }
    return stream;
  }

  public ASTTranslationUnit constAst(String tu_name) throws ParseException {
    ASTTranslationUnit translation_unit;

    try {
      InputStream tu_stream = getCodeStream(tu_name);

      Debug.println_dbg("parsing(" + tu_name + ")"); // example : unit_test.Test
      Parser parser = new Parser(tu_stream);
      translation_unit = parser.TranslationUnit();
      // translation_unit.dump(".");
      // translation_unit.dump_tree(".", 0, 0);
    } catch (ParseException e) {
      e.printStackTrace();
      throw new ParseException(e.getMessage() + " in " + tu_name);
    } catch (TokenMgrError e) {
      e.printStackTrace();
      throw new ParseException(e.getMessage() + " in " + tu_name);
    }

    return translation_unit;
  }

  public void register_tu_context(TContext tu_context, String tu_name) throws CompileException {
    String pkg_name = PathUtil.getPkgName(tu_name);

    // ISSUE : Is this a correction action.
    File pkg_file = new File(this.target_base + pkg_name);

    if (!pkg_file.exists()) {
      pkg_file.mkdirs();

      if (!pkg_file.exists()) {
        throw new CompileException("Failed to mkdirs for (" + this.target_base + pkg_name + ")");
      }
    }

    Debug.println_dbg("Registering TU(" + tu_name + ") Pkg(" + pkg_name + ")");

    TContextPkg pkg_ctx = getContextPackage(pkg_name);

    if (pkg_ctx == null) {
      registerContextPackage(pkg_name);

      if (Debug.enable_print_dbg) {
        this.dumpContextPackageTree();
      }

      pkg_ctx = getContextPackage(pkg_name);

      if (pkg_ctx == null) {
        throw new CompileException("Failed to register context package(" + pkg_name + ")");
      }
    }

    TContext tmp_tu_ctx = pkg_ctx.getChildContextTu(tu_name);

    if (tmp_tu_ctx != null) {
      throw new CompileException("Translation Unit(" + tu_name + ") already exists");
    }

    pkg_ctx.addChildContext(tu_context);

  }

  public void registerContextPackage(String pkg_name) throws CompileException {
    String[] name_token = Util.getTokens(pkg_name, COMPILER_PKG_DELIMETER);

    String path_pkg_name = "";

    TContextPkg pkg_ctx = null;
    TContextPkg parent_pkg_ctx = this.root_pkg;

    for (int i = 0; i < name_token.length; i++) {
      if (i == 0) {
        path_pkg_name += (name_token[i]);
      } else {
        path_pkg_name += (COMPILER_PKG_DELIMETER + name_token[i]);
      }

      pkg_ctx = (TContextPkg) parent_pkg_ctx.getChildPackage(path_pkg_name, this);

      if (pkg_ctx == null) {
        pkg_ctx = new TContextPkg(path_pkg_name, this);
        parent_pkg_ctx.addChildContext(pkg_ctx);
      }

      parent_pkg_ctx = pkg_ctx;
    }

  }

  public TContextPkg getContextPackage(String pkg_name) throws CompileException {
    String[] name_token = Util.getTokens(pkg_name, COMPILER_PKG_DELIMETER);

    String path_pkg_name = "";

    TContextPkg pkg_ctx = null;
    TContextPkg parent_pkg_ctx = this.root_pkg;

    for (int i = 0; i < name_token.length; i++) {
      if (i == 0) {
        path_pkg_name += (name_token[i]);
      } else {
        path_pkg_name += (COMPILER_PKG_DELIMETER + name_token[i]);
      }

      pkg_ctx = (TContextPkg) parent_pkg_ctx.getChildPackage(path_pkg_name, this);

      if (pkg_ctx == null) {
        return null;
      }

      parent_pkg_ctx = pkg_ctx;
    }

    return pkg_ctx;
  }

  public void dumpContextPackageTree() {
    System.out.println("--Dump Context Package Tree ------------------");

    root_pkg.dump_child_context("", 0);
  }

  static void printLongerTrace(Throwable t) {
    for (StackTraceElement e : t.getStackTrace()) {
      Debug.println_dbg(e);
    }
  }

  public AbsType getFileSystemPackage(String pkg_name) throws CompileException {
    File cp_base_file = null;
    File pkg_file = null;
    TFilesystemPkg fspkg = null;

    fspkg = this.fspkg_hash.get(pkg_name);
    if (fspkg != null) {
      return fspkg;
    }

    for (int i = 0; i < class_pathes.length; i++) {
      cp_base_file = new File(class_pathes[i]);

      if (!cp_base_file.exists()) {
        throw new CompileException("ClassPath(" + class_pathes[i] + ") does not exist");
      }

      if (cp_base_file.isDirectory()) {
        String pkg_path = pkg_name.replace('.', DIR_DELIMETER_CHAR);
        pkg_path = class_pathes[i] + pkg_path;

        Debug.println_dbg(
            "Finding FileSystemPackage(" + pkg_path + ") from path(" + cp_base_file.getAbsolutePath() + ")");
        pkg_file = new File(pkg_path);

        if (pkg_file.exists() && pkg_file.isDirectory()) {
          // fspkg = (new TFilesystemPkg(pkg_name, pkg_file));
          fspkg = (new TFilesystemPkg(pkg_name));

          this.fspkg_hash.put(pkg_name, fspkg);
          return fspkg;
        }
      } else if (cp_base_file.getName().endsWith(JAR_FILENAME_EXTENSION)) {
        final String pkg_path = pkg_name.replace('.', DIR_DELIMETER_CHAR);

        Debug.println_dbg(
            "Finding FileSystemPackage(" + pkg_path + ") from jar(" + cp_base_file.getAbsolutePath() + ")");

        JarFileCache jarcache = this.jarcache_hash.get(class_pathes[i]);
        Debug.assertion(jarcache != null, "jarcache should be valid");

        if (jarcache.isValidPkg(pkg_path)) {
          // fspkg = (new TFilesystemPkg(pkg_name, pkg_file));
          fspkg = (new TFilesystemPkg(pkg_name));

          this.fspkg_hash.put(pkg_name, fspkg);
          return fspkg;
        }

      } else {
        throw new CompileException("Invalid ClassPath(" + class_pathes[i] + ")");
      }
    }

    // throw new InterpreterException("Package("+pkg_path+") does not exist");
    return null;
  }

  public AbsType findPackage(String pkg_name) throws CompileException {
    AbsType type = null;

    // CAUTION !! Sequence is important. Context Package(Building Package) has
    // higher priority
    // searching for building classes package
    type = getContextPackage(pkg_name);
    if (type != null) {
      return type;
    }

    // searching for existing classes package
    type = getFileSystemPackage(pkg_name);

    if (type != null) {
      return type;
    }

    return null;
  }

  // search class with simple class name or full class name
  public AbsClassType findClass(String class_name, TContextTU tu_context) throws CompileException {
    String[] tokens = Util.getTokens(class_name, "/");

    if (tokens == null) {
      throw new CompileException("Invalid class name(" + class_name + ")");
    }

    if (tokens.length > 1) {
      // full name case
      return findClassFull(class_name);
    } else {
      // canonical name case
      return findClassSimple(class_name, tu_context);
    }
  }

  // search class simple class name
  public AbsClassType findClassSimple(String simple_class_name, TContextTU tu_context) throws CompileException {
    AbsClassType type = null;
    type = (AbsClassType) findPrimitiveType(simple_class_name);
    if (type != null) {
      return type;
    }

    type = (AbsClassType) tu_context.getImportClass(simple_class_name);
    if (type != null) {
      return type;
    }

    String[] import_list = tu_context.getImportPathList();

    String full_class_name = null;

    for (int i = 0; i < import_list.length; i++) {
      full_class_name = import_list[i] + CompilerLoader.COMPILER_PKG_DELIMETER + simple_class_name;

      type = findClassFull(full_class_name);
      if (type != null) {
        return type;
      }
    }

    return null;

  }

  // search class with full calss name
  public AbsClassType findClassFull(String full_class_name) throws CompileException {
    AbsClassType type = null;

    type = (AbsClassType) findPrimitiveType(full_class_name);
    if (type != null) {
      return type;
    }

    String pkg_name = PathUtil.getPkgName(full_class_name);

    type = findContextClass(full_class_name, pkg_name);
    if (type != null) {
      return type;
    }

    type = findResolvedClass(full_class_name);

    return type;
  }

  // type contains class, function, map, ....
  public AbsType findNamedTypeFull(String full_type_name) throws CompileException {
    int map_dimension = TypeUtil.get_map_dimension(full_type_name);
    String ele_type_name = TypeUtil.get_type_name(full_type_name);

    AbsType ele_type = (AbsType) findClassFull(ele_type_name);

    if (map_dimension != 0) {
      TMapType map_type = findMapType(ele_type, map_dimension);
      Debug.assertion(map_type != null, "map_type reduce should be valid");
      return map_type;
    } else {
      return ele_type;
    }

  }

  public AbsClassType findPrimitiveType(String full_class_name) throws CompileException {
    ClassNode primtive_classnode = null;

    if (full_class_name.equals(TPrimitiveClass.NAME_INT)) {
      primtive_classnode = findClassNode("java/lang/Integer");
      Debug.assertion(primtive_classnode != null, "primtive_classnode should be valid");
      return new TPrimitiveClass(TPrimitiveClass.NAME_INT, primtive_classnode, new OpInt(this), this);
    } else if (full_class_name.equals(TPrimitiveClass.NAME_CHAR)) {
      primtive_classnode = findClassNode("java/lang/Character");
      Debug.assertion(primtive_classnode != null, "primtive_classnode should be valid");
      return new TPrimitiveClass(TPrimitiveClass.NAME_CHAR, primtive_classnode, new OpChar(this), this);
    } else if (full_class_name.equals(TPrimitiveClass.NAME_SHORT)) {
      primtive_classnode = findClassNode("java/lang/Short");
      Debug.assertion(primtive_classnode != null, "primtive_classnode should be valid");
      return new TPrimitiveClass(TPrimitiveClass.NAME_SHORT, primtive_classnode, new OpShort(this), this);
    } else if (full_class_name.equals(TPrimitiveClass.NAME_BYTE)) {
      primtive_classnode = findClassNode("java/lang/Byte");
      Debug.assertion(primtive_classnode != null, "primtive_classnode should be valid");
      return new TPrimitiveClass(TPrimitiveClass.NAME_BYTE, primtive_classnode, new OpByte(this), this);
    } else if (full_class_name.equals(TPrimitiveClass.NAME_LONG)) {
      primtive_classnode = findClassNode("java/lang/Long");
      Debug.assertion(primtive_classnode != null, "primtive_classnode should be valid");
      return new TPrimitiveClass(TPrimitiveClass.NAME_LONG, primtive_classnode, new OpLong(this), this);
    } else if (full_class_name.equals(TPrimitiveClass.NAME_BOOL)) {
      primtive_classnode = findClassNode("java/lang/Boolean");
      Debug.assertion(primtive_classnode != null, "primtive_classnode should be valid");
      return new TPrimitiveClass(TPrimitiveClass.NAME_BOOL, primtive_classnode, new OpBool(this), this);
    } else if (full_class_name.equals(TPrimitiveClass.NAME_FLOAT)) {
      primtive_classnode = findClassNode("java/lang/Float");
      Debug.assertion(primtive_classnode != null, "primtive_classnode should be valid");
      return new TPrimitiveClass(TPrimitiveClass.NAME_FLOAT, primtive_classnode, new OpFloat(this), this);
    }  else if (full_class_name.equals(TPrimitiveClass.NAME_DOUBLE)) {
      primtive_classnode = findClassNode("java/lang/Double");
      Debug.assertion(primtive_classnode != null, "primtive_classnode should be valid");
      return new TPrimitiveClass(TPrimitiveClass.NAME_DOUBLE, primtive_classnode, new OpDouble(this), this);
    } else if (full_class_name.equals(TPrimitiveClass.NAME_VOID)) {
      primtive_classnode = findClassNode("java/lang/Void");
      Debug.assertion(primtive_classnode != null, "primtive_classnode should be valid");
      return new TPrimitiveClass(TPrimitiveClass.NAME_VOID, primtive_classnode, new OpVoid(this), this);
    }

    return null;
  }

  public TMapType findMapType(AbsType type, int map_dimension) throws CompileException {
    TMapType map_type = null;
    AbsType element_type = type;

    for (int i = 1; i <= map_dimension; i++) {
      map_type = new TMapType(element_type, i, new OpObjectArray(this), this);
      element_type = map_type;
    }

    return map_type;
  }

  public AbsClassType findContextClass(String full_class_name, String pkg_name) throws CompileException {
    TContextClass classContext = null;

    TContextPkg pkg_ctx = getContextPackage(pkg_name);

    // if(pkg_ctx==null) throw new InterpreterException("It cannot find
    // package("+pkg_name+")");
    if (pkg_ctx == null) {
      return null;
    }

    classContext = (TContextClass) pkg_ctx.getChildClass(full_class_name);

    return classContext;
  }

  public AbsClassType findResolvedClass(String full_class_name) throws CompileException {
    TResolvedClass resclass = null;
    OpLvalue class_op = null;
    ClassNode cn = null;

    resclass = this.resolvedclass_hash.get(full_class_name);

    if (resclass != null) {
      return resclass;
    }

    cn = findClassNode(full_class_name);

    if (cn != null) {
      class_op = findClassOp(full_class_name);

      resclass = new TResolvedClass(cn, class_op, this);

      this.resolvedclass_hash.put(full_class_name, resclass);

      return resclass;
    }

    return null;
  }

  public ClassNode findClassNode(String full_class_name) throws CompileException {
    File cp_base_file = null;
    File class_file = null;
    String class_path = null;

    ClassReader cr = null;
    ClassNode cn = null;

    cn = this.classnode_hash.get(full_class_name);

    if (cn != null) {
      return cn;
    }

    for (int i = 0; i < class_pathes.length; i++) {
      cp_base_file = new File(class_pathes[i]);

      if (!cp_base_file.exists()) {
        throw new CompileException("ClassPath(" + class_pathes[i] + ") does not exist");
      }

      if (cp_base_file.isDirectory()) {
        class_path = class_pathes[i] + full_class_name + CLASS_FILENAME_EXTENSION;
        Debug.println_dbg("Finding Resolved Class File(" + class_path + ")");

        class_file = new File(class_path);

        if (class_file.exists()) {
          if (class_file.isDirectory()) {
            throw new CompileException("Invalid Class File(" + class_file + " is directory)");
          }

          try {
            cr = new ClassReader(new java.io.FileInputStream(class_path));
          } catch (IOException e) {
            e.printStackTrace();
            throw new CompileException("IOException Occurred(" + e.getMessage() + ")");
          }

          cn = new ClassNode();
          cr.accept(cn, ClassReader.SKIP_DEBUG);
          // Debug.println_dbg("ClassNode("+cn.name+")");

          this.classnode_hash.put(full_class_name, cn);

          return cn;
        }
      } else if (cp_base_file.getName().endsWith(JAR_FILENAME_EXTENSION)) {
        class_path = full_class_name + CLASS_FILENAME_EXTENSION;
        Debug.println_dbg(
            "Finding Resolved Class File(" + class_path + ") from jar(" + cp_base_file.getAbsolutePath() + ")");

        try {
          JarFileCache jarcache = this.jarcache_hash.get(class_pathes[i]);
          Debug.assertion(jarcache != null, "jarcache should be valid");

          InputStream class_is = jarcache.getClassInputStream(class_path);

          if (class_is != null) {
            cr = new ClassReader(class_is);
            cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_DEBUG);
            // Debug.println_dbg("ClassNode("+cn.name+")");
            this.classnode_hash.put(full_class_name, cn);

            return cn;
          }

        } catch (IOException e) {
          e.printStackTrace();

          throw new CompileException("IOException Occurred(" + e.getMessage() + ")");
        }

        // failed to find class from this jar file
      } else {
        throw new CompileException("Invalid ClassPath(" + class_pathes[i] + ")");
      }

    }

    return null;
  }

  public OpLvalue findClassOp(String full_class_name) {
    if (full_class_name.equals("java/lang/String")) {
      return new OpString(this);
    } else if (full_class_name.equals("java/lang/Integer")) {
      return new OpIntegerObj(this);
    } else if (full_class_name.equals("java/lang/Character")) {
      return new OpCharacterObj(this);
    } else if (full_class_name.equals("java/lang/Short")) {
      return new OpShortObj(this);
    } else if (full_class_name.equals("java/lang/Byte")) {
      return new OpByteObj(this);
    } else if (full_class_name.equals("java/lang/Long")) {
      return new OpLongObj(this);
    } else if (full_class_name.equals("java/lang/Boolean")) {
      return new OpBooleanObj(this);
    } else if (full_class_name.equals("java/lang/Float")) {
      return new OpFloatObj(this);
    } else if (full_class_name.equals("java/lang/Double")) {
      return new OpDoubleObj(this);
    }

    return new OpObject(this);
  }

  public AbsType findLocalFunction(AbsClassType class_type, String name, AbsTypeList argtype_list)
      throws CompileException {
    Debug.println_dbg("findLocalFunction: " + name + "(" + argtype_list + ") in " + class_type);

    AbsType func_type = null;
    func_type = (AbsType) ((AbsClassType) class_type).getLocalFunction(name, argtype_list);

    if (func_type == null) {
      String opfunc_name = this.opfuncmap_hash.get(name);
      if (opfunc_name != null) {
        func_type = (AbsType) ((AbsClassType) class_type).getLocalFunction(opfunc_name, argtype_list);
      }
    }

    if (func_type == null) {
      func_type = (AbsType) ((AbsClassType) class_type).getLocalVarArgFunction(name, argtype_list);
    }

    // ISSUE : Is the priority correct ...?
    if (func_type == null) {
      func_type = (AbsType) ((AbsClassType) class_type).getLocalApplyFunction(name, argtype_list);
    }

    return func_type;
  }

  public void checkExceptionHandling(AbsType type, TContext top_context) throws CompileException {
    Debug.assertion(type instanceof AbsFuncType, "func_type should be function type");

    AbsFuncType func_type = (AbsFuncType) type;

    AbsTypeList throws_excp_list = func_type.getThrowsList();
    AbsType excp_type = null;
    if (throws_excp_list != null) {
      int size = throws_excp_list.size();

      for (int i = 0; i < size; i++) {
        excp_type = throws_excp_list.get(i);
        if (!top_context.isAddressableException(excp_type)) {
          throw new CompileException("Unhandled Exception (" + excp_type + ")");
        }
      }
    }
  }

  public byte[] writeClassFile(ClassWriter cw, String class_name) throws IOException {
    byte[] code = cw.toByteArray();

    String class_file_name = target_base + class_name + CLASS_FILENAME_EXTENSION;

    Debug.println_dbg("writing (" + class_name + ") on (" + class_file_name + ")");

    FileOutputStream fos = new FileOutputStream(class_file_name);
    fos.write(code);
    fos.close();
    return code;
  }

  public void writeNodeFile(LangUnitNode node, String node_name) throws CompileException {
    String node_file_name = target_base + node_name + NODE_FILENAME_EXTENSION;

    Debug.println_dbg("writing node(" + node_name + ") on (" + node_file_name + ")");
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(node_file_name);
    } catch (FileNotFoundException e) {
      CompileException excp = new CompileException("Exception occurred in writing node file", node);
      excp.setTargetException(e);
    }

    node.serialize(fos);

  }

  public static String getApplyNodeFileName(String class_name, String func_name, String sig) {
    return class_name + "_" + func_name + "_" + sig;
  }

  public LangUnitNode readNodeFile(String node_name) throws CompileException {
    String node_file_name = target_base + node_name + NODE_FILENAME_EXTENSION;

    Debug.println_dbg("reading node(" + node_name + ") on (" + node_file_name + ")");
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(node_file_name);
    } catch (FileNotFoundException e) {
      CompileException excp = new CompileException("Exception occurred in reading node file");
      excp.setTargetException(e);
      throw excp;
    }

    LangUnitNode node = LangUnitNode.de_serialize(fis);

    return node;
  }

  public boolean isImpliedCastibleParameterList(AbsTypeList src_paratype_list, AbsTypeList tgt_paratype_list)
      throws CompileException {
    int size = src_paratype_list.size();

    if (size != tgt_paratype_list.size()) {
      return false;
    }

    return isImpliedCastibleParameterListIdx(src_paratype_list, tgt_paratype_list, size - 1);
  }

  public boolean isImpliedCastibleParameterListIdx(AbsTypeList src_paratype_list, AbsTypeList tgt_paratype_list,
      int last_idx) throws CompileException {
    AbsType src_type = null;
    AbsType tgt_type = null;

    for (int i = 0; i <= last_idx; i++) {
      src_type = src_paratype_list.get(i);
      tgt_type = tgt_paratype_list.get(i);

      if (!isImpliedCastible(src_type, tgt_type)) {
        return false;
      }

      /*
       * if( src_type instanceof AbsFuncType && tgt_type instanceof
       * TMethodHandle) { if( ! isAppliableFunc((AbsFuncType)src_type,
       * (TMethodHandle)tgt_type) ) return false; } else { if( !
       * isCompatibleClass(src_type, tgt_type) && ! isConvertibleClass(src_type,
       * tgt_type) ) return false; }
       */

    }

    return true;
  }

  public boolean isImpliedCastible(AbsType src_type, AbsType tgt_type) throws CompileException {
    if (src_type instanceof AbsFuncType) {
      if (tgt_type instanceof TMethodHandle) {
        if (isAppliableFunc((AbsFuncType) src_type, (TMethodHandle) tgt_type)) {
          return true;
        }
      }

    } else if (src_type instanceof AbsClassType) {
      if (tgt_type instanceof AbsClassType) {
        if (isCompatibleClass(src_type, tgt_type) || isConvertibleClass(src_type, tgt_type)) {
          return true;
        }
      }
    } else if (src_type instanceof TMapType) {
      if (tgt_type instanceof AbsClassType) {
        if (isCompatibleClass(src_type, tgt_type) || isConvertibleClass(src_type, tgt_type)) {
          return true;
        }
      }
    }

    return false;
  }

  public boolean isAppliableFunc(AbsFuncType src_func_type, TMethodHandle mh_type) throws CompileException {
    if (src_func_type.is_apply()) {
      FuncSignatureDesc src_sig = src_func_type.getFuncSignatureDesc();
      Debug.assertion(src_sig != null, "src_sig should be valid");
      Debug.println_dbg("src_sig:" + src_sig);

      FuncSignatureDesc tgt_sig = mh_type.getFuncSignatureDesc();
      Debug.assertion(tgt_sig != null, "tgt_sig should be valid");
      Debug.println_dbg("tgt_sig:" + tgt_sig);

      return (src_sig.getParameterTypeList().size() == tgt_sig.getParameterTypeList().size());
      /*
      if (src_sig.getParameterTypeList().size() == tgt_sig.getParameterTypeList().size())
        return true;
      else
        return false;
      */
    }

    return false;
  }

  public boolean isCompatibleClass(AbsType srctype, AbsType tgttype) throws CompileException {
    Debug.assertion(srctype instanceof AbsClassType || srctype instanceof TMapType,
        "srctype should be instanceof AbsClassType|TMapType, but " + srctype);
    Debug.assertion(tgttype instanceof AbsClassType || tgttype instanceof TMapType,
        "tgttype should be instanceof AbsClassType|TMapType, but " + tgttype);

    // if it is same class
    if (srctype.equals(tgttype)) {
      return true;
    }

    if (srctype instanceof AbsClassType && tgttype instanceof AbsClassType) {
      AbsClassType srcclass = (AbsClassType) srctype;
      AbsClassType tgtclass = (AbsClassType) tgttype;

      // ISSUE: primitive type is not compatible.. (Is this correct ..?)
      // if( srcclass instanceof TPrimitiveClass ) return false;
      if (srctype.op().is_compatible_type(tgttype)) {
        return true;
      }

      // if tgtclass is ancestor class of srcclass
      Debug.println_dbg("descendent checking: srcclass:" + srcclass + ", tgtclass:" + tgtclass);
      if (srcclass.isDescendentOf(tgtclass)) {
        return true;
      }
    } else if (srctype instanceof TMapType && tgttype instanceof AbsClassType) {
      if (srctype.op().is_compatible_type(tgttype)) {
        return true;
      }
    }

    return false;
  }

  public boolean isConvertibleClass(AbsType srctype, AbsType tgttype) throws CompileException {
    Debug.assertion(srctype instanceof AbsClassType || srctype instanceof TMapType,
        "srctype should be instanceof AbsClassType|TMapType");
    Debug.assertion(tgttype instanceof AbsClassType || tgttype instanceof TMapType,
        "tgttype should be instanceof AbsClassType|TMapType");

    return (srctype.op().get_converting_type(tgttype) != null);
    /*
    if (srctype.op().get_converting_type(tgttype) == null)
      return false;
    else
      return true;
    */
  }

  public boolean isExplicitlyCastibleClass(AbsType srctype, AbsType tgttype) throws CompileException {
    Debug.assertion(srctype instanceof AbsClassType, "srctype should be instanceof AbsClassType");
    Debug.assertion(tgttype instanceof AbsClassType, "tgttype should be instanceof AbsClassType");

    AbsClassType srcclass = (AbsClassType) srctype;
    AbsClassType tgtclass = (AbsClassType) tgttype;

    if (tgttype instanceof TPrimitiveClass) {
      // java.lang.Object(srctype) -> int(tgttype)
      return true;
    } else {
      return (tgtclass.isDescendentOf(srcclass));
      /*
      if (tgtclass.isDescendentOf(srcclass)) // java.lang.Object(srctype) ->
                                             // java.lang.Integer(tgttype)
        return true;
      else
        return false;
      */
    }
  }

  public boolean isVarArgAbsTypeList(AbsTypeList invoke_paratype_list, AbsTypeList found_paratype_list,
      boolean is_polymorphic) throws CompileException {
    int tgt_last_param_idx = found_paratype_list.size() - 1;

    AbsType last_type = found_paratype_list.get(tgt_last_param_idx);
    AbsType ele_type = null;

    if (last_type instanceof TMapType) {
      ele_type = ((TMapType) last_type).getElementType();
      Debug.assertion(ele_type != null, "ele_type should be valid");

      if (is_polymorphic) {

        if (this.isImpliedCastibleParameterListIdx(invoke_paratype_list, found_paratype_list, tgt_last_param_idx - 1)) {
          // Note !!
          // if invoke_paratype_list does not have var arg list, it will succeed
          // example) public static java.nio.file.Path get(String first,
          // String... more)
          // but, 'Path.get("./a") is possible( invoking parameter does not have
          // var arg list ).
          // Compiler automatically add String[0] following the "./a"

          Debug.println_dbg("last_idx=" + tgt_last_param_idx + " paratype_list.size()=" + invoke_paratype_list.size());
          if (invoke_paratype_list.isImpliedCastibleTypeInRange(ele_type, tgt_last_param_idx,
              invoke_paratype_list.size() - 1, this)) {
            return true;
          }
        }
      } else {
        if (invoke_paratype_list.equalAbsTypeListWithSize(found_paratype_list, tgt_last_param_idx)) {
          // Note !!
          // if invoke_paratype_list does not have var arg list, it will succeed
          // example) public static java.nio.file.Path get(String first,
          // String... more)
          // but, 'Path.get("./a") is possible( invoking parameter does not have
          // var arg list ).
          // Compiler automatically add String[0] following the "./a"

          Debug.println_dbg("last_idx=" + tgt_last_param_idx + " paratype_list.size()=" + invoke_paratype_list.size());
          if (invoke_paratype_list.isEqualTypeInRange(ele_type, tgt_last_param_idx, invoke_paratype_list.size() - 1)) {
            return true;
          }
        }
      }

    }

    return false;
  }

  public AbsClassType getExceptionClass() throws CompileException {
    AbsClassType excp_class = findClassFull("java/lang/Exception");
    Debug.assertion(excp_class != null, "excp_class should be valid");
    return excp_class;
  }

}
