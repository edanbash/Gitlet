package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Edan Bash
 */
public class Main {

    /** Current Working Directory. */
    static final File CWD = new File(".");

    /** File that stores repo object after init. */
    static final File REPO = Utils.join(CWD, ".gitlet/repo");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            if (args.length == 0) {
                System.out.println("Please enter a command.");
            } else if (args[0].equals("init")) {
                createRepository();
            } else {
                Repository repo;
                try {
                    repo = Utils.readObject(REPO, Repository.class);
                } catch (IllegalArgumentException i) {
                    throw new GitletException("Not in an initialized "
                            + "Gitlet directory.");
                }
                switch (args[0]) {
                case "add":
                    repo.addCommand(args);
                    break;
                case "commit":
                    repo.commitCommand(args);
                    break;
                case "rm":
                    repo.rmCommand(args);
                    break;
                case "log":
                    repo.logCommand(args);
                    break;
                case "global-log":
                    repo.globalLogCommand(args);
                    break;
                case "find":
                    repo.findCommand(args);
                    break;
                case "status":
                    repo.statusCommand(args);
                    break;
                case "checkout":
                    repo.checkoutCommand(args);
                    break;
                case "branch":
                    repo.branchCommand(args);
                    break;
                case "rm-branch":
                    repo.rmBranchCommand(args);
                    break;
                case "reset":
                    repo.resetCommand(args);
                    break;
                case "merge":
                    repo.mergeCommand(args);
                    break;
                default:
                    throw new GitletException("No command with "
                           +  "that name exists.");
                }
            }
        } catch (GitletException g) {
            System.out.println(g.getMessage()); System.exit(0);
        }
    }

    /** Creates new Repository. */
    private static void createRepository() {
        File gitDir = Utils.join(CWD, ".gitlet");
        if (!gitDir.exists()) {
            Repository repo = new Repository();
            Utils.writeObject(REPO, repo);
        } else {
            throw new GitletException("A Gitlet version-control "
                    + "system already exists in the current directory.");
        }
    }
}
