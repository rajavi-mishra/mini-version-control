package gitlet;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;


/** command machine for gitlet.
 *  @author Rajavi Mishra
 */

public class Machine {
    /** staging folder. **/
    private File staging = new File(".gitlet/staging/");
    /** addition folder. **/
    private File stageToAdd = new File(".gitlet/staging/toAdd/");
    /** deletion folder. **/
    private File stageToDelete = new File(".gitlet/staging/toDelete/");
    /** commit folder. **/
    private File commitCol = new File(".gitlet/commitCol/");
    /** blob folder. **/
    private File blobCol = new File(".gitlet/blobCol/");
    /** branch folder. **/
    private File branchCol = new File(".gitlet/branchCol/");
    /** head folder. **/
    private File headCommit = new File(".gitlet/head/");
    /** id abbreviation bound. **/
    public static final int UPPER_LIMIT = 40;

    /** init. **/
    void init() {
        Date initialTimestamp = new Date();
        initialTimestamp.setTime(0);
        HashMap<String, String> fileCol = new HashMap<>();
        Commit initialCommit = new Commit(initialTimestamp, "initial commit",
                null, null, fileCol);
        commitCol.mkdir();
        commitToFolder(initialCommit);
        branchCol.mkdir();
        branchToFolder(branchCol, initialCommit, "master");
        headCommit.mkdir();
        branchToFolder(headCommit, initialCommit, "master");
        staging.mkdir();
        stageToAdd.mkdir();
        stageToDelete.mkdir();
        blobCol.mkdir();
    }

    /** add.
     * @param file (file)
     */
    void add(File file) {
        File checkAdd = Utils.join(stageToAdd, file.getName());
        File checkDelete = Utils.join(stageToDelete, file.getName());
        Blob forToAdd = new Blob(file);
        String check = forToAdd.getUniqueId();

        if (checkDelete.exists()) {
            checkDelete.delete();
            stageToFolder(file, stageToAdd);
            blobToFolder(blobCol, new Blob(file));
        }

        boolean seenInCommit = false;
        File header = headCommit.listFiles()[0];
        Commit currCommit = Utils.readObject(header, Commit.class);
        if (currCommit.getFileBlob() != null) {
            for (HashMap.Entry<String, String> x
                    : currCommit.getFileBlob().entrySet()) {
                if (check.equals(x.getValue())
                        && file.getName().equals(x.getKey())) {
                    seenInCommit = true;
                    break;
                }
            }
        }
        if (seenInCommit) {
            if (checkAdd.exists()) {
                checkAdd.delete();
            }
            return;
        }  else {
            if (checkAdd.exists()) {
                checkAdd.delete();
                stageToFolder(file, stageToAdd);
                blobToFolder(blobCol, new Blob(file));
            } else {
                stageToFolder(file, stageToAdd);
                blobToFolder(blobCol, new Blob(file));
            }
        }
    }

    /** commit.
     *
     * @param message (m)
     * @param optional (o)
     */
    void commit(String message, String optional) {
        if (stageToAdd.listFiles().length != 0
                || stageToDelete.listFiles().length != 0) {
            File header = headCommit.listFiles()[0];
            Commit currCommit = Utils.readObject(header, Commit.class);
            HashMap<String, String> fileToBlob = new HashMap<>();
            if (currCommit.getFileBlob() != null
                    && !currCommit.getFileBlob().isEmpty()) {
                for (HashMap.Entry<String, String> entry
                        : currCommit.getFileBlob().entrySet()) {
                    fileToBlob.put(entry.getKey(), entry.getValue());
                }
            }
            if (stageToAdd.listFiles() != null) {
                for (File thisFile : stageToAdd.listFiles()) {
                    Blob thisBlob = new Blob(thisFile);
                    fileToBlob.put(thisFile.getName(), thisBlob.getUniqueId());
                }
            }
            if (stageToDelete.listFiles() != null) {
                for (File thisFile : stageToDelete.listFiles()) {
                    fileToBlob.remove(thisFile.getName());
                }
            }

            Date timestampNow = new Date();
            Commit newCommit = new Commit(timestampNow, message,
                    currCommit.getMyUniqueId(), optional, fileToBlob);
            for (Blob myBlob : newCommit.getAllBlobs()) {
                File toCheck = Utils.join(blobCol, myBlob.getUniqueId());
                if (!toCheck.exists()) {
                    blobToFolder(blobCol, myBlob);
                }
            }
            commitToFolder(newCommit);
            branchToFolder(headCommit, newCommit, header.getName());
            branchToFolder(branchCol, newCommit, header.getName());
            clearDir(stageToAdd);
            clearDir(stageToDelete);
        } else {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
    }

    /** remove.
     * @param file (file)
     */
    void rm(File file) {
        File header = headCommit.listFiles()[0];
        File toCheck = Utils.join(stageToAdd, file.getName());
        Commit currCommit = Utils.readObject(header, Commit.class);
        if (toCheck.exists()) {
            toCheck.delete();
        } else if (currCommit.getAllFileNames().contains(file.getName())) {
            File checking = new File("" + file.getName());
            if (checking.exists()) {
                stageToFolder(file, stageToDelete);
                Utils.restrictedDelete(file);
            } else {
                File forDeletion = Utils.join(stageToDelete, file.getName());
                Blob fileContent = Utils.readObject(Utils.join(blobCol,
                        currCommit.getFileBlob()
                                .get(file.getName())), Blob.class);
                Utils.writeContents(forDeletion, fileContent.getByteArr());
            }
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    /** log. **/
    void log() {
        File header = headCommit.listFiles()[0];
        Commit currCommit = Utils.readObject(header, Commit.class);
        while (currCommit.getParent1uniqueId() != null) {
            logger(currCommit);
            currCommit = Utils.readObject(new File(".gitlet/commitCol/"
                    + currCommit.getParent1uniqueId()), Commit.class);
        }
        logger(currCommit);
    }

    /** global log. **/
    void globalLog() {
        ArrayList<Commit> seen = new ArrayList<>();
        if (commitCol.listFiles() != null
                && commitCol.listFiles().length != 0) {
            for (File file : commitCol.listFiles()) {
                Commit currCommit = Utils.readObject(file, Commit.class);
                logger(currCommit);
            }
        }
    }

    /** find a commit.
     * @param message (string)
     */
    void find(String message) {
        boolean seen = false;
        if (commitCol.listFiles() != null && commitCol.length() > 0) {
            for (File file : commitCol.listFiles()) {
                Commit currCommit = Utils.readObject(file, Commit.class);
                if (currCommit.getLogMessage().equals(message)) {
                    System.out.println(currCommit.getMyUniqueId());
                    seen = true;
                }
            }
            if (!seen) {
                System.out.println("Found no commit with that message.");
                System.exit(0);
            }
        }
    }

    /** checkout with file name.
     * @param fileName (string)
     */
    void checkoutBasic(String fileName) {
        File header = headCommit.listFiles()[0];
        Commit currCommit = Utils.readObject(header, Commit.class);
        byte[] contentsToWrite = new byte[]{};
        if (currCommit.getFileBlob() != null) {
            if (currCommit.getFileBlob().containsKey(fileName)) {
                String identification = currCommit.getFileBlob().get(fileName);
                File thisFile = new File(".gitlet/blobCol/" + identification);
                Blob thisBlob = Utils.readObject(thisFile, Blob.class);
                contentsToWrite = thisBlob.getByteArr();
                File overrideFile = new File("" + fileName);
                if (overrideFile.exists()) {
                    overrideFile.delete();
                }
                Utils.writeContents(overrideFile, contentsToWrite);
            } else {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
        }
    }

    /** checkout with commitID.
     * @param commitId (string)
     * @param fileName (string)
     */
    void checkoutPrev(String commitId, String fileName) {
        commitId = helperID(commitId);
        if (Utils.plainFilenamesIn(commitCol).contains(commitId)) {
            Commit currCommit = Utils.readObject(new File(".gitlet/commitCol/"
                    + commitId), Commit.class);
            byte[] contentsToWrite = new byte[]{};
            if (currCommit.getFileBlob() != null) {
                if (currCommit.getFileBlob().containsKey(fileName)) {
                    String identification = currCommit
                            .getFileBlob().get(fileName);
                    File thisFile = new File(".gitlet/blobCol/"
                            + identification);
                    Blob thisBlob = Utils.readObject(thisFile, Blob.class);
                    contentsToWrite = thisBlob.getByteArr();
                    File overrideFile = new File("" + fileName);
                    if (overrideFile.exists()) {
                        overrideFile.delete();
                    }
                    Utils.writeContents(overrideFile, contentsToWrite);
                } else {
                    System.out.println("File does not exist in that commit.");
                    System.exit(0);
                }
            }
        } else {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
    }

    /** checkout Branch.
     * @param branchName (string)
     */
    void checkoutBranch(String branchName) {
        File toCheck = new File(".gitlet/branchCol/" + branchName);
        if (toCheck.exists()) {
            Commit branchToBe = Utils.readObject(toCheck, Commit.class);
            File header = headCommit.listFiles()[0];
            Commit headCom = Utils.readObject(header, Commit.class);
            if (!branchName.equals(header.getName())) {
                if (allTrackedCurrent(branchName)) {
                    byte[] contentsToWrite = new byte[]{};
                    if (branchToBe.getFileBlob() != null
                            && !branchToBe.getFileBlob().isEmpty()) {
                        for (HashMap.Entry<String, String> entry
                                : branchToBe.getFileBlob().entrySet()) {
                            File thisFile = new File(".gitlet/blobCol/"
                                    + entry.getValue());
                            Blob thisBlob = Utils
                                    .readObject(thisFile, Blob.class);
                            contentsToWrite = thisBlob.getByteArr();
                            File overrideFile = new File("" + entry.getKey());
                            if (overrideFile.exists()) {
                                overrideFile.delete();
                            }
                            Utils.writeContents(overrideFile, contentsToWrite);
                        }
                    }
                    for (String fileName: headCom.getAllFileNames()) {
                        if (!branchToBe.getAllFileNames().contains(fileName)) {
                            File thisFile = new File("" + fileName);
                            thisFile.delete();
                        }
                    }
                    header.delete();
                    branchToFolder(headCommit, branchToBe, branchName);
                    clearDir(stageToAdd);
                } else {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it or add it first.");
                    System.exit(0);
                }
            } else {
                System.out.println("No need to checkout the current branch.");
                System.exit(0);
            }
        } else {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
    }

    /** branch.
     * @param branchName (String)
     **/
    void branch(String branchName) {
        File header = headCommit.listFiles()[0];
        Commit headCom = Utils.readObject(header, Commit.class);
        if (header.getName().equals("master")) {
            File checkBranch = new File(".gitlet/branchCol/" + branchName);
            if (!checkBranch.exists()) {
                branchToFolder(branchCol, headCom, branchName);
            } else {
                System.out.println("A branch with that name already exists.");
                System.exit(0);
            }
        }
    }

    /** status. **/
    void status() {
        if (headCommit.listFiles() != null
                && headCommit.listFiles().length != 0) {
            System.out.println("=== Branches ===");
            String headComparator = headCommit.listFiles()[0].getName();
            File[] temp = branchCol.listFiles();
            Arrays.sort(temp);
            for (File thisFile : temp) {
                if (thisFile.getName().equals(headComparator)) {
                    System.out.println("*" + thisFile.getName());
                } else {
                    System.out.println(thisFile.getName());
                }
            }
            System.out.println();
            System.out.println("=== Staged Files ===");
            File[] temp2 = stageToAdd.listFiles();
            Arrays.sort(temp2);
            for (File thisFile : temp2) {
                System.out.println(thisFile.getName());
            }
            System.out.println();
            System.out.println("=== Removed Files ===");
            File[] temp3 = stageToDelete.listFiles();
            Arrays.sort(temp3);
            for (File thisFile : temp3) {
                System.out.println(thisFile.getName());
            }
            System.out.println();
            System.out.println("=== Modifications Not Staged For Commit ===");
            modificationEC();
            System.out.println();
            System.out.println("=== Untracked Files ===");
            untrackedEC();
            System.out.println();
        }
    }

    /** rm-branch.
     * @param branchName (String)
     * **/
    void removeBranch(String branchName) {
        File header = headCommit.listFiles()[0];
        if (!header.getName().equals(branchName)) {
            File toDelete = Utils.join(branchCol, branchName);
            if (toDelete.exists()) {
                toDelete.delete();
            } else {
                System.out.println("A branch with that name does not exist.");
                System.exit(0);
            }
        } else {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
    }

    /** reset.
     * @param commitId (String)
     * **/
    void reset(String commitId) {
        commitId = helperID(commitId);
        if (Utils.plainFilenamesIn(commitCol).contains(commitId)) {
            if (allTrackedCurrentID(commitId)) {
                File resetCommit = Utils.join(commitCol, commitId);
                Commit currCommit = Utils.readObject(resetCommit, Commit.class);
                if (currCommit.getAllFileNames().size() != 0) {
                    for (String fileName : currCommit.getAllFileNames()) {
                        checkoutPrev(commitId, fileName);
                    }
                }
                File header = headCommit.listFiles()[0];
                Commit headCom = Utils.readObject(header, Commit.class);
                if (headCom.getAllFileNames().size() != 0) {
                    for (String fileName : headCom.getAllFileNames()) {
                        if (currCommit.getAllFileNames() == null
                                || currCommit.getAllFileNames().size() == 0
                                || !currCommit.getAllFileNames()
                                .contains(fileName)) {
                            File toRemove = new File("" + fileName);
                            toRemove.delete();
                        }
                    }
                }
                branchToFolder(headCommit, currCommit, header.getName());
                branchToFolder(branchCol, currCommit, header.getName());
                clearDir(stageToAdd);
            } else {
                System.out.println("There is an untracked "
                        + "file in the way; delete it or add it first.");
            }
        } else {
            System.out.println("No commit with that id exists.");
        }
    }

    /**merge.
     *
     * @param branchName (branch name)
     */
    void merge(String branchName) {
        if (Utils.join(branchCol, branchName).exists()) {
            if (!branchName.equals(headCommit.listFiles()[0].getName())) {
                if (stageToAdd.listFiles().length == 0
                        && stageToDelete.listFiles().length == 0) {
                    if (allTrackedCurrent(branchName)) {
                        File header = headCommit.listFiles()[0];
                        File mergeBranch = Utils
                                .join(branchCol, branchName);
                        Commit currentCommit = Utils
                                .readObject(header, Commit.class);
                        Commit tempCurrent = Utils
                                .readObject(header, Commit.class);
                        Commit mergeCommit = Utils
                                .readObject(mergeBranch, Commit.class);
                        Commit tempMerge = Utils
                                .readObject(mergeBranch, Commit.class);
                        Commit errorM = Utils
                                .readObject(mergeBranch, Commit.class);
                        Commit errorC = Utils
                                .readObject(header, Commit.class);
                        error(errorC, errorM, branchName);
                        Commit splitPointLatest = splitFinder
                                (tempCurrent, tempMerge);
                        helperOneFive(mergeCommit,
                                splitPointLatest, currentCommit);
                        helperSix(mergeCommit,
                                splitPointLatest, currentCommit);
                        boolean check = conflictFinder(currentCommit, mergeCommit, splitPointLatest);
                        String message = "Merged " + branchName
                                + " into " + header.getName() + ".";
                        commit(message, mergeCommit.getMyUniqueId());
                        if (check) {
                            System.out.println("Encountered "
                                    + "a merge conflict.");
                        }
                    } else {
                        System.out.println("There is an untracked "
                                + "file in the way; "
                                + "delete it or add it first.");
                    }
                } else {
                    System.out.println("You have uncommitted changes.");
                }
            } else {
                System.out.println("Cannot merge a branch with itself.");
            }
        } else {
            System.out.println("A branch with that name does not exist.");
        }
    }

    /**error.
     *
     * @param errorC (com)
     * @param errorM (com)
     * @param branchName (string)
     */
    void error(Commit errorC, Commit errorM, String branchName) {
        Commit errorCopy = errorC;
        while (errorC.getParent1uniqueId() != null) {
            if (errorC.getMyUniqueId().equals(errorM.getMyUniqueId())) {
                System.out.println("Given branch is an "
                       + "ancestor of the current branch.");
                System.exit(0);
            }
            errorC = Utils.readObject(Utils.join(commitCol,
                    errorC.getParent1uniqueId()), Commit.class);
        }

        while (errorM.getParent1uniqueId() != null) {
            if (errorM.getMyUniqueId().equals(errorCopy.getMyUniqueId())) {
                checkoutBranch(branchName);
                System.out.println("Current branch fast-forwarded.");
                System.exit(0);
            }
            errorM = Utils.readObject(Utils.join(commitCol,
                    errorM.getParent1uniqueId()), Commit.class);
        }
    }

    /**helper for one and five.
     *
     * @param mergeCommit (m)
     * @param splitPointLatest (m)
     * @param currentCommit (m)
     */
    void helperOneFive(Commit mergeCommit,
                       Commit splitPointLatest,
                       Commit currentCommit) {
        for (String fileName : mergeCommit
                .getAllFileNames()) {
            if (splitPointLatest.getFileBlob() != null
                    && !mergeCommit
                    .getFileBlob().get(fileName)
                    .equals(splitPointLatest
                            .getFileBlob().get(fileName))) {
                if (currentCommit.getAllFileNames()
                        .contains(fileName)
                        && currentCommit
                        .getFileBlob().get(fileName)
                        .equals(splitPointLatest
                                .getFileBlob()
                                .get(fileName))) {
                    checkoutPrev(mergeCommit
                            .getMyUniqueId(), fileName);
                    File toUpdate = new File("" + fileName);
                    stageToFolder(toUpdate, stageToAdd);
                }
            }
            if (!splitPointLatest.getAllFileNames()
                    .contains(fileName)
                    && !currentCommit.getAllFileNames()
                    .contains(fileName)) {
                checkoutPrev(mergeCommit
                        .getMyUniqueId(), fileName);
                File thisFile = new File("" + fileName);
                stageToFolder(thisFile, stageToAdd);
            }
        }
    }

    /**helper for six.
     *
     * @param mergeCommit (m)
     * @param splitPointLatest (m)
     * @param currentCommit (m)
     */
    void helperSix(Commit mergeCommit,
                   Commit splitPointLatest, Commit currentCommit) {
        for (String fileName : splitPointLatest
                .getAllFileNames()) {
            if (!mergeCommit.getAllFileNames()
                    .contains(fileName)
                    && currentCommit.getFileBlob() != null
                    && splitPointLatest
                    .getFileBlob().get(fileName)
                    .equals(currentCommit
                            .getFileBlob().get(fileName))) {
                File thisFile = new File("" + fileName);
                rm(thisFile);
            }
        }
    }

    /**split finder.
     *
     * @param headBranch (head)
     * @param mergeBranch (merge)
     * @return commit
     */
    Commit splitFinder(Commit headBranch, Commit mergeBranch) {
        Commit rv = headBranch;
        if (headBranch.getParent1uniqueId()
                .equals(mergeBranch.getMyUniqueId())) {
            rv = mergeBranch;
        } else {
            while (headBranch.getParent1uniqueId() != null) {
                while (mergeBranch.getParent1uniqueId() != null) {
                    if (headBranch.getParent1uniqueId()
                            .equals(mergeBranch.getParent1uniqueId())) {
                        File temp = Utils.join(commitCol,
                                headBranch.getParent1uniqueId());
                        rv = Utils.readObject(temp, Commit.class);
                        return rv;
                    }
                    mergeBranch = Utils.readObject(Utils
                            .join(commitCol, mergeBranch
                                    .getParent1uniqueId()), Commit.class);
                }
                headBranch = Utils.readObject(Utils
                        .join(commitCol, headBranch
                                .getParent1uniqueId()), Commit.class);
            }
        }
        return rv;
    }

    /** commit folder.
     * @param object (commit)
     */
    private void commitToFolder(Commit object) {
        File destination = Utils.join(commitCol, object.getMyUniqueId());
        Utils.writeObject(destination, object);
    }

    /** branch & head folder.
     * @param file (file)
     * @param object (commit)
     * @param branchName (file)
     */
    private void branchToFolder(File file, Commit object, String branchName) {
        if (branchName != null) {
            File destination = Utils.join(file, branchName);
            Utils.writeObject(destination, object);
        }
    }

    /** blob folder.
     * @param file (file)
     * @param object (object)
     */
    private void blobToFolder(File file, Blob object) {
        File destination = Utils.join(file, object.getUniqueId());
        Utils.writeObject(destination, object);
    }

    /** staging / marking for deletion folder.
     * @param file (file)
     * @param dir (file)
     */
    private void stageToFolder(File file, File dir) {
        File destination = Utils.join(dir, file.getName());
        try {
            Files.copy(file.toPath(), destination.toPath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** clear directory.**/
    /** @param file (file) */
    private void clearDir(File file) {
        File[] temp = file.listFiles();
        for (File myFile: temp) {
            myFile.delete();
        }
    }

    /** log helper.**/
    /** @param myCommit (commit) */
    private void logger(Commit myCommit) {
        System.out.println("===");
        System.out.println("commit " + myCommit.getMyUniqueId());
        if (myCommit.getParent2uniqueId() != null) {
            String lineExtra = myCommit.getParent1uniqueId()
                    + " " + myCommit.getParent2uniqueId();
            System.out.println("Merge: "
                    + myCommit.getParent1uniqueId()
                    + " " + myCommit.getParent2uniqueId());
        }
        System.out.println("Date: " + myCommit.getTimestamp());
        System.out.println(myCommit.getLogMessage());
        System.out.println();
    }

    /** error for tracked files - used in branch & checkout branch.
     * @param  branch (branch name)
     * @return boolean */
    private boolean allTrackedCurrent(String branch) {
        File header = headCommit.listFiles()[0];
        Commit currCommit = Utils.readObject(header, Commit.class);
        File branchCommitPath = Utils.join(branchCol, branch);
        Commit branchCommit = Utils.readObject(branchCommitPath, Commit.class);
        File userDir = new File(System.getProperty("user.dir"));
        for (String thisFileName: branchCommit.getAllFileNames()) {
            if (!currCommit.getAllFileNames().contains(thisFileName)) {
                File toCheck = new File(userDir + "/" + thisFileName);
                if (toCheck.exists()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** error for tracked files - used in branch & checkout branch.
     * @param commitID (string)
     * @return boolean */
    private boolean allTrackedCurrentID(String commitID) {
        File header = headCommit.listFiles()[0];
        Commit currCommit = Utils.readObject(header, Commit.class);
        File resetCommitPath = Utils.join(commitCol, commitID);
        Commit resetCommit = Utils.readObject(resetCommitPath, Commit.class);
        File userDir = new File(System.getProperty("user.dir"));
        for (String thisFileName: resetCommit.getAllFileNames()) {
            if (!currCommit.getAllFileNames().contains(thisFileName)) {
                File toCheck = new File(userDir + "/" + thisFileName);
                if (toCheck.exists()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** helper.
     *
     * @param i (id)
     * @return i
     */
    private String helperID(String i) {
        int size = i.length();
        if (i.length() < UPPER_LIMIT) {
            for (File file: commitCol.listFiles()) {
                if (file.getName().substring(0, size).equals(i)) {
                    return file.getName();
                }
            }
        }
        return i;
    }

    /** modification. **/
    private void modificationEC() {
        File header = headCommit.listFiles()[0];
        Commit curr = Utils.readObject(header, Commit.class);
        File workingDir = new File(System.getProperty("user.dir"));
        ArrayList<String> temp = new ArrayList<>();
        for (File file: workingDir.listFiles()) {
            if (curr.getAllFileNames().contains(file.getName())
                    && !curr.getFileBlob().get(file.getName())
                    .equals(new Blob(file).getUniqueId())
                    && !Utils.join(stageToAdd, file.getName()).exists()) {
                temp.add(file.getName() + "(modified)");
            } else if (Utils.join(stageToAdd, file.getName()).exists()
                    && !new Blob(Utils.join(stageToAdd, file.getName()))
                    .getUniqueId().equals(new Blob(file).getUniqueId())) {
                temp.add(file.getName() + "(modified)");
            }
        }
        for (File file: stageToAdd.listFiles()) {
            if (!Utils.join(workingDir, file.getName()).exists()) {
                temp.add(file.getName() + "(deleted)");
            }
        }
        for (String fileName: curr.getAllFileNames()) {
            if (!Utils.join(stageToDelete, fileName).exists()
                    && !Utils.join(workingDir, fileName).exists()) {
                temp.add(fileName + "(deleted)");
            }
        }
        ArrayList<String> seen = new ArrayList<>();
        for (String name: temp) {
            if (!seen.contains(name)) {
                seen.add(name);
                System.out.println(name);
            }
        }
    }

    /** untracked. **/
    private void untrackedEC() {
        File header = headCommit.listFiles()[0];
        Commit curr = Utils.readObject(header, Commit.class);
        File workingDir = new File(System.getProperty("user.dir"));
        ArrayList<String> temp = new ArrayList<>();
        for (File file: workingDir.listFiles()) {
            if (!file.isDirectory()) {
                if (!Utils.join(stageToAdd, file.getName()).exists()
                        && !curr.getAllFileNames().contains(file.getName())) {
                    temp.add(file.getName());
                }
                if (Utils.join(stageToDelete, file.getName()).exists()) {
                    temp.add(file.getName());
                }
            }
        }
        ArrayList<String> seen = new ArrayList<>();
        for (String name: temp) {
            if (!seen.contains(name)) {
                seen.add(name);
                System.out.println(name);
            }
        }
    }

    boolean conflictFinder(Commit headBranch, Commit mergeBranch, Commit split) {
        boolean checkConflict = false;
        for (String fileNameC: headBranch.getAllFileNames()) {
            if (!headBranch.getFileBlob().get(fileNameC).equals(mergeBranch.getFileBlob().get(fileNameC))
                    && mergeBranch.getFileBlob() != null && split.getFileBlob() != null
                    && mergeBranch.getAllFileNames().contains(fileNameC) && headBranch.getAllFileNames().contains(fileNameC)
                    && !headBranch.getFileBlob().get(fileNameC).equals(split.getFileBlob().get(fileNameC))
                    && !mergeBranch.getFileBlob().get(fileNameC).equals(split.getFileBlob().get(fileNameC))) {
                checkConflict = true;
                String file1 = new String(Utils.readObject(Utils.join(blobCol, headBranch.getFileBlob().get(fileNameC)), Blob.class).getByteArr(), StandardCharsets.UTF_8);
                String file2 = new String(Utils.readObject(Utils.join(blobCol, mergeBranch.getFileBlob().get(fileNameC)), Blob.class).getByteArr(), StandardCharsets.UTF_8);
                String toWrite = "<<<<<<< HEAD\n" + file1 + "=======\n" +  file2 + ">>>>>>>\n";
                File thisFile = new File("" + fileNameC);
                thisFile.delete();
                Utils.writeContents(thisFile, toWrite);
                add(thisFile);
            } else if (mergeBranch.getFileBlob() != null && split.getFileBlob() != null && mergeBranch.getAllFileNames().contains(fileNameC)
                    && !headBranch.getFileBlob().get(fileNameC).equals(mergeBranch.getFileBlob().get(fileNameC))
                    && !split.getAllFileNames().contains(fileNameC)) {
                checkConflict = true;
                String file1 = new String(Utils.readObject(Utils.join(blobCol, headBranch.getFileBlob().get(fileNameC)), Blob.class).getByteArr(), StandardCharsets.UTF_8);
                String file2 = new String(Utils.readObject(Utils.join(blobCol, mergeBranch.getFileBlob().get(fileNameC)), Blob.class).getByteArr(), StandardCharsets.UTF_8);
                String toWrite = "<<<<<<< HEAD\n" + file1 + "=======\n" +  file2 + ">>>>>>>\n";
                File thisFile = new File("" + fileNameC);
                thisFile.delete();
                Utils.writeContents(thisFile, toWrite);
                add(thisFile);
            } else if (mergeBranch.getFileBlob() != null && split.getFileBlob() != null
                    && !mergeBranch.getAllFileNames().contains(fileNameC) && split.getFileBlob() != null
                    && !headBranch.getFileBlob().get(fileNameC).equals(split.getFileBlob().get(fileNameC))) {
                checkConflict = true;
                String file1 = new String(Utils.readObject(Utils.join(blobCol, headBranch.getFileBlob().get(fileNameC)), Blob.class).getByteArr(), StandardCharsets.UTF_8);
                String toWrite = "<<<<<<< HEAD\n" + file1 + "=======\n" + ">>>>>>>\n";
                File thisFile = new File("" + fileNameC);
                thisFile.delete();
                Utils.writeContents(thisFile, toWrite);
                add(thisFile);
            }
        }
        for (String fileNameM: mergeBranch.getAllFileNames()) {
            if (headBranch.getFileBlob() != null && split.getFileBlob() != null
                    && !headBranch.getAllFileNames().contains(fileNameM)
                    && !mergeBranch.getFileBlob().get(fileNameM).equals(split.getFileBlob().get(fileNameM))) {
                checkConflict = true;
                String file2 = new String(Utils.readObject(Utils.join(blobCol, mergeBranch.getFileBlob().get(fileNameM)), Blob.class).getByteArr(), StandardCharsets.UTF_8);
                String toWrite = "<<<<<<< HEAD\n" + "=======\n" +  file2 + ">>>>>>>\n";
                File thisFile = new File("" + fileNameM);
                thisFile.delete();
                Utils.writeContents(thisFile, toWrite);
                add(thisFile);
            }
        }
        return checkConflict;
    }
}
