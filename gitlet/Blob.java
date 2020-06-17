package gitlet;
import java.io.File;
import java.io.Serializable;

/** Blob class.
 *  @author Rajavi Mishra
 */

public class Blob implements Serializable {

    /** create blobs. */
    /** @param myFile (file path)*/
    Blob(File myFile) {
        _file = myFile;
        _byteArr = Utils.readContents(_file);
        _uniqueId = computeSHA(_file);
    }

    /** computer SHA.*/
    /** @param file (file path)
    /** @return _uniqueId */
    String computeSHA(File file) {
        _uniqueId = Utils.sha1(getByteArr());
        return _uniqueId;
    }

    /** get Byte Array. */
    /** @return _byteArr (get byte array)*/
    byte[] getByteArr() {
        return _byteArr;
    }

    /** get UniqueId. */
    /** @return _uniqueId (get unique ID)*/
    String getUniqueId() {
        return _uniqueId;
    }

    /** byte Array. */
    private byte[] _byteArr;
    /** File path. */
    private File _file;
    /** unique ID. */
    private  String _uniqueId;
}
