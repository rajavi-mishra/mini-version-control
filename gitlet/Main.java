package gitlet;
import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Rajavi Mishra worked with Devarsh Dhanuka
 */
public class Main {
    /**machine.**/
    private static Machine machine = new Machine();
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println(" Please enter a command.");
            System.exit(0);
        } else {
            if (args[0].equals("init")) {
                initHelper(args);
            } else if (new File(".gitlet").exists()) {
                if (args[0].equals("add")) {
                    addHelper(args);
                } else if (args[0].equals("commit")) {
                    commitHelper(args);
                } else if (args[0].equals("rm")) {
                    if (operandCheck(2, args)) {
                        if (args.length == 2) {
                            File toRemove = new File("" + args[1]);
                            machine.rm(toRemove);
                        }
                    }
                } else if (args[0].equals("log")) {
                    if (operandCheck(1, args)) {
                        machine.log();
                    }
                } else if (args[0].equals("global-log")) {
                    if (operandCheck(1, args)) {
                        machine.globalLog();
                    }
                } else if (args[0].equals("find")) {
                    if (operandCheck(2, args)) {
                        machine.find(args[1]);
                    }
                } else if (args[0].equals("checkout")) {
                    checkoutHelper(args);
                } else if (args[0].equals("branch")) {
                    if (operandCheck(2, args)) {
                        machine.branch(args[1]);
                    }
                } else if (args[0].equals("status")) {
                    if (operandCheck(1, args)) {
                        machine.status();
                    }
                } else if (args[0].equals("rm-branch")) {
                    if (operandCheck(2, args)) {
                        machine.removeBranch(args[1]);
                    }
                } else if (args[0].equals("reset")) {
                    if (operandCheck(2, args)) {
                        machine.reset(args[1]);
                    }
                } else if (args[0].equals("merge")) {
                    mergeHelper(args);
                } else {
                    System.out.println("No command with that name exists.");
                }
            } else {
                System.out.println("Not in an initialized Gitlet directory.");
            }
        }
    }

    /** error finding method.*/
    /** @param size (command size)
     *  @param args (command)
    /** @return boolean*/
    static boolean operandCheck(int size, String... args) {
        if (args.length != size) {
            System.out.println("Incorrect operands.");
            System.exit(0);
            return false;
        } else {
            return true;
        }
    }

    /**helper.
     *
     * @param args (command)
     */
    static void commitHelper(String... args) {
        if (operandCheck(2, args)) {
            File stageToAdd = new
                    File(".gitlet/staging/toAdd/");
            File stageToDelete = new
                    File(".gitlet/staging/toDelete/");
            if (stageToAdd.listFiles().length != 0
                    || stageToDelete.listFiles().length != 0) {
                if (args.length == 2 & !args[1].equals("")) {
                    machine.commit(args[1], null);
                } else {
                    System.out.println("Please "
                            + "enter a commit message.");
                    System.exit(0);
                }
            } else {
                System.out.println("No changes added to the commit.");
                System.exit(0);
            }
        }
    }

    /**helper.
     *
     * @param args (command)
     */
    static void checkoutHelper(String... args) {
        if (args.length == 2) {
            machine.checkoutBranch(args[1]);
        } else if (args.length == 3) {
            if (args[1].equals("--")) {
                machine.checkoutBasic(args[2]);
            } else {
                System.out.println("Incorrect operands");
            }
        } else if (args.length == 4) {
            if (args[2].equals("--")) {
                machine.checkoutPrev(args[1], args[3]);
            } else {
                System.out.println("Incorrect operands");
            }
        } else {
            System.out.println("Incorrect operands.");
        }
    }

    /**helper.
     *
     * @param args (command)
     */
    static void initHelper(String... args) {
        if (operandCheck(1, args)) {
            File curr = new File(".gitlet");
            if (curr.exists()) {
                System.out.println("A Gitlet version-control "
                        + "system already exists "
                        + "in the current directory.");
                System.exit(0);
            } else {
                curr.mkdir();
                machine.init();
            }
        }
    }

    /**helper.
     *
     * @param args (command)
     */
    static void addHelper(String... args) {
        if (operandCheck(2, args)) {
            File toAdd = new File("" + args[1]);
            if (toAdd.exists()) {
                machine.add(toAdd);
            } else {
                System.out.println("File does not exist.");
                System.exit(0);
            }
        }
    }

    /** merger.
     *
     * @param args (args)
     */
    static void mergeHelper(String... args) {
        if (operandCheck(2, args)) {
            machine.merge(args[1]);
        }
    }
}
