package mezic.compiler.type;

import java.util.LinkedList;

import mezic.compiler.CompileException;
import mezic.compiler.CompilerLoader;
import mezic.compiler.Debug;
import mezic.util.TypeUtil;

public class AbsTypeList extends LinkedList<AbsType> {

  private static final long serialVersionUID = 8886558343759302270L;

  public String getMthdDsc() throws CompileException {
    String dsc_str = "";
    int size = this.size();
    for (int i = 0; i < size; i++) {
      dsc_str += ((AbsType) this.get(i)).getMthdDscStr();
    }
    return dsc_str;
  }

  public String getSigStr() {
    String sig_str = "";
    int size = this.size();
    for (int i = 0; i < size; i++) {
      try {
        if (this.get(i) instanceof TMethodHandle) {
          sig_str += ((TMethodHandle) this.get(i)).getSigStr();
        } else {
          sig_str += ((AbsType) this.get(i)).getMthdDscStr();
        }
      } catch (CompileException e) {
        e.printStackTrace();
      }
    }

    return sig_str;
  }

  public boolean equalAbsTypeList(AbsTypeList typelist) throws CompileException {
    return this.getMthdDsc().equals(typelist.getMthdDsc());
  }

  public boolean equalAbsTypeListWithSize(AbsTypeList typelist, int size) throws CompileException {
    AbsType src_type = null;
    AbsType tgt_type = null;

    for (int i = 0; i < size; i++) {
      src_type = this.get(i);
      Debug.assertion(src_type != null, "src_type should be valid");

      tgt_type = typelist.get(i);
      Debug.assertion(tgt_type != null, "tgt_type should be valid");

      if (!src_type.getMthdDscStr().equals(tgt_type.getMthdDscStr())) {
        return false;
      }

    }

    return true;
  }

  public boolean isEqualTypeInRange(AbsType tgt_type, int start_idx, int last_idx) throws CompileException {
    AbsType src_type = null;

    for (int i = start_idx; i <= last_idx; i++) {
      src_type = this.get(i);
      Debug.assertion(src_type != null, "src_type should be valid");

      // Debug.println_dbg("tgt_type["+tgt_type+"] src_type["+src_type+"]");
      if (!src_type.getMthdDscStr().equals(tgt_type.getMthdDscStr())) {
        return false;
      }
    }

    return true;
  }

  public boolean isImpliedCastibleTypeInRange(AbsType tgt_type, int start_idx, int last_idx, CompilerLoader cpLoader)
      throws CompileException {
    AbsType src_type = null;

    for (int i = start_idx; i <= last_idx; i++) {
      src_type = this.get(i);
      Debug.assertion(src_type != null, "src_type should be valid");

      // Debug.println_dbg("tgt_type["+tgt_type+"] src_type["+src_type+"]");
      if (!cpLoader.isImpliedCastible(src_type, tgt_type)) {
        return false;
      }
    }

    return true;
  }

  public static AbsTypeList construct(String tgt_type_dsc, CompilerLoader cpLoader) throws CompileException {
    String tgt_type_name;
    AbsType tgt_classtype;
    AbsTypeList tgt_abstype_list = new AbsTypeList();
    String[] tgt_abstype_split = TypeUtil.splitArgTypeDsc(tgt_type_dsc);

    Debug.println_dbg(" Constructing Parameter Type List from (" + tgt_type_dsc + ")");

    for (int i = 0; i < tgt_abstype_split.length; i++) {
      tgt_type_name = TypeUtil.dsc2name(tgt_abstype_split[i]);
      if (tgt_type_name == null) {
        throw new CompileException("It cannot get class name (" + tgt_abstype_split[i] + ")");
      }

      // tgt_classtype = (AbsType)cpLoader.findClassFull(tgt_type_name);
      tgt_classtype = cpLoader.findNamedTypeFull(tgt_type_name);

      if (tgt_classtype == null) {
        throw new CompileException("It cannot find class(" + tgt_type_name + ")");
      }
      tgt_abstype_list.add((AbsType) tgt_classtype);
    }

    return tgt_abstype_list;
  }

  public boolean has(AbsType tgt_type) {
    int size = this.size();
    AbsType type = null;
    for (int i = 0; i < size; i++) {
      type = this.get(i);
      if (type.equals(tgt_type)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String toString() {
    return getSigStr();
  }

  public String[] toStringArray() {
    int size = this.size();
    String[] str_arr = new String[size];

    for (int i = 0; i < size; i++) {
      str_arr[i] = this.get(i).getName();
    }

    return str_arr;
  }

}
