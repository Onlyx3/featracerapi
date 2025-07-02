package se.gu.metrics;

import me.tongfei.progressbar.ProgressBar;
import se.gu.assets.AssetDB;
import se.gu.assets.AssetMappingDB;
import se.gu.data.DataController;
import se.gu.git.Commit;
import se.gu.main.ProjectData;
import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.CosineSimilarity;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class MetricCalculatorDB {
    private ProjectData projectData;
    private DataController dataController;

    public MetricCalculatorDB(ProjectData projectData) {
        this.projectData = projectData;
    }


    public void calculateMetricsALLASSETSLOADED() throws SQLException, ClassNotFoundException {
        dataController = new DataController(projectData.getConfiguration());
        AbstractStringMetric metric = new CosineSimilarity();
        String projectName = projectData.getConfiguration().getProjectRepository().getName();
        int startingCommitIndex = projectData.getConfiguration().getStartingCommitIndex();
        int commitsToRun = projectData.getConfiguration().getCommitsToExecute();
        //get all commits
        List<Commit> commits = dataController.getAllCommits(projectName);
        commitsToRun = commitsToRun == 0 ? commits.size() : commitsToRun;
        int commitsRun = 0;
        //get all needed lists
        List<AssetDB> allAssetsForProject = dataController.getAssetsForProject(projectName);
        List<AssetMappingDB> assetMappingsForProject = dataController.getAssetMappingsForProject(projectName);
        List<AssetMetricsDB> allFeaturesPerCommitInProject = dataController.getFeatureModifiedPerCommitInProject(projectName);
        List<AssetMetricsDB> allCommitCCCsForProject = dataController.getCCCForProject(projectName);
        List<String> assetTypesAllowed = projectData.getConfiguration().getAssetTypesToPredict();
        try (ProgressBar pb = new ProgressBar("Commits:", commitsToRun)) {
            for (Commit commit : commits) {
                try {
                    if (commit.getCommitIndex() < startingCommitIndex) {
                        pb.step();
                        continue;
                    }
                    if (commitsRun > commitsToRun) {
                        break;
                    }
                    //delete existing metrics for commit
                    try {
                        dataController.deleteMetricsForCommit(commit.getCommitHash(), projectName);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    System.out.printf("%s, commidIndex: %d\n",projectName,commit.getCommitIndex());
                    //first time, load all assets up to this commit
//                if (!gotAllAssets) {
//                    allassetsAtCommit.addAll(dataController.getAllAssetsUptoCommit(commit.getCommitIndex(), projectName));
//                    gotAllAssets = true;
//                } else {
//                    allassetsAtCommit.addAll(assetsInCommit);
//                }
                    pb.step();
                    pb.setExtraMessage(commit.getCommitHash());
                    //List<AssetMetricsDB> parentNFFs = dataController.getParentNFF(commit.getCommitIndex(), projectName);
                    Map<String, Double> parentNFF = getParentNFF(assetMappingsForProject,commit.getCommitIndex());
//                    for (AssetMetricsDB p : parentNFFs) {
//                        parentNFF.put(p.getParent(), p.getNff());
//                    }
                    //Map<String, Integer> parentNLOC = dataController.getParentNLOC(commit.getCommitIndex(), projectName);
                    //get all assets that changed in commit
                    List<AssetDB> assetsInCommit = allAssetsForProject.parallelStream().filter(a->a.getCommitHash().equalsIgnoreCase(commit.getCommitHash())).collect(Collectors.toList());
                    List<AssetDB> allassetsAtCommit = allAssetsForProject.parallelStream().filter(a->a.getCommitIndex()<=commit.getCommitIndex()).collect(Collectors.toList());
                    List<AssetMappingDB> assetmappings = assetMappingsForProject.parallelStream().filter(a->a.getCommitIndex()<=commit.getCommitIndex()).collect(Collectors.toList());
                    List<AssetMetricsDB> commitCCCs = allCommitCCCsForProject.parallelStream().filter(c->c.getCommitIndex()<=commit.getCommitIndex()).collect(Collectors.toList());
                    List<AssetMetricsDB> devContributions = getDeveloperContribution(allAssetsForProject,commit.getCommitIndex()); //dataController.getDeveloperContribution(commit.getCommitIndex(), projectName);
                    List<AssetMetricsDB> featuresInCommit = allFeaturesPerCommitInProject.parallelStream().filter(f->f.getCommitIndex()<=commit.getCommitIndex()).collect(Collectors.toList());
                    try (ProgressBar cb = new ProgressBar("Asset Metrics:", assetsInCommit.size())) {
                        for (AssetDB asset : assetsInCommit) {
                            try {
                                cb.step();
                                cb.setExtraMessage(asset.getAssetName());
                                List<String> commitInWHichAssetChanged = allassetsAtCommit.parallelStream().filter(a -> a.getAssetFullName().equalsIgnoreCase(asset.getAssetFullName()))
                                        .map(AssetDB::getCommitHash).distinct().collect(Collectors.toList());
                                List<String> developersOfAsset = allassetsAtCommit.parallelStream().filter(a -> a.getAssetFullName().equalsIgnoreCase(asset.getAssetFullName()))
                                        .map(AssetDB::getDeveloper).distinct().collect(Collectors.toList());
                                AssetMetricsDB metrics = new AssetMetricsDB();
                                metrics.setAsset(asset.getAssetFullName());
                                metrics.setProject(projectName);
                                metrics.setCommitHash(commit.getCommitHash());
                                metrics.setCommitIndex(commit.getCommitIndex());
                                metrics.setParent(asset.getParent());
                                metrics.setAssetType(asset.getAssetType());
                                Optional<AssetMappingDB> assetMapping = assetmappings.parallelStream().filter(m -> m.getAssetfullname().equalsIgnoreCase(asset.getAssetFullName())).findAny();
                                metrics.setMapped(assetMapping.isPresent());

                                //number of features in parent file
                                double nff = 0;
                                if (asset.getAssetType().equalsIgnoreCase("FILE") || asset.getAssetType().equalsIgnoreCase("FOLDER")) {
                                    if (parentNFF.containsKey(asset.getAssetFullName())) {
                                        nff = parentNFF.get(asset.getAssetFullName());
                                    }
                                } else if (parentNFF.containsKey(asset.getParent())) {
                                    nff = parentNFF.get(asset.getParent());
                                }
                                //==NFF==
                                metrics.setNff(nff);
                                //assets modified together with asset
                                //==ACC==
                                metrics.setCcc(assetsInCommit.size());
                                //average assets modified with asset

                                //==ACC==
                                double accc = commitCCCs.parallelStream()
                                        .filter(c -> commitInWHichAssetChanged.contains(c.getCommitHash()))
                                        .map(AssetMetricsDB::getCcc).mapToDouble(Double::doubleValue).average().orElse(0);
                                metrics.setAccc(accc);
                                //==COMM==
                                metrics.setComm(commitInWHichAssetChanged.size());
                                //==CSDEV==
                                double csdev = csDev(developersOfAsset, metric);
                                metrics.setCsvDev(csdev);

                                //==DDEV==
                                metrics.setDdev(developersOfAsset.size());

                                //==DCONT==
                                List<Double> assetContributions = devContributions.parallelStream()
                                        .filter(d -> developersOfAsset.contains(d.getDeveloper()))
                                        .map(AssetMetricsDB::getDcont).collect(Collectors.toList());
                                double dcont = assetContributions.parallelStream().mapToDouble(Double::doubleValue).average().orElse(0);
                                metrics.setDcont(dcont);

                                //==HDCONT==
                                double hdcont = assetContributions.parallelStream().mapToDouble(Double::doubleValue).max().orElse(0);
                                metrics.setHdcont(hdcont);
                                //==NLOC==
                                metrics.setNloc(asset.getNloc());

                                //==DNFMA==
                                double dnfma = featuresInCommit.parallelStream().filter(f -> commitInWHichAssetChanged.contains(f.getCommitHash())).map(AssetMetricsDB::getNfma).mapToDouble(Double::doubleValue).average().orElse(0);
                                metrics.setDnfma(dnfma);
                                //==NFMA==
                                double nfma = featuresInCommit.parallelStream().filter(f -> f.getCommitHash().equalsIgnoreCase(commit.getCommitHash())).map(AssetMetricsDB::getNfma).mapToDouble(Double::doubleValue).sum();
                                metrics.setNfma(nfma);
                                dataController.assetMetricsInsert(metrics);

                            }catch (Exception ex){
                                ex.printStackTrace();
                            }
                        }
                    }
                    commitsRun++;
                    assetsInCommit = null;
                    allassetsAtCommit = null;
                    assetmappings = null;
                    commitCCCs = null;
                    devContributions = null;
                    featuresInCommit = null;
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }
    }

    private Map<String, Double> getParentNFF(List<AssetMappingDB> allAssetMappingsForProject,int currentCommitIndex){
        Map<String, Double> parentNFF = new HashMap<>();
        //get distinct parents upto commit
        List<String> parents = allAssetMappingsForProject.parallelStream().filter(m->m.getCommitIndex()<=currentCommitIndex).map(AssetMappingDB::getParent).distinct().collect(Collectors.toList());
        for(String parent:parents){
            double features = allAssetMappingsForProject.parallelStream().filter(m->m.getCommitIndex()<=currentCommitIndex && m.getParent().equalsIgnoreCase(parent)).map(AssetMappingDB::getFeaturename)
                    .distinct().count();
            parentNFF.put(parent,features);
        }
        return parentNFF;
    }
    private List<AssetMetricsDB> getDeveloperContribution(List<AssetDB> allAssetsInProject,int currentCommitIndex){
        List<AssetMetricsDB> records = new ArrayList<>();
        //get distinct developers upto commit
        List<String> developers = allAssetsInProject.parallelStream().filter(m->m.getCommitIndex()<=currentCommitIndex).map(AssetDB::getDeveloper).distinct().collect(Collectors.toList());
        for(String developer:developers){
            double assets = allAssetsInProject.parallelStream().filter(m->m.getCommitIndex()<=currentCommitIndex && m.getDeveloper().equalsIgnoreCase(developer)&&m.getAssetType().equalsIgnoreCase("LOC"))
                    .map(AssetDB::getAssetFullName)
                    .distinct().count();
            AssetMetricsDB record = new AssetMetricsDB();
            record.setDcont(assets);
            record.setDeveloper(developer);
            records.add(record);
        }
        return records;
    }
    public void calculateMetrics() throws SQLException, ClassNotFoundException {
        dataController = new DataController(projectData.getConfiguration());
        AbstractStringMetric metric = new CosineSimilarity();
        String projectName = projectData.getConfiguration().getProjectRepository().getName();
        int startingCommitIndex = projectData.getConfiguration().getStartingCommitIndex();
        int commitsToRun = projectData.getConfiguration().getCommitsToExecute();
        //get all commits
        List<Commit> commits = dataController.getAllCommits(projectName);
        commitsToRun = commitsToRun == 0 ? commits.size() : commitsToRun;
        int commitsRun = 0;
        boolean gotAllAssets = false;
        try (ProgressBar pb = new ProgressBar("Commits:", commitsToRun)) {
            for (Commit commit : commits) {
                try {
                    if (commit.getCommitIndex() < startingCommitIndex) {
                        pb.step();
                        continue;
                    }
                    if (commitsRun > commitsToRun) {
                        break;
                    }
                    //delete existing metrics for commit
                    try {
                        dataController.deleteMetricsForCommit(commit.getCommitHash(), projectName);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    System.out.printf("%s, commidIndex: %d\n",projectName,commit.getCommitIndex());
                    //first time, load all assets up to this commit
//                if (!gotAllAssets) {
//                    allassetsAtCommit.addAll(dataController.getAllAssetsUptoCommit(commit.getCommitIndex(), projectName));
//                    gotAllAssets = true;
//                } else {
//                    allassetsAtCommit.addAll(assetsInCommit);
//                }
                    pb.step();
                    pb.setExtraMessage(commit.getCommitHash());
                    List<AssetMetricsDB> parentNFFs = dataController.getParentNFF(commit.getCommitIndex(), projectName);
                    Map<String, Double> parentNFF = new HashMap<>();
                    for (AssetMetricsDB p : parentNFFs) {
                        parentNFF.put(p.getParent(), p.getNff());
                    }
                    //Map<String, Integer> parentNLOC = dataController.getParentNLOC(commit.getCommitIndex(), projectName);
                    //get all assets that changed in commit
                    List<AssetDB> assetsInCommit = dataController.getAssetsForCommit(commit.getCommitHash(), projectName);
                    List<AssetDB> allassetsAtCommit = dataController.getAllAssetsUptoCommit(commit.getCommitIndex(), projectName);
                    List<AssetMappingDB> assetmappings = dataController.getAssetMappingsForCommit(commit.getCommitIndex(), projectName);
                    List<AssetMetricsDB> commitCCCs = dataController.getCCCForCommit(commit.getCommitIndex(), projectName);
                    List<AssetMetricsDB> devContributions = dataController.getDeveloperContribution(commit.getCommitIndex(), projectName);
                    List<AssetMetricsDB> featuresInCommit = dataController.getFeatureModifiedInCommit(commit.getCommitIndex(), projectName);
                    try (ProgressBar cb = new ProgressBar("Asset Metrics:", assetsInCommit.size())) {
                        for (AssetDB asset : assetsInCommit) {
                            try {
                                cb.step();
                                cb.setExtraMessage(asset.getAssetName());
                                List<String> commitInWHichAssetChanged = allassetsAtCommit.parallelStream().filter(a -> a.getAssetFullName().equalsIgnoreCase(asset.getAssetFullName()))
                                        .map(AssetDB::getCommitHash).distinct().collect(Collectors.toList());
                                List<String> developersOfAsset = allassetsAtCommit.parallelStream().filter(a -> a.getAssetFullName().equalsIgnoreCase(asset.getAssetFullName()))
                                        .map(AssetDB::getDeveloper).distinct().collect(Collectors.toList());
                                AssetMetricsDB metrics = new AssetMetricsDB();
                                metrics.setAsset(asset.getAssetFullName());
                                metrics.setProject(projectName);
                                metrics.setCommitHash(commit.getCommitHash());
                                metrics.setCommitIndex(commit.getCommitIndex());
                                metrics.setParent(asset.getParent());
                                metrics.setAssetType(asset.getAssetType());
                                Optional<AssetMappingDB> assetMapping = assetmappings.parallelStream().filter(m -> m.getAssetfullname().equalsIgnoreCase(asset.getAssetFullName())).findAny();
                                metrics.setMapped(assetMapping.isPresent());

                                //number of features in parent file
                                double nff = 0;
                                if (asset.getAssetType().equalsIgnoreCase("FILE") || asset.getAssetType().equalsIgnoreCase("FOLDER")) {
                                    if (parentNFF.containsKey(asset.getAssetFullName())) {
                                        nff = parentNFF.get(asset.getAssetFullName());
                                    }
                                } else if (parentNFF.containsKey(asset.getParent())) {
                                    nff = parentNFF.get(asset.getParent());
                                }
                                //==NFF==
                                metrics.setNff(nff);
                                //assets modified together with asset
                                //==ACC==
                                metrics.setCcc(assetsInCommit.size());
                                //average assets modified with asset

                                //==ACC==
                                double accc = commitCCCs.parallelStream()
                                        .filter(c -> commitInWHichAssetChanged.contains(c.getCommitHash()))
                                        .map(AssetMetricsDB::getCcc).mapToDouble(Double::doubleValue).average().orElse(0);
                                metrics.setAccc(accc);
                                //==COMM==
                                metrics.setComm(commitInWHichAssetChanged.size());
                                //==CSDEV==
                                double csdev = csDev(developersOfAsset, metric);
                                metrics.setCsvDev(csdev);

                                //==DDEV==
                                metrics.setDdev(developersOfAsset.size());

                                //==DCONT==
                                List<Double> assetContributions = devContributions.parallelStream()
                                        .filter(d -> developersOfAsset.contains(d.getDeveloper()))
                                        .map(AssetMetricsDB::getDcont).collect(Collectors.toList());
                                double dcont = assetContributions.parallelStream().mapToDouble(Double::doubleValue).average().orElse(0);
                                metrics.setDcont(dcont);

                                //==HDCONT==
                                double hdcont = assetContributions.parallelStream().mapToDouble(Double::doubleValue).max().orElse(0);
                                metrics.setHdcont(hdcont);
                                //==NLOC==
                                metrics.setNloc(asset.getNloc());

                                //==DNFMA==
                                double dnfma = featuresInCommit.parallelStream().filter(f -> commitInWHichAssetChanged.contains(f.getCommitHash())).map(AssetMetricsDB::getNfma).mapToDouble(Double::doubleValue).average().orElse(0);
                                metrics.setDnfma(dnfma);
                                //==NFMA==
                                double nfma = featuresInCommit.parallelStream().filter(f -> f.getCommitHash().equalsIgnoreCase(commit.getCommitHash())).map(AssetMetricsDB::getNfma).mapToDouble(Double::doubleValue).sum();
                                metrics.setNfma(nfma);
                                dataController.assetMetricsInsert(metrics);

                            }catch (Exception ex){
                                ex.printStackTrace();
                            }
                        }
                    }
                    commitsRun++;
                    assetsInCommit = null;
                    allassetsAtCommit = null;
                    assetmappings = null;
                    commitCCCs = null;
                    devContributions = null;
                    featuresInCommit = null;
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }
    }

    public void calculateAssetBasedCommitMetrics() throws SQLException, ClassNotFoundException {
        dataController = new DataController(projectData.getConfiguration());

        String projectName = projectData.getConfiguration().getProjectRepository().getName();
        int startingCommitIndex = projectData.getConfiguration().getStartingCommitIndex();
        int commitsToRun = projectData.getConfiguration().getCommitsToExecute();
        //get all commits
        List<Commit> commits = dataController.getAllCommits(projectName);
        List<AssetDB> allAssetsForProject = dataController.getAssetsForProject(projectName);
        List<AssetMappingDB> assetMappingsForProject = dataController.getAssetMappingsForProject(projectName);
        commitsToRun = commitsToRun == 0 ? commits.size() : commitsToRun;
        int commitsRun = 0;

        try (ProgressBar pb = new ProgressBar("Commits:", commitsToRun)) {
            for (Commit commit : commits) {
                if (commit.getCommitIndex() < startingCommitIndex) {
                    pb.step();
                    continue;
                }
                if (commitsRun > commitsToRun) {
                    break;
                }

                pb.step();
                pb.setExtraMessage(commit.getCommitHash());
                System.out.printf("Commit: %s, Index: %d\n",commit.getCommitHash(),commit.getCommitIndex());

                //get all assets that changed in commit
                //List<AssetDB> assetsInCommit = allAssetsForProject.parallelStream().filter(a->a.getCommitHash().equalsIgnoreCase(commit.getCommitHash())).collect(Collectors.toList());
                List<AssetDB> allassetsAtCommit = allAssetsForProject.parallelStream().filter(a->a.getCommitIndex()<=commit.getCommitIndex()).collect(Collectors.toList());
                List<AssetMappingDB> assetmappings = assetMappingsForProject.parallelStream().filter(a->a.getCommitIndex()<=commit.getCommitIndex()).collect(Collectors.toList());


                //get distinct asset names at commit
                List<String> allDistinctAssetsAtCommit = allassetsAtCommit.parallelStream().map(AssetDB::getAssetFullName).distinct().collect(Collectors.toList());
                //get distnct folders
                //folders
                double allFoldersSize = 0.0;
                if(allassetsAtCommit.stream().anyMatch(a->a.getAssetType().equalsIgnoreCase("folder"))){
                    allFoldersSize =  allassetsAtCommit.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("folder"))
                            .map(AssetDB::getAssetFullName).distinct().count();
                }
                double mappedFoldersSize = 0.0;
                if(assetmappings.stream().anyMatch(a->a.getAssetType().equalsIgnoreCase("folder"))){
                    mappedFoldersSize =  assetmappings.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("folder")).map(AssetMappingDB::getAssetfullname).distinct().count();
                }
                //files
                double allFilesSize = 0.0;
                if(allassetsAtCommit.stream().anyMatch(a->a.getAssetType().equalsIgnoreCase("file"))){
                    allFilesSize = allassetsAtCommit.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("file"))
                            .map(AssetDB::getAssetFullName).distinct().count();
                }
                double mappedFilesSize = 0.0;
                if(assetmappings.stream().anyMatch(a->a.getAssetType().equalsIgnoreCase("file"))){
                    mappedFilesSize = assetmappings.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("file")).map(AssetMappingDB::getAssetfullname).distinct().count();
                }
                //fragments
                double allFragmentsSize = 0.0;
                if(allassetsAtCommit.stream().anyMatch(a->a.getAssetType().equalsIgnoreCase("fragment"))) {
                allFragmentsSize = allassetsAtCommit.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("fragment"))
                        .map(AssetDB::getAssetFullName).distinct().count();
                }
                double mappedFragmentsSize=0.0;
                if(assetmappings.stream().anyMatch(a->a.getAssetType().equalsIgnoreCase("fragment"))) {
                    mappedFragmentsSize = assetmappings.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("fragment")).map(AssetMappingDB::getAssetfullname).distinct().count();
                }
                //lines
                double allLinesSize = 0.0;
                if(allassetsAtCommit.stream().anyMatch(a->a.getAssetType().equalsIgnoreCase("loc"))){
                    allLinesSize =  allassetsAtCommit.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("loc"))
                            .map(AssetDB::getAssetFullName).distinct().count();
                }
               double mappedLinesSize=0.0;
                if(assetmappings.stream().anyMatch(a->a.getAssetType().equalsIgnoreCase("loc"))){
                    mappedLinesSize =  assetmappings.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("loc")).map(AssetMappingDB::getAssetfullname).distinct().count();
                }
                //get distinct mapped assets
                List<String> allMappedassets = assetmappings.parallelStream().map(AssetMappingDB::getAssetfullname).distinct().collect(Collectors.toList());
                //get distinct mapped Folders
//                List<String> mappedFolders = assetmappings.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("folder")).map(AssetMappingDB::getAssetfullname).distinct().collect(Collectors.toList());
//                //get distinct mapped files
//                List<String> mappedFiles = assetmappings.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("file")).map(AssetMappingDB::getAssetfullname).distinct().collect(Collectors.toList());
//                //get distinct mapped fragments
//                List<String> mappedFragments = assetmappings.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("fragment")).map(AssetMappingDB::getAssetfullname).distinct().collect(Collectors.toList());
//                //get distinct mapped Folders
//                List<String> mappedLines = assetmappings.parallelStream().filter(a -> a.getAssetType().equalsIgnoreCase("loc")).map(AssetMappingDB::getAssetfullname).distinct().collect(Collectors.toList());

                double allAssetsSize = allDistinctAssetsAtCommit.size();



                double tAA = allMappedassets.size();
                double tUA = allAssetsSize - tAA;
                double rAA = tAA / allAssetsSize;
                double rUA = tUA / allAssetsSize;
                double tAFo = mappedFoldersSize;
                double tAFi = mappedFilesSize;
                double tAFra = mappedFragmentsSize;
                double tALoc = mappedLinesSize;
                double rAFo = (allFoldersSize==0.0&&tAFo>0.0)||(allFoldersSize==0.0&&tAFo==0.0)?1.0:tAFo / allFoldersSize;
                double rAFi = tAFi / allFilesSize;
                double rAFra = tAFra / allFragmentsSize;
                double rALoc = tALoc / allLinesSize;
                String project = projectName;
                String commitHash = commit.getCommitHash();
                dataController.commitMetricsInsertFromDB(tAA, tUA, rAA, rUA, tAFo, tAFi, tAFra, tALoc, rAFo, rAFi, rAFra, rALoc, project, commitHash);


            }
        }
    }

    /**
     * CSDEV
     * get cosine similarity between all developers controbuting to asset
     */

    private double csDev(List<String> allDevs, AbstractStringMetric stringMetric) {
        double csDevValue = 0;
        if (true) {
            //get all deves who contributed to the file

            //compare developer names
            if (allDevs.size() == 1) {
                csDevValue = 1;
            } else {

                List<Double> values = new ArrayList<>();
                for (int i = 0; i < allDevs.size(); i++) {
                    for (int j = i + 1; j < allDevs.size(); j++) {
                        double value = stringMetric.getSimilarity(allDevs.get(i), allDevs.get(j));
                        values.add(value);

                    }

                }
                csDevValue = values.parallelStream().filter(d -> d > 0.0).mapToDouble(Double::doubleValue).average().orElse(0);
            }
        }
        return csDevValue;
    }
}
