package mezic.compiler;

import java.util.LinkedList;

import org.objectweb.asm.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MatchCaseEntry {
  public int const_val = 0;
  public Label label = null;
}

public class MatchCaseList extends LinkedList<MatchCaseEntry> {

  private static final long serialVersionUID = -4407672227824544756L;
  
  private static final Logger LOG = LoggerFactory.getLogger(MatchCaseList.class);

  public boolean has(int const_val) {
    int size = this.size();

    MatchCaseEntry entry = null;

    for (int i = 0; i < size; i++) {
      entry = this.get(i);
      if (entry.const_val == const_val) {
        return true;
      }
    }

    return false;
  }

  public boolean add(int const_val, Label label) {
    MatchCaseEntry entry = new MatchCaseEntry();
    entry.const_val = const_val;
    entry.label = label;

    return this.add(entry);
  }

  public void sort() {
    int size = this.size();
    MatchCaseEntry entry_i = null;
    MatchCaseEntry entry_j = null;

    int tmp_const = 0;
    Label tmp_label = null;

    for (int i = 0; i < size; i++) {
      entry_i = get(i);

      for (int j = i; j < size; j++) {
        entry_j = get(j);

        if (entry_j.const_val < entry_i.const_val) { // swap
          tmp_const = entry_i.const_val;
          tmp_label = entry_i.label;

          entry_i.const_val = entry_j.const_val;
          entry_i.label = entry_j.label;

          entry_j.const_val = tmp_const;
          entry_j.label = tmp_label;

        }
      }
    }

  }

  public int[] getConstValList() {
    int size = this.size();
    int[] constval_arr = new int[size];

    MatchCaseEntry entry = null;

    for (int i = 0; i < size; i++) {
      entry = this.get(i);
      constval_arr[i] = entry.const_val;
    }

    return constval_arr;
  }

  public Label[] getLabelList() {
    int size = this.size();
    Label[] label_arr = new Label[size];

    MatchCaseEntry entry = null;

    for (int i = 0; i < size; i++) {
      entry = this.get(i);
      label_arr[i] = entry.label;
    }

    return label_arr;

  }

  public void dump() {
    int size = this.size();

    MatchCaseEntry entry = null;

    for (int i = 0; i < size; i++) {
      entry = this.get(i);

      LOG.debug("entry.const_val=" + entry.const_val + " entry.label=" + entry.label);
    }

  }
}
