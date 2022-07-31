package gitlet;

import java.awt.datatransfer.SystemFlavorMap;
import java.io.File;
import java.io.IOException;
import java.util.*;
import static gitlet.Utils.*;
import gitlet.Commit;
import java.util.List;

public class RepoHelper {

    public static final File OBJECTS = join(".gitlet", "objects");

    /** If repo has been initialized, command "init" will cause failure;
     * If repo hasn't been initialized, commands other than "init" will cause failure.
     */
    public static void checkInitializeCondition(String cmd) {
        File GITLET = new File(".gitlet");
        boolean initialized = GITLET.exists();
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
    public static File hashToPath(String hashID) {
        return join(OBJECTS, hashID.substring(0, 2), hashID.substring(2));
    }


    /** Save the file as a blob object under .gitlet/objects */
    public static void saveBlob(File blob, String blobHash) {

        File blobPath = hashToPath(blobHash);

        // If this blob has been created before, then we don't need to save it again.
        if (blobPath.exists()) {
            return;
        }

        try {
            File blobDir = join(OBJECTS, blobHash.substring(0, 2));
            if (!blobDir.exists()) {
                blobDir.mkdir();
            }
            blobPath.createNewFile();
            writeContents(blobPath, readContents(blob));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    /** Read the content of a serialized blob object and put it in given file path.
     *  Overwriting that file if it's already there
     */
    public static void dumpBlob(String filePath, String blobHash) {
        File blobPath = hashToPath(blobHash);
        if (!blobPath.exists()) {
            System.out.println("Unable to load " + blobPath.getPath());
            return;
        }
        File writtenFile = new File(filePath);
        writeContents(writtenFile, readContents(blobPath));
    }

    /** Given the 6-digit ID of a commit.
     *  If such commit exists, return the file path of its blob; else return null
     * */
    public static File commitIDToPath(String commitID) {
        File dir = join(".gitlet", "objects", commitID.substring(0, 2));
        if (!dir.exists()) {
            return null;
        }
        List<String> blobList = plainFilenamesIn(dir);
        for (String blobName: blobList) {
            if (blobName.substring(0, 4).equals(commitID.substring(2))) {
                return join(dir, blobName);
            }
        }
        return null;
    }

    /** Given the 6-digit ID of a commit.
     *  If such commit exists, return the 40-digit SHA1 of its blob; else return null
     * */
    public static String commitIDToSHA1(String commitID) {
        File dir = join(".gitlet", "objects", commitID.substring(0, 2));
        if (!dir.exists()) {
            return null;
        }
        List<String> blobList = plainFilenamesIn(dir);
        for (String blobName: blobList) {
            if (blobName.substring(0, 4).equals(commitID.substring(2))) {
                return commitID.substring(0, 2) + blobName;
            }
        }
        return null;
    }

    /** Given 40-digit SHA1 of a commit,
     * return a set of strings containing all its ancestors */
    public static Set<String> getAncestors(String commitHash) {
        Set<String> ancestors = new HashSet<>();
        ancestors.add(commitHash);
        File commitPath = hashToPath(commitHash);
        Commit commit = readObject(commitPath, Commit.class);
        if (commit.getMessage().equals("initial commit")) {
            return ancestors;
        }
        ancestors.addAll(getAncestors(commit.getParent()));
        if (commit.getSecondParent() != null) {
            ancestors.addAll(getAncestors(commit.getSecondParent()));
        }
        return ancestors;
    }


    /** Given 40-digit SHA1 of two commits,
     * find their most recent ancestor (namely the split of history tree) and return its 40-digit SHA1 */
    public static String findSplit(String commitHash1, String commitHash2) {

        File commitPath1 = hashToPath(commitHash1);
        if (!commitPath1.exists()) {
            raiseError("Commit with ID:" + commitHash1 + " doesn't exist.");
        }

        File commitPath2 = hashToPath(commitHash2);
        if (!commitPath1.exists()) {
            raiseError("Commit with ID:" + commitHash2 + " doesn't exist.");
        }

        // stores all the ancestors of the first commit (including itself)
        Set<String> ancestorsOfCommit1= getAncestors(commitHash1);

        // Then iterate through the ancestors of the second commit from most recent to least recent
        // If iterating to a common ancestor of the two commits, stop and return its sha1
        // Using bfs to do this
        Commit commitPointer = null;
        String currentCommitHash = commitHash2;
        Deque<String> dq = new ArrayDeque<>();
        dq.addLast(currentCommitHash);
        while(!dq.isEmpty()) {
            int layerSize = dq.size();
            for (int i = 0; i < layerSize; ++i) {
                currentCommitHash = dq.pop();
                if (ancestorsOfCommit1.contains(currentCommitHash)) {
                    return currentCommitHash;
                }
                commitPointer = readObject(hashToPath(currentCommitHash), Commit.class);
                if (commitPointer.getParent() != null) {
                    dq.addLast(commitPointer.getParent());
                }
                if (commitPointer.getSecondParent() != null) {
                    dq.addLast(commitPointer.getSecondParent());
                }
            }
        }
        return null;
    }
}
