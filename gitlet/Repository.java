package gitlet;

import java.io.File;
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

    private String head;
    private Commit headCommit;


    /** The Constructor of Repository instance */
    public Repository() {
        head = "refs/heads/master";
        headCommit = new Commit("initial commit", "null");
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
            GITLET_DIR.mkdir();
            File objects = join(GITLET_DIR, "objects");
            objects.mkdir();
            File refs = join(GITLET_DIR, "refs");
            refs.mkdir();
            File heads = join(refs, "heads");
            heads.mkdir();
            File remotes = join(refs, "remotes");
            remotes.mkdir();
            File HEAD = join(GITLET_DIR, "HEAD");
            HEAD.createNewFile();
            File index = join(GITLET_DIR, "index");
            index.createNewFile();

            // setting the initial commit
            Utils.writeContents(HEAD, head);
            headCommit.saveCommit(objects);
            File masterBranch = join(heads, "master");
            String initialCommitHash = headCommit.getHash();
            Utils.writeContents(masterBranch, initialCommitHash);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }


    }

    // There should be a function to detect the difference between the current dir and the dir saved in headCommit



}
