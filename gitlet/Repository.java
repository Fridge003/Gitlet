package gitlet;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.io.IOException;
import static gitlet.Utils.*;
import static gitlet.RepoHelper.*;



/** Represents a gitlet repository. */
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
            headCommit = new Commit("initial commit", "null", null, null);
            headCommit.saveCommit();

            // Constructing master branch
            File masterBranch = join(pathDict.get("branches"), "master");
            headCommitHash = headCommit.getHash();
            writeContents(masterBranch, headCommitHash);

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
            raiseError("File does not exist.");
        }
        String blobHash = sha1(readContents(addedFile));


        // If the current working version of the file is identical to the version in the current commit,
        // do not stage it to be added, and remove it from the staging area if it is already there
        if (headCommit.tracked(filePath) && headCommit.getBlobHash(filePath).equals(blobHash)) {
            index.cancelAdd(filePath);
            index.save();
            return;
        }

        saveBlob(addedFile, blobHash);
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
            raiseError("No reason to remove the file.");
        }

        // Unstage the file if it is currently staged for addition.
        if (stagedForAddition) {
            index.cancelAdd(filePath);
         }

        // If the file is tracked in the current commit, stage it for removal
        if (trackedByCurrentCommit) {
            index.remove(filePath);
            // Ensure that the file has been deleted
            if (removedFile.exists()) {
                removedFile.delete();
            }
        }
        index.save();
    }


    /** Saves a snapshot of tracked files in the current commit and staging area
     * so they can be restored at a later time, creating a new commit.
     * */
    public void commit(String message, String secondParent) {

        checkInitializeCondition("commit");

        // an empty staging area leads to a failure case
        if (index.stageSize() == 0) {
            raiseError("No changes added to the commit.");
        }

        // Create a new commit that takes the current commit(represented by its sha1) as parent
        // Its blobmap is initialized with headCommit.blobMap
        Commit newCommit = new Commit(message, headCommitHash, headCommit.snapshot, secondParent);

        // Adding to the new commit the files for addition in the staging area
        for (Map.Entry<String, String> entry:index.additionIndex.entrySet()){
            newCommit.updateFileVersion(entry.getKey(), entry.getValue());
        }

        // Remove the files staged for removal from tracking
        for (String removedFile: index.removalIndex) {
            newCommit.removeFileFromTracking(removedFile);
        }

        // save the new commit
        newCommit.saveCommit();

        // Update the head of current branch
        String newCommitHash = newCommit.getHash();
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
        checkInitializeCondition("global-log");
        System.out.println(readContentsAsString(pathDict.get("log")));
    }


    /**  Prints out the ids of all commits that have the given commit message.
     *   This function is implemented through searching line by line in the log file that records all the commit information.
     * */
    public void find(String message) {
        checkInitializeCondition("find");

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
            raiseError("Found no commit with that message.");
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
                addedFiles.add(file);
                if (!addedHash.equals(fileHash)) {
                    // Staged for addition, but with different contents than in the working directory;
                    unstagedFiles.add(file + " (modified)");
                }
            } else if (headCommit.tracked(file)) {
                // Tracked in the current commit, czshanged in the working directory, but not staged
                if (!headCommit.getBlobHash(file).equals(fileHash)) {
                    unstagedFiles.add(file + " (modified)");
                }
            } else { // neither staged for addition nor be tracked by HEAD commit
                untrackedFiles.add(file);
            }
        }

        //  Tracked in the current commit and deleted from the working directory, but not staged for removal
        for (String file: headCommit.getTrackedFiles()) {
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

    /** Takes the version of the file as it exists in the head commit
     * and puts it in the working directory */
    public void restoreFile(String fileName) {
        checkInitializeCondition("checkout");
        if (!headCommit.tracked(fileName)) {
            raiseError("File does not exist in that commit.");
        }
        dumpBlob(fileName, headCommit.getBlobHash(fileName));
        index.cancelAdd(fileName);
        index.save();
    }

    /** Takes the version of the file as it exists in the commit with the given id,
     * and puts it in the working directory.
     * The commit ID is the six digits at the beginning of the 40-digit sha1 of commit */
    public void restoreFileGivenCommit(String fileName, String commitID) {
        checkInitializeCondition("checkout");

        File targetCommitPath = commitIDToPath(commitID);
        if (targetCommitPath == null) {
            raiseError("No commit with that id exists.");
        }

        Commit targetCommit = readObject(targetCommitPath, Commit.class);
        if (!targetCommit.tracked(fileName)) {
            raiseError("File does not exist in that commit.");
        }

        dumpBlob(fileName, targetCommit.getBlobHash(fileName));
        index.cancelAdd(fileName);
        index.save();
    }

    /** Takes all files in the commit at the head of the given branch, and puts them in the working directory,
     * overwriting the versions of the files that are already there if they exist.
     * Also, change the HEAD pointer to the given branch and empty the staging area. */
    public void checkoutBranch(String branchName) {
        checkInitializeCondition("checkout");

        if (head.getName().equals(branchName)) {
            raiseError("No need to checkout the current branch.");
        }

        File targetBranch = join(".gitlet", "branches", branchName);
        if (!targetBranch.exists()) {
            raiseError("No such branch exists.");
        }

        File targetCommitPath = hashToPath(readContentsAsString(targetBranch));
        Commit targetCommit = readObject(targetCommitPath, Commit.class); // The commit to restore.

        // Clean unneeded files in the working dir
        List<String> currentFiles = plainFilenamesIn(CWD);
        for (String fileName: currentFiles) {
            boolean trackedByHead = headCommit.tracked(fileName);
            boolean trackedByTarget = targetCommit.tracked(fileName);
            if (!trackedByHead) {
                if (trackedByTarget) {
                    raiseError("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            } else {
                if (!trackedByTarget) {
                    File thisFile = join(CWD, fileName);
                    thisFile.delete();
                }
            }
        }

        // Dump all the content tracked by target commit
        for (String fileName: targetCommit.getTrackedFiles()) {
            dumpBlob(fileName, targetCommit.getBlobHash(fileName));
        }

        // Move HEAD to the given branch
        writeContents(pathDict.get("HEAD"), targetBranch.getPath());

        // empty the staging area
        index.clear();
        index.save();
    }


    /** Creates a new branch with the given name, and points it at the current head commit. */
    public void branch(String branchName) {
        checkInitializeCondition("branch");

        File newBranch = join(pathDict.get("branches"), branchName);
        if (newBranch.exists()) {
            raiseError("A branch with that name already exists.");
        }
        writeContents(newBranch, headCommitHash);
    }

    /** Deletes the branch with the given name. */
    public void rmBranch(String branchName) {
        checkInitializeCondition("rm-branch");

        if (head.getName().equals(branchName)) {
            raiseError("Cannot remove the current branch.");
        }

        File removedBranch = join(pathDict.get("branches"), branchName);
        if (!removedBranch.exists()) {
            raiseError("A branch with that name does not exist.");
        }

        removedBranch.delete();
    }

    /** Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch’s head to that commit node.
     * The ID given should be 6-digit */
    public void reset(String commitID) {
        checkInitializeCondition("reset");

        File targetCommitPath = commitIDToPath(commitID);
        if (targetCommitPath == null) {
            raiseError("No commit with that id exists.");
        }

        Commit targetCommit = readObject(targetCommitPath, Commit.class); // The commit to reset

        // Clean unneeded files in the working dir
        List<String> currentFiles = plainFilenamesIn(CWD);
        for (String fileName: currentFiles) {
            boolean trackedByHead = headCommit.tracked(fileName);
            boolean trackedByTarget = targetCommit.tracked(fileName);
            if (!trackedByHead) {
                if (trackedByTarget) {
                    raiseError("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            } else {
                if (!trackedByTarget) {
                    File thisFile = join(CWD, fileName);
                    thisFile.delete();
                }
            }
        }

        // Dump all the content tracked by target commit
        for (String fileName: targetCommit.getTrackedFiles()) {
            dumpBlob(fileName, targetCommit.getBlobHash(fileName));
        }

        // Move the current branch's head to target commit
        writeContents(head, commitIDToSHA1(commitID));

        // empty the staging area
        index.clear();
        index.save();
    }


    /**  Merges files from the given branch into the current branch. */
    public void merge(String branchName) {
        checkInitializeCondition("merge");

        // First deal with failure cases
        if (index.stageSize() > 0) {
            raiseError("You have uncommitted changes.");
        }
        if (head.getName().equals(branchName)) {
            raiseError("Cannot merge a branch with itself.");
        }
        File targetBranch = join(".gitlet", "branches", branchName);
        if (!targetBranch.exists()) {
            raiseError("No such branch exists.");
        }

        String targetCommitHash = readContentsAsString(targetBranch);
        Commit targetCommit = readObject(hashToPath(targetCommitHash), Commit.class);

        // Search for the split commit (latest common ancester) of current commit and given commit
        String splitCommitHash = findSplit(headCommitHash, targetCommitHash);
        Commit splitCommit = readObject(hashToPath(splitCommitHash), Commit.class);

        // Cases when givenBranch and head lie on the same line
        if (splitCommitHash.equals(targetCommitHash)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        if (splitCommitHash.equals(headCommitHash)) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(branchName);
            return;
        }

        // Search for untracked dangerous files
        List<String> currentFiles = plainFilenamesIn(CWD);
        for (String fileName: currentFiles) {
            boolean trackedByHead = headCommit.tracked(fileName);
            boolean trackedByTarget = targetCommit.tracked(fileName);
            boolean trackedBySplit = splitCommit.tracked(fileName);
            if (!trackedByHead && (trackedByTarget || trackedBySplit)) {
                raiseError("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }

        // Files not present in split commit, not present in current branch but present in given branch
        Set<String> filesTrackedByTarget = targetCommit.getTrackedFiles();
        for (String fileName: filesTrackedByTarget) {
            if (!splitCommit.tracked(fileName) && !headCommit.tracked(fileName)) {
                String blobhash = targetCommit.getBlobHash(fileName);
                dumpBlob(fileName, blobhash); // checkout the file
                index.add(fileName, blobhash); // stage for addition
            }
        }

        // Next deal with all the files present in split commit
        boolean conflict = false;
        Set<String> filesTrackedBySplit = splitCommit.getTrackedFiles();
        for (String fileName: filesTrackedBySplit) {
            File file = new File(fileName);
            boolean trackedByHead = headCommit.tracked(fileName);
            boolean trackedByTarget = targetCommit.tracked(fileName);
            boolean modifiedInHead = !headCommit.getBlobHash(fileName).equals(splitCommit.getBlobHash(fileName));
            boolean modifiedInTarget = !targetCommit.getBlobHash(fileName).equals(splitCommit.getBlobHash(fileName));


            // Files modified in given branch but unmodified in current branch, take the modified version (including deletion)
            if (!modifiedInHead && modifiedInTarget) {
                if (trackedByTarget) {
                    String blobhash = targetCommit.getBlobHash(fileName);
                    dumpBlob(fileName, blobhash); // checkout the file
                    index.add(fileName, blobhash); // stage for addition
                } else {
                    index.remove(fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }

            // If the file is modified in current branch and given branch in different ways, report a conflict
            if (modifiedInHead && modifiedInTarget) {
                if (!headCommit.getBlobHash(fileName).equals(targetCommit.getBlobHash(fileName))) {
                    conflict = true;
                    String headContent = "";
                    String targetContent = "";
                    if (trackedByHead) {
                        headContent = readContentsAsString(hashToPath(headCommit.getBlobHash(fileName)));
                    }
                    if (trackedByTarget) {
                        targetContent = readContentsAsString(hashToPath(targetCommit.getBlobHash(fileName)));
                    }
                    String conflictContent = "<<<<<<< HEAD\n" + headContent
                            + "=======\n" + targetContent
                            + ">>>>>>>\n";
                    writeContents(file, conflictContent);
                    String conflictHash = sha1(readContents(file));
                    saveBlob(file, conflictHash);
                    index.add(fileName, conflictHash);
                }
            }
        }


        // Initialize the merged commit with head commit
        // Commit mergedCommit = readObject(hashToPath(headCommitHash), Commit.class);
        if (index.stageSize() > 0) {
            commit("Merged " + branchName + " into " + head.getName() + ".", targetCommitHash);
            if (conflict) {
                System.out.println("Encountered a merge conflict.");
            }
        }

    }
}
