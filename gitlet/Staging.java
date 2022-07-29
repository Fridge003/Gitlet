package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static gitlet.Utils.join;
import static gitlet.Utils.writeObject;

/** This simple class represents the staging area of gitlet */
public class Staging implements Serializable {

    /** "index" is a hashmap that maps the relative path of a file
     * to the filename of its blob(namely the sha1 of its content)
     */
    protected Map<String, String> additionIndex;
    protected Set<String> removalIndex;

    public Staging() {
        additionIndex = new HashMap<>();
        removalIndex = new HashSet<>();
    }

    /** Stage a file for addition. If the file is already in the index for addition, overwrite it*/
    public void add(String filePath, String blobHash) {
        additionIndex.put(filePath, blobHash);
    }

    /** Check whether a file has been staged for addition */
    public boolean stagedForAddition(String filePath) {
        return additionIndex.containsKey(filePath);
    }


    /** Return the sha1 of a file staged for addition, if this file hasn't been staged, return "null" */
    public String getBlobHash(String filePath) {
        if (stagedForAddition(filePath)) {
            return additionIndex.get(filePath);
        } else {
            return "null";
        }
    }

    /** Delete a file from the additionIndex */
    public void cancelAdd(String filePath) {
        if (stagedForAddition(filePath)) {
            additionIndex.remove(filePath);
        }
    }


    /** Stage a file for removal.*/
    public void remove(String filePath) {
        removalIndex.add(filePath);
    }

    /** Check whether a file has been staged for removal */
    public boolean stagedForRemoval(String filePath) {
        return removalIndex.contains(filePath);
    }

    /** Delete a file from the removalIndex */
    public void cancelRemove(String filePath) {
        if (stagedForRemoval(filePath)) {
            removalIndex.remove(filePath);
        }
    }


    public void clear() {
        additionIndex.clear();
        removalIndex.clear();
    }

    public int stageSize() {
        return additionIndex.size() + removalIndex.size();
    }


    /** Write the content of Staging object back to .gitlet/index */
    public void save() {
        File savePath = join( ".gitlet", "index");
        writeObject(savePath, this);
    }

}
