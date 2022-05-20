package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


/** The Repository class.
 *  @author Edan Bash
 */
public class Repository implements Serializable {

    /** Current working directory of repo. */
    private final File _CWD = new File(".");

    /** Directory of .gitlet folder. */
    private final File _GITLET = Utils.join(_CWD, ".gitlet");

    /** File that stores edits staged for addition. */
    private final File _ADDSTAGE = Utils.join(_GITLET, "addStage");

    /** File that stores edits staged for removal. */
    private final File _RMSTAGE = Utils.join(_GITLET, "rmStage");

    /** File that stores head commits of all branches. */
    private final File _BRANCHES = Utils.join(_GITLET, "branches");

    /** File that stores all commits with their IDS. */
    private final File _COMMITS = Utils.join(_GITLET, "commits");

    /** File that stores head commit of repo. */
    private final File _HEAD = Utils.join(_GITLET, "head");

    /** File that stores current branch of repo. */
    private final File _CURRBRANCH = Utils.join(_GITLET, "currBranch");

    /** Constructor for Repository Class. */
    public Repository() {
        _addStage = new HashMap<>();
        _rmStage = new HashMap<>();
        _branches = new HashMap<>();
        _commits = new HashMap<>();
        init();
    }

    /** Creates a new Gitlet version-control system in the current directory.
     * This system will automatically start with one commit: a commit that
     * contains no files and has the commit message initial commit (just like
     * that, with no punctuation). It will have a single branch: master, which
     * initially points to this initial commit, and master will be the current
     * branch. The timestamp for this initial commit will be 00:00:00 UTC,
     * Thursday, 1 January 1970 in whatever format you choose for dates (this
     * is called "The (Unix) Epoch",  represented internally by the time 0.)
     * Since the initial commit in all repositories created by Gitlet will
     * have exactly the same content, it follows that all repositories will
     * automatically share this commit (they will all have the same UID) and
     * all commits in all repositories will trace back to it. */
    private void init() {
        _GITLET.mkdir();
        try {
            boolean a = _ADDSTAGE.createNewFile();
            boolean b = _RMSTAGE.createNewFile();
            boolean c = _BRANCHES.createNewFile();
            boolean d = _COMMITS.createNewFile();
            boolean e = _HEAD.createNewFile();
        } catch (IOException e) {
            System.out.println("Could not create file");
        }

        Commit initialCom = new Commit("initial commit", null,
                new HashMap<>(), new Date(0));
        initialCom.updateCommit(_addStage, _rmStage);
        _branches.put("master", initialCom);
        _commits.put(initialCom.getHashCode(), initialCom);
        _currBranch = "master";
        _head = _branches.get("master");
        saveCurrentState();
    }


    /** Adds a copy of the file as it currently exists to the staging area (see
     * the description of the commit command). For this reason, adding a file
     * is also called staging the file for addition. Staging an already-staged
     * file overwrites the previous entry in the staging area with the new
     * contents. The staging area should be somewhere in .gitlet. If the current
     * working version of the file is identical to the version in the current
     * commit, do not stage it to be added, and remove it from the staging area
     * if it is already there (as can happen when a file is changed, added, and
     * then changed back). The file will no longer be staged for removal (see
     * gitlet rm), if it was at the time of the command.
     *
     * @param args Argument array from command line
     */
    public void addCommand(String[] args) {
        validateNumArgs(args, 2);
        if (!fileExists(args[1])) {
            throw new GitletException("File does not exist.");
        }
        retreiveState();
        boolean alreadyStaged = (_addStage.containsKey(args[1]));
        if (_head.contains(args[1]) && !_rmStage.containsKey(args[1])
                && sameContents(_head.getBlobFromFile(args[1]), args[1])) {
            if (alreadyStaged) {
                _addStage.remove(args[1]);
            }
        } else if (!alreadyStaged
                || !sameContents(_addStage.get(args[1]), args[1])) {
            if (_rmStage.containsKey(args[1])) {
                _rmStage.remove(args[1]);
            } else {
                _addStage.put(args[1], new Blob(getFileContent(args[1])));
            }
        }
        saveCurrentState();
    }

    /** Saves a snapshot of certain files in the current commit and staging
     * area so they can be restored at a later time, creating a new commit.
     * The commit is said to be tracking the saved files. By default, each
     * commit's snapshot of files will be exactly the same as its parent
     * commit's snapshot of files; it will keep versions of files exactly
     * as they are, and not update them. A commit will only update the
     * contents of files it is tracking that have been staged for addition
     * at the time of commit, in which case the commit will now include the
     * version of the file that was staged instead of the version it got from
     * its parent. A commit will save and start tracking any files that were
     * staged for addition but weren't tracked by its parent. Finally, files
     * tracked in the current commit may be untracked in the new commit as a
     * result being staged for removal by the rm command (below).
     *
     * @param args Argument array from command line
     * */
    public void commitCommand(String[] args) {
        validateNumArgs(args, 2);
        if (args[1].equals("")) {
            throw new GitletException("Please enter a commit message.");
        }
        retreiveState();
        Commit commit = new Commit(args[1], _head, _head.getBlobs(),
                new Date());
        commit.updateCommit(_addStage, _rmStage);
        _branches.put(_currBranch, commit);
        _commits.put(commit.getHashCode(), commit);
        _head = commit;
        _addStage.clear();
        _rmStage.clear();
        saveCurrentState();
    }

    /** Unstage the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and remove the file
     * from the working directory if the user has not already done so (do not
     * remove it unless it is tracked in the current commit).
     *
     * @param args Argument array from command line
     * */
    public void rmCommand(String[] args) {
        validateNumArgs(args, 2);
        retreiveState();
        if (_addStage.containsKey(args[1])) {
            _addStage.remove(args[1]);
        } else if (_head.contains(args[1])) {
            _rmStage.put(args[1], _head.getBlobFromFile(args[1]));
            deleteFile(args[1]);
        } else {
            throw new GitletException("No reason to remove the file.");
        }
        saveCurrentState();
    }

    /** Starting at the current head commit, display information about each
     * commit backwards along the commit tree until the initial commit,
     * following the first parent commit links, ignoring any second parents
     * found in merge commits. (In regular Git, this is what you get with git
     * log --first-parent). This set of commit nodes is called the commit's
     * history. For every node in this history, the information it should
     * display is the commit id, the time the commit was made, and the
     * commit message.
     *
     * @param args Argument array from command line
     */
    public void logCommand(String[] args) {
        validateNumArgs(args, 1);
        retreiveState();
        logHelper(_head);
    }

    /** Prints out correct format for logCommand starting with COM. */
    public void logHelper(Commit com) {
        Commit curr = com;
        while (curr != null) {
            System.out.println("===");
            System.out.println("commit " + curr.getHashCode());
            System.out.println("Date: " + curr.getDate());
            System.out.println(curr.getMsg());
            System.out.println();
            curr = curr.getParent();
        }
    }

    /** Like log, except displays information about all commits ever made.
     * The order of the commits does not matter.
     *
     * @param args Argument array from command line
     */
    public void globalLogCommand(String[] args) {
        validateNumArgs(args, 1);
        retreiveState();
        for (Commit com: _commits.values()) {
            logHelper(com);
        }
    }

    /** Prints out the ids of all commits that have the given commit message,
     * one per line. If there are multiple such commits, it prints the ids
     * out on separate lines. The commit message is a single operand; to
     * indicate a multiword message, put the operand in quotation marks.
     *
     * @param args Argument array from command line
     */
    public void findCommand(String[] args) {
        validateNumArgs(args, 2);
        retreiveState();
        boolean found = false;
        for (Commit com: _commits.values()) {
            if (com.getMsg().equals(args[1])) {
                found = true;
                System.out.println(com.getHashCode());
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Displays what branches currently exist, and marks the current branch
     * with a *. Also displays what files have been staged for addition
     * or removal.
     *
     * @param args Argument array from command line
     */
    public void statusCommand(String[] args) {
        validateNumArgs(args, 1);
        retreiveState();
        System.out.println("=== Branches ===");
        statusHelper(new ArrayList<>(_branches.keySet()), true);
        System.out.println("=== Staged Files ===");
        statusHelper(new ArrayList<>(_addStage.keySet()));
        System.out.println("=== Removed Files ===");
        statusHelper(new ArrayList<>(_rmStage.keySet()));

        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> files = Utils.plainFilenamesIn(_CWD);
        List<String> untracked = new ArrayList<>();
        for (String fileName: files) {
            if ((_head.contains(fileName)
                    && !sameContents(_head.getBlobFromFile(fileName), fileName)
                    && !_addStage.containsKey(fileName))
                    || (_addStage.containsKey(fileName)
                    && !sameContents(_addStage.get(fileName), fileName))) {
                System.out.println(fileName + " (modified)");
            } else if (!fileExists(fileName) && (_addStage.containsKey(fileName)
                    || (!_rmStage.containsKey(fileName)
                    && _head.contains(fileName)))) {
                System.out.println(fileName + " (deleted)");
            } else if (!_addStage.containsKey(fileName)
                    && !_rmStage.containsKey(fileName)
                    && !_head.contains(fileName)) {
                untracked.add(fileName);
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        statusHelper(untracked);
    }

    /** Prints out file NAMES in correct format for statusCommand.
     * BRANCH checks if we are printing out branch names. */
    private void statusHelper(List<String> names, boolean branch) {
        Collections.sort(names);
        for (String elem: names) {
            if (branch && elem.equals(_currBranch)) {
                System.out.print("*");
            }
            System.out.println(elem);
        }
        System.out.println();
    }

    /** Calls statusHelper on NAMES with branch set to false. */
    private void statusHelper(List<String> names) {
        statusHelper(names, false);
    }

    /** Manages the three types of checkout commands.
     * @param args Argument array from command line
     */
    public void checkoutCommand(String[] args) {
        retreiveState();
        int n = args.length;
        if (n == 2) {
            checkoutBranch(args[1]);
        } else if (n == 3) {
            checkoutFile(args[1], args[2]);
        } else if (n == 4) {
            checkoutFileWithCommitID(args[1], args[2], args[3]);
        } else {
            throw new GitletException("Incorrect operands.");
        }
        saveCurrentState();
    }

    /** Takes the version of FILENAME as it exists in the head commit, the
     * front of the current branch, and puts it in the working directory,
     * overwriting the version of the file that's already there if there
     * is one. The new version of the file is not staged. Also verifies DASHES.
     */
    private void checkoutFile(String dashes, String fileName) {
        checkoutFile(_head, dashes, fileName);
    }

    /** Takes the version of FILENAME as it exists in the commit with the
     * given COMMITID, and puts it in the working directory, overwriting the
     * version of the file that's already there if there is one. The new
     * version of the file is not staged. Also verifies DASHES.
     * */
    private void checkoutFileWithCommitID(String commitID,
                                          String dashes, String fileName) {
        validateID(commitID);
        Commit com = findCommit(commitID);
        if (com == null) {
            throw new GitletException("No commit with that id exists.");
        }
        checkoutFile(com, dashes, fileName);
    }

    /** Takes all files in the commit at the head of the BRANCHNAME, and
     * puts them in the working directory, overwriting the versions of the
     * files that are already there if they exist. Also, at the end of this
     * command, the given branch will now be considered the current branch
     * (_HEAD). Any files that are tracked in the current branch but are not
     * present in the checked-out branch are deleted. The staging area is
     * cleared, unless the checked-out branch is the current branch.
     * */
    private void checkoutBranch(String branchName) {
        Commit branchHead = _branches.get(branchName);
        if (branchHead == null) {
            throw new GitletException("No such branch exists.");
        } else if (_currBranch.equals(branchName)) {
            throw new GitletException("No need to checkout "
                    + "the current branch.");
        }
        checkoutCommit(branchHead);
        _currBranch = branchName;
    }

    /** Puts FILENAME being tracked by Commit COM in _CWD.
     * Also verifies DASHES. */
    private void checkoutFile(Commit com, String dashes, String fileName) {
        if (!dashes.equals("--")) {
            throw new GitletException("Incorrect operands.");
        }
        File file = Utils.join(_CWD, fileName);
        if (!com.contains(fileName)) {
            throw new GitletException("File does not exist in that commit.");
        } else {
            if (!fileExists(fileName)) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    System.out.println("Could not create file");
                }
            }
            Utils.writeContents(file,
                    com.getFileContents(fileName));
        }
    }

    /** Puts all files in Commit COM in _CWD. */
    private void checkoutCommit(Commit com) {
        for (String fileName: com.getBlobs().keySet()) {
            if (!_head.contains(fileName) && fileExists(fileName)) {
                throw new GitletException("There is an untracked file "
                        + "in the way; delete it or add and commit it first.");
            }
            checkoutFile(com, "--", fileName);
        }
        for (String fileName: _head.getBlobs().keySet()) {
            if (!com.contains(fileName)) {
                deleteFile(fileName);
            }
        }
        _head = com;
        _addStage.clear();
        _rmStage.clear();
    }

    /** Creates a new branch with the given name, and points it at the
     * current head node. A branch is nothing more than a name for a
     * reference (a SHA-1 identifier) to a commit node. This command
     * does NOT immediately switch to the newly created branch (just
     * as in real Git). Before you ever call branch, your code should
     * be running with a default branch called "master".
     *
     * @param args Argument array from command line
     */
    public void branchCommand(String[] args) {
        validateNumArgs(args, 2);
        retreiveState();
        if (_branches.containsKey(args[1])) {
            throw new GitletException("A branch with that name already "
                    + "exists.");
        }
        _branches.put(args[1], _head);
        saveCurrentState();
    }

    /** Deletes the branch with the given name. This only means to
     * delete the pointer associated with the branch; it does not
     * mean to delete all commits that were created under the branch,
     * or anything like that.
     *
     * @param args Argument array from command line
     */
    public void rmBranchCommand(String[] args) {
        validateNumArgs(args, 2);
        retreiveState();
        if (!_branches.containsKey(args[1])) {
            throw new GitletException("A branch with that name "
                    + "does not exist.");
        } else if (_currBranch.equals(args[1])) {
            throw new GitletException("Cannot remove the current branch.");
        } else {
            _branches.remove(args[1]);
        }
        saveCurrentState();
    }

    /** Checks out all the files tracked by the given commit. Removes
     * tracked files that are not present in that commit. Also moves
     * the current branch's head to that commit node. See the intro
     * for an example of what happens to the head pointer after using
     * reset. The [commit id] may be abbreviated as for checkout. The
     * staging area is cleared. The command is essentially checkout
     * of an arbitrary commit that also changes the current branch head.
     *
     * @param args Argument array from command line
     */
    public void resetCommand(String[] args) {
        validateNumArgs(args, 2);
        validateID(args[1]);
        retreiveState();
        Commit com = findCommit(args[1]);
        if (com == null) {
            throw new GitletException("No commit with that id exists.");
        }
        checkoutCommit(com);
        _branches.put(_currBranch, com);
        saveCurrentState();
    }

    /** Merges files from the given branch into the current branch.
     * @param args Argument array from command line
     */
    public void mergeCommand(String[] args) {
        validateNumArgs(args, 2);
        retreiveState();
        String branchName = args[1];
        Commit branchHead = _branches.get(branchName);
        if (_addStage.size() != 0 || _rmStage.size() != 0) {
            throw new GitletException("You have uncommitted changes.");
        } else if (branchHead == null) {
            throw new GitletException("A branch with that name does "
                    + "not exist.");
        } else if (_currBranch.equals(branchName)) {
            throw new GitletException("Cannot merge a branch with itself.");
        }
        Commit splitPoint = findSplitPoint(new HashSet<>(), _head,
                branchHead, 0, new HashMap<>());
        if (splitPoint.equals(branchHead)) {
            System.out.println("Given branch is an ancestor of the "
                    + "current branch.");
        } else if (splitPoint.equals(_head)) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
        } else {
            List<String> confFiles = compareMergeFiles(splitPoint, branchHead);
            writeConflicts(confFiles, branchHead);
            saveCurrentState();
            commitCommand(new String[]{"commit", "Merged " + branchName
                    + " into " + _currBranch + "."});
            _head.setMergeParent(branchHead);
        }
        saveCurrentState();
    }

    /** Compares merge files based on SPLITPOINT and BRANCHHEAD,
     * adding and removing the necessary files. Returns the list
     * of files with merge conflicts. */
    private List<String> compareMergeFiles(Commit splitPoint,
                                           Commit branchHead) {
        HashSet<String> allFiles = new HashSet<>();
        allFiles.addAll(_head.getBlobs().keySet());
        allFiles.addAll(branchHead.getBlobs().keySet());
        allFiles.addAll(splitPoint.getBlobs().keySet());
        List<String> conflictedFiles = new ArrayList<>();
        for (String fileName: allFiles) {
            if (!_head.contains(fileName) && fileExists(fileName)
                    && branchHead.contains(fileName)
                    && !sameContents(branchHead.getBlobFromFile(fileName),
                        fileName)) {
                throw new GitletException("There is an untracked file "
                        + "in the way; delete it, or add and commit it first.");
            } else if ((containedIn(new Commit[]{splitPoint, branchHead, _head},
                    fileName)
                        && diffContent(splitPoint, branchHead, fileName)
                        && !diffContent(splitPoint, _head, fileName))
                    || (!splitPoint.contains(fileName)
                        && !_head.contains(fileName)
                        && branchHead.contains(fileName))) {
                checkoutFileWithCommitID(branchHead.getHashCode(),
                        "--", fileName);
                _addStage.put(fileName, branchHead.getBlobFromFile(fileName));
                saveCurrentState();
            } else if (!branchHead.contains(fileName)
                    && splitPoint.contains(fileName)
                    && _head.contains(fileName)
                    && !diffContent(splitPoint, _head, fileName)) {
                rmCommand(new String[]{"rm", fileName});
            } else if ((containedIn(new Commit[]{splitPoint, branchHead, _head},
                    fileName)
                    && diffContent(splitPoint, _head, fileName)
                    && diffContent(splitPoint, branchHead, fileName)
                    && diffContent(_head, branchHead, fileName))
                    || (splitPoint.contains(fileName)
                    && !_head.contains(fileName)
                    && branchHead.contains(fileName)
                    && diffContent(splitPoint, branchHead, fileName))
                    || (splitPoint.contains(fileName)
                    && _head.contains(fileName)
                    && !branchHead.contains(fileName)
                    && diffContent(splitPoint, _head, fileName))
                    || (!splitPoint.contains(fileName)
                    && _head.contains(fileName)
                    && branchHead.contains(fileName)
                    && diffContent(_head, branchHead, fileName))) {
                conflictedFiles.add(fileName);
            }
        }
        return conflictedFiles;
    }

    /** Write contents of the CONFLICTEDFILES based on contents in
     * BRANCHHEAD and _head. */
    private void writeConflicts(List<String> conflictedFiles,
                                Commit branchHead) {
        if (conflictedFiles.size() > 0) {
            for (String fileName: conflictedFiles) {
                String currContent = ""; String givenContent = "";
                if (_head.contains(fileName)) {
                    currContent = _head.getFileContents(fileName);
                }
                if (branchHead.contains(fileName)) {
                    givenContent = branchHead.getFileContents(fileName);
                }
                String content = "<<<<<<< HEAD\n" + currContent
                        + "=======\n" + givenContent + ">>>>>>>\n";
                File file = Utils.join(_CWD, fileName);
                Utils.writeContents(file, content);
                _addStage.put(fileName, new Blob(content));
                System.out.println("Encountered a merge conflict.");
            }
        }
    }

    /** Returns commit that is split point of two branches starting
     * with CURR and GIVEN. SEEN keeps track of seen commits. DIST is
     * number of commits away from _head and POSSSPLITS keeps track of
     * possible split points. */
    private Commit findSplitPoint(HashSet<String> seen, Commit curr,
                                  Commit given, int dist,
                                  HashMap<Integer, String> possSplits) {
        if (curr == null && given == null) {
            return null;
        }
        if (possSplits.size() > 0
                && dist > Collections.min(possSplits.keySet())) {
            return null;
        }
        if (curr != null) {
            if (seen.contains(curr.getHashCode())) {
                possSplits.clear();
                possSplits.put(dist, curr.getHashCode());
                return curr;
            }
            dist += 1;
            seen.add(curr.getHashCode());
        }
        if (given != null) {
            if (seen.contains(given.getHashCode())) {
                possSplits.clear();
                possSplits.put(dist, given.getHashCode());
                return given;
            }
            seen.add(given.getHashCode());
        }
        Commit parent = ((curr == null) ? null : curr.getParent());
        Commit gParent = ((given == null) ? null : given.getParent());
        Commit mergeParent = ((curr == null) ? null : curr.getMergeParent());
        Commit gMergeParent = ((given == null) ? null : given.getMergeParent());

        findSplitPoint(seen, parent, gParent, dist, possSplits);
        if (mergeParent != null) {
            findSplitPoint(seen, mergeParent, gParent, dist, possSplits);
        }
        if (gMergeParent != null) {
            findSplitPoint(seen, parent, gMergeParent, dist, possSplits);
        }
        if (mergeParent != null && gMergeParent != null) {
            findSplitPoint(seen, mergeParent, gMergeParent, dist, possSplits);
        }
        String hashcode = possSplits.get(Collections.min(possSplits.keySet()));
        return _commits.get(hashcode);
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a GitletException if they do not match.
     *
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    private static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            throw new GitletException("Incorrect Operands");
        }
    }

    /** Checks if COMMITID is correct length. */
    private void validateID(String commitID) {
        if (commitID.length() > Utils.UID_LENGTH) {
            throw new GitletException("Incorrect Operands");
        }
    }

    /** Returns commit with COMMITID. */
    private Commit findCommit(String commitID) {
        for (String hash: _commits.keySet()) {
            if (hash.substring(0, commitID.length()).equals(commitID)) {
                return _commits.get(hash);
            }
        }
        return null;
    }

    /** Deletes FILENAME from _CWD. */
    private void deleteFile(String fileName) {
        File file = Utils.join(_CWD, fileName);
        Utils.restrictedDelete(file);
    }

    /** Returns true if FILENAME exist in _CWD. */
    private boolean fileExists(String fileName) {
        return Utils.join(_CWD, fileName).exists();
    }

    /** Returns content of FILENAME. */
    private String getFileContent(String fileName) {
        File file = Utils.join(_CWD, fileName);
        return Utils.readContentsAsString(file);
    }

    /** Returns true if FILENAME is tracked by all commits in COMS. */
    private boolean containedIn(Commit[] coms, String fileName) {
        for (Commit com: coms) {
            if (!com.contains(fileName)) {
                return false;
            }
        }
        return true;
    }

    /** Returns true FILENAME is different in COM1 and COM2. */
    private boolean diffContent(Commit com1, Commit com2, String fileName) {
        return !com1.getFileContents(fileName).equals(
                com2.getFileContents(fileName));
    }

    /** Returns true if contents of BLOB equal contents of FILENAME. */
    private boolean sameContents(Blob blob, String fileName) {
        if (!Utils.join(_CWD, fileName).exists()) {
            return false;
        }
        String content = getFileContent(fileName);
        return blob.getContents().equals(content);
    }

    /** Gets current state of repo. */
    @SuppressWarnings("unchecked")
    private void retreiveState() {
        _head = Utils.readObject(_HEAD, Commit.class);
        _currBranch = Utils.readContentsAsString(_CURRBRANCH);
        _addStage = Utils.readObject(_ADDSTAGE, HashMap.class);
        _rmStage = Utils.readObject(_RMSTAGE, HashMap.class);
        _branches = Utils.readObject(_BRANCHES, HashMap.class);
        _commits = Utils.readObject(_COMMITS, HashMap.class);
    }

    /** Saves current state of repo. */
    private void saveCurrentState() {
        Utils.writeObject(_ADDSTAGE, _addStage);
        Utils.writeObject(_RMSTAGE, _rmStage);
        Utils.writeObject(_BRANCHES, _branches);
        Utils.writeObject(_COMMITS, _commits);
        Utils.writeObject(_HEAD, _head);
        Utils.writeContents(_CURRBRANCH, _currBranch);
    }


    /** Staging area for addition. */
    private HashMap<String, Blob> _addStage;

    /** Staging area for removal. */
    private HashMap<String, Blob> _rmStage;

    /** Maps branch names to their head commit. */
    private HashMap<String, Commit> _branches;

    /** Maps commit ids to their commit. */
    private HashMap<String, Commit> _commits;

    /** Curent branch name. */
    private String _currBranch;

    /** Head commit. */
    private Commit _head;
}
