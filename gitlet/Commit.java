package gitlet;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import static gitlet.Utils.*;
import gitlet.Utils.*;
import java.io.File;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
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

    /** The parent commit of the current commit, represented as a SHA1 string
     * containing the filename of serialized parent commit.
     * There is only one such parent since the commit history is a tree-like structure.
     * */
    private String parent;

    /** Constructor of Commit class */
    public Commit(String message, String parent) {
        this.message = message;
        this.parent = parent;
        if (parent == "null") { // The parent of initial commit will be "null"
            Date date = new Date(0);
            this.timestamp = date.toString();
        } else {
            Date date = new Date();
            this.timestamp = date.toString();
        }
    }

    public String getMessage() {
        return this.message;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getParent() {
        return this.parent;
    }

    /** Get the SHA1 hash of the concatenation of metadata and content */
    public String getHash() {
        return sha1(message, timestamp, parent);
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


    // main function is created for debugging
    public static void main(String[] args) {
        Commit c1 = new Commit("commit 1", "null");
        Commit c2 = new Commit("commit 2", "sdfsdf");
        System.out.println(c1.getHash());
        System.out.println(c2.getHash());

    }

}
