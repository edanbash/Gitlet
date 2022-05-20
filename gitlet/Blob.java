package gitlet;

import java.io.Serializable;

/** The blob object.
 *  @author Edan Bash
 */
public class Blob implements Serializable {

    /** Number of blobs created. */
    private static int blobNum = 0;

    /** Constructor for blob object with NAME and CONTENT. */
    public Blob(String content) {
        _content = content;
        _name = "Blob " + blobNum;
        blobNum += 1;
    }

    /** Returns string contents of the blob. */
    public String getContents() {
        return _content;
    }

    /** Returns name of the blob. */
    public String getName() {
        return _name;
    }

    /** Name of Blob. */
    private String _name;

    /** String content within Blob. */
    private String _content;
}
