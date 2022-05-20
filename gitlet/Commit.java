package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

/** The commit object.
 *  @author Edan Bash
 */
public class Commit implements Serializable {

    /** Date format for all commits. */
    private static final DateFormat DATE_FORMAT =
            new SimpleDateFormat("EEE MMM dd HH:mm:ss YYYY Z");

    /** Constructor for Commit object with LOGMESSAGE AND PARENT.
     * Contains BLOBS that assign tracked files to their contents.
     * Created at time DATE. */
    public Commit(String logMessage, Commit parent, HashMap<String,
            Blob> blobs, Date date) {
        _logMessage = logMessage;
        _parent = parent;
        _mergeParent = null;
        _blobs = new HashMap<>();
        for (Map.Entry elem: blobs.entrySet()) {
            _blobs.put((String) elem.getKey(), (Blob) elem.getValue());
        }
        _date = DATE_FORMAT.format(date);
    }

    /** Add files from ADDSTAGE and remove files in RMSTAGE for this
     * commit object. */
    public void updateCommit(HashMap<String, Blob> addStage,
                             HashMap<String, Blob> rmStage) {
        if (addStage.size() == 0 && rmStage.size() == 0 && _parent != null) {
            throw new GitletException("No changes added to the commit.");
        }
        for (Map.Entry elem: addStage.entrySet()) {
            _blobs.put((String) elem.getKey(), (Blob) elem.getValue());
        }
        for (String elem: rmStage.keySet()) {
            _blobs.remove(elem);
        }
        _hashcode = Utils.sha1(Utils.serialize(this));
    }

    /** Return log message of commit. */
    public String getMsg() {
        return _logMessage;
    }

    /** Return parent of commit. */
    public Commit getParent() {
        return _parent;
    }

    /** Return date commit was created. */
    public String getDate() {
        return _date;
    }

    /** Return blobs associated with this commit. */
    public HashMap<String, Blob> getBlobs() {
        return _blobs;
    }

    /** Return true if commit tracks FILENAME. */
    public boolean contains(String fileName) {
        return _blobs.get(fileName) != null;
    }

    /** Return Blob associated with FILENAME. */
    public Blob getBlobFromFile(String fileName) {
        return _blobs.get(fileName);
    }

    /** Return file contents of FILENAME. */
    public String getFileContents(String fileName) {
        return _blobs.get(fileName).getContents();
    }

    /** Returns merge parent of this commit. */
    public Commit getMergeParent() {
        return _mergeParent;
    }

    /** Sets merge parent to COM for this commit. */
    public void setMergeParent(Commit com) {
        _mergeParent = com;
    }

    /** Returns true of COM and THIS have same hashcode. */
    public boolean equals(Commit com) {
        return _hashcode.equals(com.getHashCode());
    }

    /** Returns hashcode of commit. */
    public String getHashCode() {
        return _hashcode;
    }

    /** Log message of this commit. */
    private String _logMessage;

    /** Parent of this commit. */
    private Commit _parent;

    /** Blobs associated with this commit. */
    private HashMap<String, Blob> _blobs;

    /** Date created of this commit. */
    private final String _date;

    /** Merge parent of this commit. */
    private Commit _mergeParent;

    /** Hashcode of this commit. */
    private String _hashcode;
}
