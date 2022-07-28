package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import gitlet.Staging;
import gitlet.Utils.*;
import static gitlet.Utils.*;
import gitlet.Commit;


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

    /** The file under .gitlet/refs/heads that stores sha1 of the current commit*/
    private File head;

    /** The commit that "head" points to */
    private Commit headCommit;
    private String headCommitHash;
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
        pathDict.put("log", join(GITLET_DIR, "log"));

        /** Set up the current working space*/
        if (!GITLET_DIR.exists()) {
            head = null;
            headCommitHash = null;
            headCommit = null;
            index = null;
        } else {
            head = join(pathDict.get("gitlet"), readContentsAsString(pathDict.get("HEAD")));
            headCommitHash = readContentsAsString(head);
            headCommit = readObject(hashToPath(headCommitHash), Commit.class);
            index = readObject(pathDict.get("index"), Staging.class);
        }
    }

    /** If the repo hasn't been initialized, use init to initialize it by establishing the .gitlet directory */
    public void init() {

        checkInitializeCondition("init");

        try {
            // establishing the directory stucture of .gitlet
            pathDict.get("gitlet").mkdir();
            pathDict.get("objects").mkdir();
            pathDict.get("refs").mkdir();
            pathDict.get("heads").mkdir();
            pathDict.get("remotes").mkdir();
            pathDict.get("HEAD").createNewFile();
            pathDict.get("index").createNewFile();
            pathDict.get("log").createNewFile();

            // Initialize HEAD file
            head = new File(join("refs", "heads", "master").getPath());
            writeContents(pathDict.get("HEAD"), head.getPath());

            // Constructing initial commit
            headCommit = new Commit("initial commit", "null", null);
            headCommit.saveCommit();

            // Constructing master branch
            File masterBranch = join(pathDict.get("heads"), "master");
            String initialCommitHash = headCommit.getHash();
            writeContents(masterBranch, initialCommitHash);

            // Constructing an empty index file
            index = new Staging();
            writeObject(pathDict.get("index"), index);

            // Record the initial commit to log
            writeContents(pathDict.get("log"), headCommit.getLogMessage());

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }



    /** add a file to the staging area ( the staged files are recorded in .gitlet/index )*/
    public void add(String filePath) {

        checkInitializeCondition("add");

        File addedFile = new File(filePath);
        if (!addedFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        String blobHash = sha1(readContents(addedFile));


        // If the current working version of the file is identical to the version in the current commit,
        // do not stage it to be added, and remove it from the staging area if it is already there
        if (headCommit.tracked(filePath) && headCommit.getBlobVersion(filePath).equals(blobHash)) {
            if(index.stagedForAddition(filePath)) {
                index.cancelAdd(filePath);
                System.out.println(filePath + " removed from staging area.");
            }
            index.save();
            return;
        }

        saveBlob(addedFile, blobHash);
        System.out.println("File saved in " + blobHash);
        index.add(addedFile.getPath(), blobHash);
        index.save();
    }

    /** Unstage the file if it is currently staged for addition.
     * If the file is tracked in the current commit, stage it for removal
     * */
    public void rm(String filePath) {

        checkInitializeCondition("rm");

        File removedFile = new File(filePath);
        boolean stagedForAddition = index.stagedForAddition(filePath);
        boolean trackedByCurrentCommit = headCommit.tracked(filePath);


        if ((!stagedForAddition) && (!trackedByCurrentCommit)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }


        // Unstage the file if it is currently staged for addition.
        if (stagedForAddition) {
            index.cancelAdd(filePath);
            System.out.println(filePath + "removed from staging area.");
         }

        // If the file is tracked in the current commit, stage it for removal
        // and remove the file from the working directory
        if (trackedByCurrentCommit) {
            index.remove(filePath);
            if (removedFile.exists()) {
                removedFile.delete();
            }
        }

        index.save();

    }


    /** Saves a snapshot of tracked files in the current commit and staging area
     * so they can be restored at a later time, creating a new commit.
     * */
    public void commit(String message) {

        checkInitializeCondition("commit");

        // an empty staging area leads to a failure case
        if (index.stageSize() == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // Create a new commit that takes the current commit(represented by its sha1) as parent
        // Its blobmap is initialized with headCommit.blobMap
        Commit newCommit = new Commit(message, headCommitHash, headCommit.snapshot);

        // Adding to the new commit the files for addition in the staging area
        for (Map.Entry<String, String> entry:index.additionIndex.entrySet()){
            newCommit.updateFileVersion(entry.getKey(), entry.getValue());
            System.out.println("Addition: " + "key:"+entry.getKey()+"  value:"+ entry.getValue());
        }

        // Remove the files staged for removal from tracking
        for (String removedFile: index.removalIndex) {
            newCommit.removeFileFromTracking(removedFile);
            System.out.println("Removal: " + removedFile);
        }

        // save the new commit
        newCommit.saveCommit();

        // Update the head of current branch
        String newCommitHash = newCommit.getHash();
        System.out.println("Head commit changed from " + headCommitHash + " to " + newCommitHash);
        writeContents(head, newCommitHash);

        // Empty the staging area
        index.clear();
        index.save();

        // Record the commit information to the global log
        String log_message = readContentsAsString(pathDict.get("log"));
        writeContents(pathDict.get("log"), newCommit.getLogMessage() + "\n" + log_message);
    }

    /** Printing the information about each commit backwards along the commit tree until the initial commit */
    public void log() {
        checkInitializeCondition("log");
        Commit commitPointer = headCommit;
        String commitHash = headCommitHash;
        while(true) {
            // Print the information
            System.out.println(commitPointer.getLogMessage());

            // Stop after printing initial commit
            if (commitPointer.getMessage().equals("initial commit")) {
                break;
            }

            // Shift to parent commit
            commitHash = commitPointer.getParent();
            commitPointer = readObject(hashToPath(commitHash), Commit.class);

        }
    }

    public void globalLog() {
        System.out.println(readContentsAsString(pathDict.get("log")));
    }


    /** If the repo has been initialized, command "init" will cause failure;
     * If the repo hasn't been initialized, commands other than "init" will cause failure.
     */
    public void checkInitializeCondition(String cmd) {
        boolean initialized = GITLET_DIR.exists();
        if (cmd.equals("init")) {
            if (initialized) {
                System.out.println("A Gitlet version-control system already exists in the current directory.");
                System.exit(0);
            }
        } else {
            if (!initialized) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
        }
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
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    // There should be a function to detect the difference between the current dir and the dir saved in headCommit



}
