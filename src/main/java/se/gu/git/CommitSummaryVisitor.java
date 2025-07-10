package se.gu.git;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.repodriller.domain.*;
import org.repodriller.domain.Commit;
import org.repodriller.persistence.PersistenceMechanism;
import org.repodriller.scm.CommitVisitor;
import org.repodriller.scm.SCMRepository;
import se.gu.main.ProjectData;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class CommitSummaryVisitor implements CommitVisitor {
    public CommitSummaryVisitor(ProjectData projectData) {
        this.projectData = projectData;
    }

    private ProjectData projectData;
    private int count = 0;
    private Calendar previousTime;

    @Override
    public void process(SCMRepository repo, Commit commit, PersistenceMechanism writer) {
        count++;
        List<String> allowedExtensions = Arrays.asList(projectData.getConfiguration().getAllowedFileExtensions().split(",")).parallelStream().map(e -> e.replace(".", "")).collect(Collectors.toList());

        List<Modification> modifications = commit.getModifications()
                .parallelStream().filter(m -> allowedExtensions.contains(FilenameUtils.getExtension(m.getFileName()))).collect(Collectors.toList());
        int filesAffected = modifications.size();
        int diffBlocks = 0;
        int totalAdded = 0;
        int totalRemoved = 0;
        List<Integer> linesAddedInBlocks = new ArrayList<>();
        List<Integer> linesDeletedInBlocks = new ArrayList<>();
        List<Integer> diffBlockSizes = new ArrayList<>();
        List<Integer> linesAdded = new ArrayList<>();
        List<Integer> linesRemoved = new ArrayList<>();

        for (Modification modification : modifications) {

            if (modification.getFileName().contains(projectData.getConfiguration().getFileMappingFile()) || modification.getFileName().contains(projectData.getConfiguration().getFolderMappingFile())) {
                continue;
            }
            if (modification.getType() == ModificationType.RENAME || modification.getType() == ModificationType.DELETE) {

                continue;
            }
            String diff = modification.getDiff();
            if (diff.equalsIgnoreCase("-- TOO BIG --")) {
                continue;//skip diffs too big to process

            }
            int added = modification.getAdded();
            int removed = modification.getRemoved();
            totalAdded += added;
            totalRemoved += removed;
            if (added > 0) {
                linesAdded.add(added);
            }
            if (removed > 0) {
                linesRemoved.add(removed);
            }
            List<DiffBlock> blocks = getDiffBlocks(diff);
            int size = blocks == null ? 0 : blocks.size();
            diffBlocks += size;
            if (size > 0) {
                diffBlockSizes.add(size);
            }
            for (DiffBlock block : blocks) {
                if (StringUtils.isBlank(block.getDiffBlock())) {
                    continue;
                }
                linesAddedInBlocks.add(block.getLinesInNewFile().size());
                linesDeletedInBlocks.add(block.getLinesInOldFile().size());
            }


        }
        double averageNewLinesInBlock = linesAddedInBlocks.parallelStream().mapToDouble(Integer::doubleValue).average().orElse(0);
        double averageDeletedLinesInBlock = linesDeletedInBlocks.parallelStream().mapToDouble(Integer::doubleValue).average().orElse(0);
        double averageNumberOfDiffBlocks = diffBlockSizes.parallelStream().mapToDouble(Integer::doubleValue).average().orElse(0);
        double averageLinesAdded = linesAdded.parallelStream().mapToDouble(Integer::doubleValue).average().orElse(0);
        double averageLinesRemoved = linesRemoved.parallelStream().mapToDouble(Integer::doubleValue).average().orElse(0);

        long days = previousTime==null?0: Duration.between(previousTime.toInstant(),commit.getDate().toInstant()).toDays();
        long hours = previousTime==null?0: Duration.between(previousTime.toInstant(),commit.getDate().toInstant()).toHours();
        long minutes = previousTime==null?0: Duration.between(previousTime.toInstant(),commit.getDate().toInstant()).toMinutes();
        writer.write(count
                , projectData.getConfiguration().getProjectRepository().getName()
                , commit.getHash()
                , filesAffected
                , totalAdded
                , totalRemoved
                , averageLinesAdded
                , averageLinesRemoved
                , diffBlocks
                , averageNumberOfDiffBlocks
                , averageNewLinesInBlock
                , averageDeletedLinesInBlock
                , commit.getAuthor().getName()
                ,days
                ,hours
                ,minutes
        );
        previousTime = commit.getDate();

    }

    public List<DiffBlock> getDiffBlocks(String diff) {

        DiffParser diffParser = new DiffParser(diff);
        List<DiffBlock> blocks = diffParser.getBlocks();
        return blocks;
    }
}
