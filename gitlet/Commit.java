package gitlet;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import static gitlet.Utils.*;
import gitlet.Utils.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Represents a gitlet commit object. */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;

    /** The time when the commit is created.
     * If it is the initial commit, timestamp will be set to UNIX timestamp
     * */
    private String timestamp;

    /** The parent commit of the current commit, represented as a 40-digit SHA1 string
     * containing the filename of serialized parent commit.
     * There is only one such parent since the commit history is a tree-like structure.
     * */
    private String parent;


    /** Second parent is only useful when merging. Similarly to parent, also a 40-digit SHA1 string */
    private String secondParent;

    /** Snapshot is hash map that map the relative paths of files tracked by this commit
     *  to their sha1 (since different files have different sha1, sha1 can denote the version of a file)
     */
    protected Map<String, String> snapshot;

    /** Constructor of Commit class */
    public Commit(String message, String parent, Map<String, String> parentSnapshot, String secondParent) {
        this.message = message;
        this.parent = parent;
        this.snapshot = new HashMap<>();
        this.secondParent = secondParent;
        if (parent == "null") { // The parent will be "null" only when the commit is "initial commit"
            Date date = new Date(0);
            this.timestamp = date.toString();
        } else {
            Date date = new Date();
            this.timestamp = date.toString();
            // Copy the blobMap from its parent
            for (Map.Entry<String, String> entry:parentSnapshot.entrySet()){
                this.snapshot.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public String getMessage() {
        return this.message;
    }

    public String getParent() {
        return this.parent;
    }

    public String getSecondParent() {return this.secondParent;}


    /** Get the SHA1 hash of the concatenation of metadata and content */
    public String getHash() {
        return sha1(serialize(this));
    }

    public Set<String> getTrackedFiles() {
        return snapshot.keySet();
    }

    public boolean tracked(String filePath) {
        return snapshot.containsKey(filePath);
    }


    /** Input the relative path of a file, if this file is tracked by the current commit, return its sha1 hash,
     * else return null */
    public String getBlobHash(String filePath) {
        if (snapshot.containsKey(filePath)) {
            return snapshot.get(filePath);
        } else {
            return "null";
        }
    }

    /** Update the version of a file through adding/changing the blob it maps to */
    public void updateFileVersion(String filePath, String blobHash) {
        snapshot.put(filePath, blobHash);
    }

    /** Stop this commit from tracking a file  */
    public void removeFileFromTracking(String filePath) {
        snapshot.remove(filePath);
    }

    /** Log message in proper format, including hashID, timestamp, and commit message.
     *  If the commit has multiple parents, contain the merge information.
     * */

    public String getLogMessage() {
        String log = "===\n";
        log = log + "commit " + getHash() + "\n";
        if (secondParent != null) {
            log = log + "Merge: " + parent.substring(0, 6) + " " + secondParent.substring(0, 6) + "\n";
        }
        log = log + "Date: " + timestamp + "\n";
        log = log + message + "\n";
        return log;
    }



    /**  serialize current commit and save it to directory .gitlet/objects/
     *   the filename of this commit will be the SHA1 of its metadata and content
     * */
    public void saveCommit() {
        File objectDir = join(System.getProperty("user.dir"), ".gitlet", "objects");
        String commitHash = getHash();
        try {
            File saveDir = join(objectDir, commitHash.substring(0, 2));
            if (!saveDir.exists()) {
                saveDir.mkdir();
            }
            File commitFile = join(saveDir, commitHash.substring(2));
            commitFile.createNewFile();
            writeObject(commitFile, this);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
