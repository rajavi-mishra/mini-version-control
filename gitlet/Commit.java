package gitlet;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/** Commit class.
 *  @author Rajavi Mishra
 */

public class Commit implements Serializable {
    /** blob collection folder. **/
    private File blobCol = new File(".gitlet/blobCol/");

    /** create commits.
     * @param myTimestamp (time)
     * @param myLogMessage (message)
     * @param myParent1uniqueId (parent 1)
     * @param myParent2uniqueId (parent 2)
     * @param myFileToBlob (mapping)
     */
    public Commit(Date myTimestamp, String myLogMessage,
                  String myParent1uniqueId, String myParent2uniqueId,
                  HashMap<String, String> myFileToBlob) {
        String pattern = "EEE MMM dd HH:mm:ss yyyy Z";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String date = simpleDateFormat.format(myTimestamp);
        _timestamp = date;
        _logMessage = myLogMessage;
        _parent1uniqueId = myParent1uniqueId;
        _parent2uniqueId = myParent2uniqueId;
        _uniqueId = computeSHA();
        _fileBlob = myFileToBlob;
        _allBlobs = new ArrayList<>();
        _allFileNames = new ArrayList<>();
        if (!_fileBlob.isEmpty()) {
            for (HashMap.Entry<String, String> entry: _fileBlob.entrySet()) {
                Blob myBlob = Utils.readObject(Utils.join(blobCol,
                        entry.getValue()), Blob.class);
                _allBlobs.add(myBlob);
                _allFileNames.add(entry.getKey());
            }
        } else {
            _fileBlob = null;
        }
    }

     /** get SHA.
      *
      * @return sha
      */
    String computeSHA() {
        String rv = _timestamp + _logMessage
                + _parent1uniqueId + _parent2uniqueId;
        if (_fileBlob != null) {
            rv = Utils.sha1(_fileBlob, rv);
        } else {
            rv = Utils.sha1(rv);
        }
        return rv;
    }

     /** get timestamp.
      *
      * @return _timestamp
      */
    String getTimestamp() {
        return _timestamp;
    }

     /** get message.
      *
      * @return _logMessage
      */
    String getLogMessage() {
        return _logMessage;
    }

     /** get my id.
      *
      * @return _uniqueId
      */
    String getMyUniqueId() {
        return _uniqueId;
    }

     /** get parent1 id.
      *
      * @return _parent1uniqueId
      */
    String getParent1uniqueId() {
        return _parent1uniqueId;
    }

     /** get all file names.
      *
      * @return _allFileNames
      */
    ArrayList<String> getAllFileNames() {
        return _allFileNames;
    }

     /** get parent2 id.
      *
      * @return _parent2uniqueId
      */
    String getParent2uniqueId() {
        return _parent2uniqueId;
    }

     /** get file to blobs mapping.
      *
      * @return _fileBlob
      */
    HashMap<String, String> getFileBlob() {
        return _fileBlob;
    }

     /** get all blobs.
      *
      * @return _allBlobs
      */
    ArrayList<Blob> getAllBlobs() {
        return _allBlobs;
    }

    /** timestamp. **/
    private String _timestamp;
    /** log message. **/
    private String _logMessage;
    /** parent 1. **/
    private String _parent1uniqueId;
    /** parent 2. **/
    private String _parent2uniqueId;
    /** all file names. **/
    private ArrayList<String> _allFileNames;
    /** all blobs. **/
    private ArrayList<Blob> _allBlobs;
    /** file name to blob id map. **/
    private HashMap<String, String> _fileBlob;
    /** my unique ID. **/
    private String _uniqueId;
}
