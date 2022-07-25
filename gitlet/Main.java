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

                // TODO: handle the `add [filename]` command
                break;
            // TODO: FILL THE REST IN
            case "commit":
                // TODO: Implement commit
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }
}
