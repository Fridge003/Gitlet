package gitlet;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.io.IOException;

import gitlet.Staging;
import gitlet.Utils.*;
import static gitlet.Utils.*;
import gitlet.Commit;


/** Represents a gitlet repository.
 *  @author TODO: merge support in log command; extra points in status command
 *  TODO: cancel the reomtes folder, only use a branch folder
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
        pathDict.put("branches", join(GITLET_DIR, "branches"));
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
            head = new File(readContentsAsString(pathDict.get("HEAD")));
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
            pathDict.get("branches").mkdir();
            pathDict.get("HEAD").createNewFile();
            pathDict.get("index").createNewFile();
            pathDict.get("log").createNewFile();

            // Initialize HEAD file
            head = new File(join(".gitlet", "branches", "master").getPath());
            writeContents(pathDict.get("HEAD"), head.getPath());

            // Constructing initial commit
            headCommit = new Commit("initial commit", "null", null);
            headCommit.saveCommit();

            // Constructing master branch
            File masterBranch = join(pathDict.get("branches"), "master");
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
        if (headCommit.tracked(filePath) && headCommit.getBlobHash(filePath).equals(blobHash)) {
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
        if (trackedByCurrentCommit) {
            index.remove(filePath);
        }

        // Ensure that the file has been deleted
        if (removedFile.exists()) {
            removedFile.delete();
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


    /**  Prints out the ids of all commits that have the given commit message.
     *   This function is implemented through searching line by line in the log file that records all the commit information.
     * */
    public void find(String message) {
        BufferedReader reader;
        boolean found = false;
        try {
            reader = new BufferedReader(new FileReader(pathDict.get("log")));
            String dummyLine= reader.readLine();
            String commitLine = "";
            String messageLine = "";
            while (dummyLine != null) {
                commitLine = reader.readLine();
                dummyLine = reader.readLine();
                messageLine = reader.readLine();
                // Commit with given message is found
                if (messageLine.equals(message)) {
                    System.out.println(commitLine.substring(7));
                    found = true;
                }
                dummyLine = reader.readLine();
                dummyLine = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Displays what branches currently exist, and marks the current branch with a *.
     * Also displays what files have been staged for addition or removal.
     * */
    public void status() {
        checkInitializeCondition("status");

        // Print branches
        System.out.println("=== Branches ===");
        List<String> branches = plainFilenamesIn(pathDict.get("branches"));
        for (String filename: branches) {
            if (head.getName().equals(filename)) {
                System.out.println("*" + filename);
            } else {
                System.out.println(filename);
            }
        }
        System.out.print("\n");

        // Collect all the files in the current working place.
        // Ignore the functional files including "Makefile",".gitignore", "README.md", "pom.xml"
        Set<String> currentFiles = new HashSet<String>(plainFilenamesIn(CWD));
        List<String> ignoredFiles = Arrays.asList("Makefile", ".gitignore", "README.md", "pom.xml", ".DS_Store");
        currentFiles.removeAll(ignoredFiles);

        // Then catalogue the collected files into four disjoint sets
        TreeSet<String> addedFiles = new TreeSet<>();
        TreeSet<String> removedFiles = new TreeSet<>();
        TreeSet<String> unstagedFiles = new TreeSet<>();
        TreeSet<String> untrackedFiles = new TreeSet<>();

        for (String file: currentFiles) {
            String fileHash = sha1(readContents(new File(file)));
            if (index.stagedForAddition(file)) {
                // If recorded in the staging area for addition
                String addedHash = index.getBlobHash(file);
                if (addedHash.equals(fileHash)) {
                    // The contents in working dir are identical to the staged version
                    addedFiles.add(file);
                } else {
                    // Staged for addition, but with different contents than in the working directory;
                    unstagedFiles.add(file + " (modified)");
                }
            } else if (headCommit.tracked(file)) {
                // Tracked in the current commit, changed in the working directory, but not staged
                if (!headCommit.getBlobHash(file).equals(fileHash)) {
                    unstagedFiles.add(file + " (modified)");
                }
            } else { // neither staged for addition nor be tracked by HEAD commit
                untrackedFiles.add(file);
            }
        }

        //  Tracked in the current commit and deleted from the working directory, but not staged for removal
        for (String file: headCommit.snapshot.keySet()) {
            if ((!currentFiles.contains(file)) && (!index.stagedForRemoval(file))) {
                unstagedFiles.add(file + " (deleted)");
            }
        }

        // Staged for addition, but deleted in the working directory
        for (String file: index.additionIndex.keySet()) {
            if (!currentFiles.contains(file)) {
                unstagedFiles.add(file + " (deleted)");
            }
        }

        // Consider the files staged for removal
        removedFiles.addAll(index.removalIndex);

        // Print the contents of the four sets one after another.
        // Since the TreeSet automatically sorts its elements, they are printed in lexicographical order.
        System.out.println("=== Staged Files ===");
        for (String file:addedFiles) {
            System.out.println(file);
        }
        System.out.print("\n");

        System.out.println("=== Removed Files ===");
        for (String file:removedFiles) {
            System.out.println(file);
        }
        System.out.print("\n");

        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String file:unstagedFiles) {
            System.out.println(file);
        }
        System.out.print("\n");

        System.out.println("=== Untracked Files ===");
        for (String file:untrackedFiles) {
            System.out.println(file);
        }
        System.out.print("\n");
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

    /** Dump the content of a blob to a file with given path */
    public void readBlob(String filePath, String blobHash) {



    }


}
