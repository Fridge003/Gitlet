package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import gitlet.Staging;
import gitlet.Utils.*;
import static gitlet.Utils.*;
import gitlet.Commit;

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

    /** The file under .gitlet/heads that points to the current commit*/
    private File head;
    /** The commit that "head" points to */
    private Commit headCommit;
    /** index records all the files in the staging area */
    private Staging index;


    /** The Constructor of Repository instance */
    public Repository() {

        /** The directory structure */
        pathDict.put("cwd", CWD);
        pathDict.put("gitlet", GITLET_DIR);
        pathDict.put("objects", join(GITLET_DIR, "objects"));
        pathDict.put("refs", join(GITLET_DIR, "refs"));
        pathDict.put("heads", join(GITLET_DIR, "refs", "heads"));
        pathDict.put("remotes", join(GITLET_DIR, "refs", "remotes"));
        pathDict.put("HEAD", join(GITLET_DIR, "HEAD"));
        pathDict.put("index", join(GITLET_DIR, "index"));

        /** Set up the current working space*/
        if (!isInitialized()) {
            head = null;
            headCommit = null;
            index = null;
        } else {
            head = new File(readContentsAsString(pathDict.get("HEAD")));
            String headCommitHash = readContentsAsString(join(pathDict.get("gitlet"), head.getPath()));
            headCommit = readObject(hashToPath(headCommitHash), Commit.class);
            index = readObject(pathDict.get("index"), Staging.class);
        }
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
            System.exit(0);
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

            // Initialize HEAD file
            head = new File(join("refs", "heads", "master").getPath());
            writeContents(pathDict.get("HEAD"), head.getPath());

            // Constructing initial commit
            headCommit = new Commit("initial commit", "null");
            headCommit.saveCommit();

            // Constructing master branch
            File masterBranch = join(pathDict.get("heads"), "master");
            String initialCommitHash = headCommit.getHash();
            writeContents(masterBranch, initialCommitHash);

            // Constructing an empty index file
            index = new Staging();
            writeObject(pathDict.get("index"), index);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }



    /** add a file to the staging area ( the staged files are recorded in .gitlet/index )*/
    public void add(String filePath) {
        File addedFile = new File(filePath);
        File indexPath = pathDict.get("index");
        if (!addedFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        String blobHash = sha1(readContents(addedFile));
        /** TODO
        if (same as the current commit version) {
            return;
        }
         */
        saveBlob(addedFile, blobHash);
        index.put(addedFile, blobHash);
        index.save();
    }



    /** Saves a snapshot of tracked files in the current commit and staging area
     * so they can be restored at a later time, creating a new commit. */
    public void commit() {


    }



    /** A small util function that maps a 40-char sha1 string to
     * the relative path of its corresponding commit file*/
    public static File hashToPath(String hashTag) {
        return join(pathDict.get("objects"), hashTag.substring(0, 2), hashTag.substring(2));
    }


    /** Save the file as a blob object under .gitlet/objects */
    public void saveBlob(File blob, String blobHash) {

        File blobPath = hashToPath(blobHash);

        // If this blob has been created before, then we don't need to save it again.
        if (blobPath.exists()) {
            return;
        }
        try {
            File blobDir = join(pathDict.get("objects"), blobHash.substring(0, 2));
            if (!blobDir.exists()) {
                blobDir.mkdir();
            }
            blobPath.createNewFile();
            writeContents(blobPath, readContents(blob));
            System.out.println("A New file saved in " + blobPath);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }



    // There should be a function to detect the difference between the current dir and the dir saved in headCommit



}
