package mezic.compiler.type;

import java.util.HashMap;
import java.util.LinkedList;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.util.PathUtil;

public class TContextTU extends TContext {

  private static final long serialVersionUID = -1947557512121807139L;

  private String pkg_name = null;

  private LinkedList<String> import_path_list = null;

  private HashMap<String, AbsType> importclass_hash = null;

  public TContextTU(String tu_name, OpLvalue type_op, CompilerLoader cpLoader) throws CompileException {
    super(null, AbsType.FORM_TU, tu_name, type_op, cpLoader);

    importclass_hash = new HashMap<>();

    pkg_name = PathUtil.getPkgName(this.name);
    import_path_list = new LinkedList<String>();
    import_path_list.add(pkg_name);

    String[] dflt_importpkgs = cpLoader.getDfltImportPkgs();
    for (int i = 0; i < dflt_importpkgs.length; i++) {
      import_path_list.add(dflt_importpkgs[i]);
    }

    /*
     * // default import list import_path_list.add("java/util");
     * import_path_list.add("java/util/function");
     * import_path_list.add("java/lang"); import_path_list.add("java/io");
     * import_path_list.add("unit_test/grammar");
     */
  }

  public String getPackageName() throws CompileException {
    return pkg_name;
  }

  public void addImportClass(String simple_classname, AbsType class_type) {
    importclass_hash.put(simple_classname, class_type);
  }

  public AbsType getImportClass(String simple_classname) {
    return importclass_hash.get(simple_classname);
  }

  public void addImportPath(String path) {
    import_path_list.add(path);
  }

  public String[] getImportPathList() {
    int size = import_path_list.size();
    String[] arr_imp_list = new String[size];

    for (int i = 0; i < size; i++) {
      arr_imp_list[i] = import_path_list.get(i);
    }

    return arr_imp_list;
  }

}
