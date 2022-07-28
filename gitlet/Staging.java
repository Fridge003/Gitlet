package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static gitlet.Utils.join;
import static gitlet.Utils.writeObject;

/** This simple class represents the staging area of gitlet */
public class Staging implements Serializable {

    /** "index" is a hashmap that maps the reletive path of a file
     * to the filename of its blob(namely the sha1 of its content)
     */
    private Map<String, String> index;
    private File savePath = join( ".gitlet", "index");

    public Staging() {
        index = new HashMap<>();
    }

    public void put(File filePath, String blobHash) {
        index.put(filePath.getPath(), blobHash);
    }

    public void clear() {
        index.clear();
    }

    public int size() {
        return index.size();
    }

    /** Write the content of Staging object back to .gitlet/index */
    public void save() {
        writeObject(savePath, this);
    }

}
