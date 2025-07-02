package se.gu.git;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.main.Configuration;
import se.gu.utils.CommandRunner;
import sun.plugin.com.event.COMEventHandler;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiffExtractor implements Serializable {
    private static final long serialVersionUID = -3067358858528245680L;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    private Configuration configuration;


    public List<DiffEntry> getDiffEntries() {
        return diffEntries;
    }

    public void setDiffEntries(List<DiffEntry> diffEntries) {
        this.diffEntries = diffEntries;
    }

    private boolean isAllowed(DiffEntry entry, List<String> annotationSpecialFileNames) {
        if (entry == null) {
            return true;
        } else {
            if (entry.getDeletedFullyQualifiedName() == null && entry.getAddedFullyQualifiedName() == null) {
                return true;
            } else if (extensionAllowed(entry.getDeletedFullyQualifiedName()) || extensionAllowed(entry.getAddedFullyQualifiedName()) ||
                    annotationSpecialFileNames.contains(entry.getAddedFileName()) || annotationSpecialFileNames.contains(entry.getDeletedFileName())) {
                return true;
            } else {
                return false;
            }
        }
    }

    public void setDiffEntries(BufferedReader bufferedReader) throws IOException {
        List<String> annotationSpecialFileNames = configuration.getAnnotationFileNames();
        List<String> featureModelFileNames = Arrays.asList(configuration.getFeatureModelFile().split(","));
        diffEntries = new ArrayList<>();
        DiffEntry diffEntry = null;
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            //System.out.println(line);
            if (lineAccepted(line)) {
                if (!isAllowed(diffEntry, annotationSpecialFileNames)) {
                    continue;
                    //check if we dont hve to wste efforts adding lines for files not accepted
                }
                if (line.startsWith("---")) {
                    diffEntry = new DiffEntry();
                    diffEntry.setDeletedFullyQualifiedName(getChangedFileNameFull(line));
                    diffEntry.setDeletedFileName(getChangedFileName(line));
                } else if (line.startsWith("+++")) {
                    diffEntry.setAddedFullyQualifiedName(getChangedFileNameFull(line));
                    diffEntry.setAddedFileName(getChangedFileName(line));
//                    File test = new File(diffEntry.getAddedFullyQualifiedName());
//                    System.out.printf("Absolute path: %s\nRelative Path: %s\nCanonical Path: %s\n",test.getAbsolutePath(),test.getPath(),test.getCanonicalPath());
                } else if (line.startsWith("+") && !StringUtils.isBlank(diffEntry.getAddedFileName()) && featureModelFileNames.contains(diffEntry.getAddedFileName())) {
                    diffEntry.getAddedLines().add(line);
                } else if (line.startsWith("-") && !StringUtils.isBlank(diffEntry.getDeletedFileName()) && featureModelFileNames.contains(diffEntry.getDeletedFileName())) {
                    diffEntry.getDeletedLines().add(line);
                } else if (line.startsWith("@@")) {
                    if (!(extensionAllowed(diffEntry.getAddedFullyQualifiedName()) || extensionAllowed(diffEntry.getDeletedFullyQualifiedName()) ||
                            annotationSpecialFileNames.contains(diffEntry.getAddedFileName()) || annotationSpecialFileNames.contains(diffEntry.getDeletedFileName()))) {
                        continue;
                    }
                    CodeChange codeChange = getChangedLines(line);
                    diffEntry.getCodeChanges().add(codeChange);

                    if (!diffEntries.contains(diffEntry)) {
                        diffEntries.add(diffEntry);
                    }

                }

            }
        }

    }

    private List<DiffEntry> diffEntries;
    private File analysisDirectory;
    private List<Commit> commitHistory;

    public DiffExtractor(Configuration configuration) throws Exception {
        setConfiguration(configuration);
    }

    public List<Commit> getCommitHistory() {

        return commitHistory;
    }
    public void setCommitHistory(List<Commit> commitList){
        commitHistory = commitList;
    }
    public List<String> getCSVCommitLines() throws IOException {
        List<File> projectRepositories = configuration.getCopiedGitRepositories();
        int indexOfCurrentProject = projectRepositories.indexOf(configuration.getProjectRepository());
        List<File> commitCSVs = configuration.isMetricCalculationCommitBased()?configuration.getCommitCSVFilesCommitBasedPrediction(): configuration.getCommitCSVs();
        File projectCommitCSV = commitCSVs.get(indexOfCurrentProject);
        List<String> lines = new ArrayList<>();
        if(projectCommitCSV.exists()) {
           lines  =FileUtils.readLines(projectCommitCSV, configuration.getTextEncoding());
        }
        return  lines;
    }
    public void setCommitHistory() throws IOException, ParseException, InterruptedException, ExecutionException {
        //&begin [Metrics]
        if(configuration.isUseMetricCommitsFromCSV()){
            commitHistory = new ArrayList<>();
            List<String> lines = getCSVCommitLines();

                if(configuration.isMetricCalculationCommitBased()){
                    //if commit based we read from the file with coluns commitId;commitHash;releaseId;release
                    for(String line:lines){
                        String[]items = line.split(";");
                        commitHistory.add(new Commit(Integer.parseInt(items[0].trim()),items[1],Integer.parseInt(items[2].trim()),items[3],configuration.getProjectRepository(), items.length==4?false:items[4].equalsIgnoreCase("skip")?true:false));
                    }
                    //commitHistory.addAll(lines.stream().map(l -> new Commit(Integer.parseInt(l.split(";")[0].trim()), l.split(";")[1].trim(),Integer.parseInt(l.split(";")[2].trim()), l.split(";")[3].trim(), configuration.getProjectRepository())).collect(Collectors.toList()));

                }else {
                    //from relesse based CSV, we read release;startingCommit;endCommit
                    ////split the line: [2]=endCommit,[1]startingCommit,[0]=relese tag
                    commitHistory.addAll(lines.stream().map(l -> new Commit(l.split(",")[2].trim(), l.split(",")[1].trim(), l.split(",")[0].trim(), configuration.getProjectRepository())).collect(Collectors.toList()));
                }


        }
        //&end [Metrics]
        else {

            CommandRunner commandRunner = new CommandRunner(configuration);
            commitHistory = getCommitsFromLog(commandRunner.getClaferSimulationCommitsFromLog());
            Collections.reverse(commitHistory);
        }
    }

    private void getRegularCommits(BufferedReader reader) throws IOException {
        String line;
        Commit commit;

        while ((line = reader.readLine()) != null) {
            commit = new Commit();
            commit.setCommitHash(line.trim());
            commitHistory.add(commit);
        }
        Collections.reverse(commitHistory);
        //System.out.println(commitHistory);
    }

    public String getDiffOutput(BufferedReader bufferedReader) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (lineAccepted(line)) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
        }
        return stringBuilder.toString();
    }

    public boolean lineAccepted(String line) {
        return line.startsWith("---") || line.startsWith("+++") || line.startsWith("@@") || line.startsWith("-") || line.startsWith("+");
    }

    public List<String> getFileTextAsList(File fileToRead) throws IOException {
        List<String> lines = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToRead)));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }


    private String getChangedFileNameFull(String filePath) {
        if (filePath.contains("null")) {
            return null;
        } else {
            String files[] = filePath.split("(--- a)|" + Pattern.quote("+++ b"));
            if (files.length == 0) {
                return null;
            } else {
                return configuration.getProjectRepository().getPath() + (files.length == 1 ? files[0] : files[1]);
            }

        }

    }

    private String getChangedFileName(String filePath) {
        if (filePath.contains("null")) {
            return null;
        } else {
            String[] pathChars = filePath.split("/");
            return pathChars[pathChars.length - 1];
        }

    }

    private CodeChange getChangedLines(String lineDetails) {

        CodeChange codeChange = new CodeChange();

        String line =  lineDetails.substring(lineDetails.indexOf("@@")+2,lineDetails.lastIndexOf("@@"));// lineDetails.replaceAll("@@", "").replaceAll("-", "");//.split(("(\\+)|,"));
        String[] all = line.split("\\+");
        String added[] = all[1].split(",");
        String deleted[] = all[0].split(",");
        codeChange.setDeletedLinesStart(Integer.parseInt(deleted[0].replaceAll("-", "").trim()));
        codeChange.setAddedLinesStart(Integer.parseInt(added[0].trim()));
        codeChange.setDeletedLines(deleted.length == 1 || StringUtils.isBlank(deleted[1]) ? 0 : Integer.parseInt(deleted[1].trim()));
        codeChange.setAddedLines(added.length == 1 || StringUtils.isBlank(added[1]) ? 0 : Integer.parseInt(added[1].split("\\s")[0].trim()));


        return codeChange;
    }

    private List<Commit> getCommitsFromLog(BufferedReader bufferedReader) throws IOException, ParseException {
        String line;
        Commit commit;
        String author;
        List<Commit> commitList = new ArrayList<>();

        String commitDate;
        List<String> lines = new ArrayList<>();
        List<String> simulationCommitAuthors = configuration.getSimulationCommitAuthorsList();
        while ((line = bufferedReader.readLine()) != null) {
            if(StringUtils.isBlank(line.replaceAll("\\W","").replaceAll(Pattern.quote("_"),"").trim())){
                continue;
            }
            String[] commitFacets = line.split(Pattern.quote("_"));
            if (commitFacets.length < 4) {
                continue;
            }

            author = commitFacets[3].trim();
            if (configuration.getProjectType() == ProjectType.REGULAR) {

                addCommitToList(author, commitList, commitFacets);

            } else {
                if (simulationCommitAuthors.contains(author)) {
                    addCommitToList(author, commitList, commitFacets);
                }
            }


        }

        return commitList;
    }

    private void addCommitToList(String author, List<Commit> commitList, String[] commitFacets) throws ParseException {
        Commit commit;
        commit = new Commit();
        commit.setAuthor(author);
        commit.setCommitHash(commitFacets[1].trim());
        commit.setCommitDate(commitFacets[2].trim());
        commit.setMessage(commitFacets.length>=5?commitFacets[4].trim():null);
        commit.setProject(configuration.getProjectRepository());
        boolean isMerge = commit.getMessage()==null?false: Pattern.compile(Pattern.quote("merge"), Pattern.CASE_INSENSITIVE).matcher(commit.getMessage()).find();
        commit.setMergeCommit(isMerge);

        commitList.add(commit);
    }

    private boolean extensionAllowed(String fileName) {
        File file = StringUtils.isBlank(fileName) ? null : new File(fileName);
        if (file == null || !file.getName().contains(".")) {
            return false;//return false for all files without extensions
        }
        String extension = file.getName().substring(file.getName().lastIndexOf("."));
        List<String> allowedFileExtensions = Arrays.asList(configuration.getAllowedFileExtensions().split(","));
        return file.exists() && allowedFileExtensions.contains(extension);

    }

    public List<ProjectClone> getClones() {
        return new ArrayList<>(Arrays.asList(new ProjectClone[]{
                new ProjectClone("2e9c173a3c4cd3153aaf4798c5b433cb85232129", "6e6bbcad3d2c374d6b07749cecb5740b9c329e5f", ClaferProject.Visualiser, ClaferProject.Configurator),
                new ProjectClone("2e9c173a3c4cd3153aaf4798c5b433cb85232129", "e9ec5b47c3d417981f4b46f861a85525c2de3e42", ClaferProject.Visualiser, ClaferProject.Configurator),
                new ProjectClone("2e9c173a3c4cd3153aaf4798c5b433cb85232129", "137e03feabfd9a3dba768d2e5d6d4df1b8a59478", ClaferProject.Visualiser, ClaferProject.Configurator),
                new ProjectClone("2e9c173a3c4cd3153aaf4798c5b433cb85232129", "452234143f8a0e450634432d6bc51d1a944b51b5", ClaferProject.Visualiser, ClaferProject.Configurator),
                new ProjectClone("acf7e05b4da2b620ae5adfc2f83bd3fc887f49a6", "6c347bbc82d9e708f0a7d7348da5fa149b8a0100", ClaferProject.Visualiser, ClaferProject.Configurator),
                new ProjectClone("5a81091401d8f392e7d2e09748af081135b59852", "6884dd91207106edb4c0d96e88dda9ffaccacd97", ClaferProject.Configurator, ClaferProject.Visualiser),
                new ProjectClone("1dd4f428067b67c344c585bce8ada70277bee208", "7a7c87aedb9974d4b1f84ca4ab80dba640d0e0d4", ClaferProject.Visualiser, ClaferProject.IDE),
                new ProjectClone("5a81091401d8f392e7d2e09748af081135b59853", "115229b528e5da53cb7dc982acd579ea518ac5c8", ClaferProject.Configurator, ClaferProject.Visualiser),
                new ProjectClone("9743ef139b7225a371f3bd6fb2a01eafb66fbd1b", "0b966d29fdef50343b06a40b35684cbd31675826", ClaferProject.IDE, ClaferProject.Visualiser),
                new ProjectClone("35a2e788ba2020b545bc456b3eeb002997bd445c", "c9c8c1db2b05ac29475b5ed94c4aeae36eb82cdb", ClaferProject.IDE, ClaferProject.Visualiser),
                new ProjectClone("6c347bbc82d9e708f0a7d7348da5fa149b8a0100", "ab8d7e7e99dce24a5499641314192b64a62bd045", ClaferProject.Configurator, ClaferProject.IDE),
                new ProjectClone("40b6557be8e6d315dc73e149376910a949f7289f", "ce58163c35fb42b1fa24a89a4534fd427285be9c", ClaferProject.IDE, ClaferProject.Platform),
                new ProjectClone("cd8a1bb2ac6e733d358887ba36bcae34319137c0", "6c8d8cdbae50e6d01aafd55ba085945308284b5e", ClaferProject.Visualiser, ClaferProject.Platform),
                new ProjectClone("ab756334a2425da94f9001cdb29aac1194352ab1", "512d4e3914ef6fcd1274bfcc633e330c015b26bc", ClaferProject.Platform, ClaferProject.Visualiser),
                new ProjectClone("b7a7f76a5cc38fa6030ffabbb0ddeeb15c80f8e2", "7040b48b20c7be975e08d9e7d3151c59496a80e0", ClaferProject.Platform, ClaferProject.Visualiser),
                new ProjectClone("72078c71dc74d51666f3774f29f1355ed3c722e8", "c45dad5d500070223da30a2c01951afa4bd8c14b", ClaferProject.Platform, ClaferProject.Visualiser),
                new ProjectClone("6c347bbc82d9e708f0a7d7348da5fa149b8a0100", "93ed28c22e1489c938896d31e8a74124d2660605", ClaferProject.Configurator, ClaferProject.Platform),
                new ProjectClone("6c347bbc82d9e708f0a7d7348da5fa149b8a0100", "15130f3661df872e07d33a8a4b0b5fec3c6a722a", ClaferProject.Configurator, ClaferProject.Platform),
                new ProjectClone("1bd0ec489d4e57b94b2cbfa0e4324fc4e634c061", "e5a36b08180e5a8845e7dec7c032dd6ed0a3a793", ClaferProject.Visualiser, ClaferProject.Configurator),
                new ProjectClone("e25483850a7dd9e669737aefe8484fed54c82d5e", "cc9f8304c58fb76b29bc3f58ae858f17e0c679d4", ClaferProject.IDE, ClaferProject.Configurator),
                new ProjectClone("3ccc7bb168aa1063dbdaec231b4eadadbd2f3a86", "a10bb4ad8dcfbc75e14c2e34031d975dc7565342", ClaferProject.Configurator, ClaferProject.Platform),
                new ProjectClone("1598b39dd859782cdb6b756dfcc36a1cc319bb8c", "a10bb4ad8dcfbc75e14c2e34031d975dc7565342", ClaferProject.IDE, ClaferProject.Platform),
        }));
    }

    public void reorderCommits() {
        List<ProjectClone> clones = getClones();
        for (ProjectClone clone : clones) {
            Commit sourceCommit = commitHistory.stream().filter(c -> c.getCommitHash().equalsIgnoreCase(clone.getSourceCommit())).findFirst().orElse(null);
            Commit targetCommit = commitHistory.stream().filter(c -> c.getCommitHash().equalsIgnoreCase(clone.getTargetCommit())).findFirst().orElse(null);
            if (sourceCommit != null && targetCommit != null) {
                int sourceIndex = commitHistory.indexOf(sourceCommit);
                int targetIndex = commitHistory.indexOf(targetCommit);
                if (sourceIndex > targetIndex) {
                    commitHistory.remove(targetCommit);
                    commitHistory.add(sourceIndex, targetCommit);
                }
            }

        }
    }

}
