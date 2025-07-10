package se.gu.git;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.repodriller.domain.*;
import org.repodriller.domain.Commit;
import org.repodriller.persistence.PersistenceMechanism;
import org.repodriller.scm.CommitVisitor;
import org.repodriller.scm.SCMRepository;
import se.gu.data.DataController;
import se.gu.main.ProjectData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommitPolicy implements CommitVisitor {
    private ProjectData projectData;
    private int count = 0;
    private DataController dataController;

    public CommitPolicy(ProjectData projectData) throws SQLException, ClassNotFoundException {
        this.projectData = projectData;
        dataController = new DataController(projectData.getConfiguration());
    }

    @Override
    public void process(SCMRepository repo, Commit commit, PersistenceMechanism writer) {
        count++;
        List<String> allowedExtensions = Arrays.asList(projectData.getConfiguration().getAllowedFileExtensions().split(",")).parallelStream().map(e -> e.replace(".", "")).collect(Collectors.toList());

        List<Modification> modifications = commit.getModifications()
                .parallelStream().filter(m -> allowedExtensions.contains(FilenameUtils.getExtension(m.getFileName()))).collect(Collectors.toList());

        double tLOCA, tLOCR, aLOCA, aLOCR, mLOCA, mLOCR, tFC, tFA, tFM, tFD, tFRe, tChurn, aChurn, tHunk, aHunk, mHunkSize, aHunkSize;
        String project = projectData.getConfiguration().getProjectRepository().getName();
        String commitHash = commit.getHash();
        ;

        List<Double> linesAdded = new ArrayList<>();
        List<Double> linesRemoved = new ArrayList<>();
        List<Double> churns = new ArrayList<>();
        List<Double> hunks = new ArrayList<>();
        List<Double> hunkSizes = new ArrayList<>();

        for (Modification modification : modifications) {
            linesAdded.add((double) modification.getAdded());
            linesRemoved.add((double) modification.getRemoved());
            churns.add((double) modification.getAdded() - modification.getRemoved());

            if (modification.getType() == ModificationType.RENAME || modification.getType() == ModificationType.DELETE) {
                hunks.add(0.0);
                hunkSizes.add(0.0);
            } else {
                String diff = modification.getDiff();
                if (diff.equalsIgnoreCase("-- TOO BIG --")) {
                    hunks.add(1.0);
                    hunkSizes.add((double) modification.getAdded());

                } else {
                    setDiffBlocks(hunks, hunkSizes, modification, diff);
                }
            }
        }
        //now calculate metrics
        tLOCA = linesAdded.stream().mapToDouble(Double::doubleValue).sum();
        tLOCR = linesRemoved.stream().mapToDouble(Double::doubleValue).sum();
        aLOCA = linesAdded.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        aLOCR = linesRemoved.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        mLOCA = linesAdded.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        mLOCR = linesRemoved.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        tFC = modifications.size();
        tFA = modifications.stream().filter(m -> m.getType() == ModificationType.ADD).count();
        tFM = modifications.stream().filter(m -> m.getType() == ModificationType.MODIFY).count();
        tFD = modifications.stream().filter(m -> m.getType() == ModificationType.DELETE).count();
        tFRe = modifications.stream().filter(m -> m.getType() == ModificationType.RENAME).count();
        tChurn = churns.stream().mapToDouble(Double::doubleValue).sum();
        aChurn = churns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        tHunk = hunks.stream().mapToDouble(Double::doubleValue).sum();
        aHunk = hunks.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        mHunkSize = hunkSizes.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        aHunkSize = hunkSizes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        try {
            writer.write(

                    project, commitHash, tLOCA, tLOCR, aLOCA, aLOCR, mLOCA, mLOCR, tFC, tFA, tFM, tFD, tFRe, tChurn, aChurn, tHunk, aHunk, mHunkSize, aHunkSize
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            dataController.commitMetricsInsertFromCode(tLOCA, tLOCR, aLOCA, aLOCR, mLOCA, mLOCR, tFC, tFA, tFM, tFD, tFRe, tHunk, aHunk, mHunkSize, aHunkSize, tChurn, aChurn, project, commitHash);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    private void setDiffBlocks(List<Double> hunks, List<Double> hunkSizes, Modification modification, String diff) {
        DiffParser diffParser;
        diffParser = new DiffParser(diff);

        List<DiffBlock> blocks = diffParser.getBlocks();
        hunks.add((double) blocks.size());
        for (DiffBlock block : blocks) {
            if (StringUtils.isBlank(block.getDiffBlock())) {
                hunkSizes.add(0.0);
            } else {
                hunkSizes.add((double) block.getLines().length);
            }
        }


    }
}
