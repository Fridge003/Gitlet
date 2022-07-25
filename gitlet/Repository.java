package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import gitlet.Utils;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** A dictionary that stores all the frequently used paths as File objects */
    public static final Map<String, File> pathDict = new HashMap<>();

    private String head;
    private Commit headCommit;


    /** The Constructor of Repository instance */
    public Repository() {
        head = null;
        headCommit = null;
        pathDict.put("cwd", CWD);
        pathDict.put("gitlet", GITLET_DIR);
        pathDict.put("objects", join(GITLET_DIR, "objects"));
        pathDict.put("refs", join(GITLET_DIR, "refs"));
        pathDict.put("heads", join(GITLET_DIR, "refs", "heads"));
        pathDict.put("remotes", join(GITLET_DIR, "refs", "remotes"));
        pathDict.put("HEAD", join(GITLET_DIR, "HEAD"));
        pathDict.put("index", join(GITLET_DIR, "index"));
    }


    /** Whether this repository has been initialized by gitlet */
    public boolean isInitialized() {
        return GITLET_DIR.exists();
    }

    /** If the repo hasn't been initialized, use init to initialize it by establishing the .gitlet directory */
    public void init() {
        // If the repo has already been initialized, print the error message and abort
        if (isInitialized()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }

        try {
            // establishing the directory stucture of .gitlet
            pathDict.get("gitlet").mkdir();
            pathDict.get("objects").mkdir();
            pathDict.get("refs").mkdir();
            pathDict.get("heads").mkdir();
            pathDict.get("remotes").mkdir();
            pathDict.get("HEAD").createNewFile();
            pathDict.get("index").createNewFile();

            // setting the initial commit
            head = "refs/heads/master";
            headCommit = new Commit("initial commit", "null");
            Utils.writeContents(pathDict.get("HEAD"), head);
            headCommit.saveCommit();
            File masterBranch = join(pathDict.get("heads"), "master");
            String initialCommitHash = headCommit.getHash();
            Utils.writeContents(masterBranch, initialCommitHash);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }


    }

    // There should be a function to detect the difference between the current dir and the dir saved in headCommit



}
