package mezic.compiler;

public class BranchTree {

  private Branch curr_branch = null;
  private Branch root_branch = null;

  public Branch getCurrBranch() {
    return curr_branch;
  }

  public void pushBranch(Branch branch) throws CompileException {
    Debug.assertion(curr_branch != null, "curr_branch should be valid");

    curr_branch.getChildList().add(branch);

    branch.initParent(curr_branch);

    curr_branch = branch;
  }

  public Branch popBranch() throws CompileException {
    Debug.assertion(curr_branch != null, "curr_branch should be valid");
    Branch prev_curr_branch = curr_branch;
    Branch parent = curr_branch.getParent();

    Debug.assertion(parent != null, "parent should be valid");
    curr_branch = parent;

    return prev_curr_branch;
  }

  public void initRootBranch(Branch root) {
    this.root_branch = root;
    this.curr_branch = root;
  }

  public Branch getRootBranch() {
    return root_branch;
  }

}
