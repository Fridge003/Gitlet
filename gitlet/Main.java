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
        Repository repo = new Repository();
        switch(firstArg) {
            case "init":
                // initialize the repository if it hasn't been initialized
                repo.init();
                break;
            case "add":
                // add a file to the staging area
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                repo.add(args[1]);
                break;
            case "commit":
                if (args.length < 2) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                repo.commit(args[1]);
                break;
            case "rm":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                repo.rm(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }
}
