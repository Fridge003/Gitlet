package gitlet;

import java.io.File;
import gitlet.Utils;

/** Driver class for Gitlet, a subset of the Git version-control system.
 */

public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {

        // At least one argument should be input
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];
        int argNum = args.length;
        Repository repo = new Repository();
        switch(firstArg) {
            case "init":
                Utils.operandCheck(1, argNum);
                repo.init();
                break;
            case "add":
                Utils.operandCheck(2, argNum);
                repo.add(args[1]);
                break;
            case "commit":
                if (args.length < 2) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                Utils.operandCheck(2, argNum);
                repo.commit(args[1]);
                break;
            case "rm":
                Utils.operandCheck(2, argNum);
                repo.rm(args[1]);
                break;
            case "log":
                Utils.operandCheck(1, argNum);
                repo.log();
                break;
            case "global-log":
                Utils.operandCheck(1, argNum);
                repo.globalLog();
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }
}
