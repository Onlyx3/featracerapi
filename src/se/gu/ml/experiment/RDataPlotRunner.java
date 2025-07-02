package se.gu.ml.experiment;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import se.gu.main.Configuration;
import se.gu.utils.Utilities;

import java.io.*;
import java.util.List;
import java.util.regex.Pattern;

public class RDataPlotRunner implements Serializable {
    private Configuration configuration;

    public RDataPlotRunner(Configuration configuration) {
        this.configuration = configuration;
    }

    public void assignRowIDs() throws IOException {
        File rFolder = configuration.getrDataFolder();
        File[] rFiles = rFolder.listFiles();


        try (ProgressBar pb = new ProgressBar("add row IDS", rFiles.length)) {
            for (File file : rFiles) {
                pb.step();
                if (file.isDirectory() || !file.getName().contains(".csv")) {
                    continue;
                }
                List<String> lines = FileUtils.readLines(file, configuration.getTextEncoding());
                if (lines.size() <= 0) {
                    continue;
                }
                //delete file
                FileUtils.forceDelete(file);
                //recreate it
                PrintWriter printWriter = new PrintWriter(new FileWriter(file, true));

                int size = lines.size();
                //fist line is header

                printWriter.println(String.format("RowID;%s", lines.get(0)));

                for (int i = 1; i < size; i++) {//skip first line
                    printWriter.println(String.format("%d;%s", i, lines.get(i)));
                }

                if (printWriter != null) {
                    printWriter.close();
                }
            }


        }
    }

    public void combineDatasetStats() throws IOException {
        File rFolder = configuration.getrDataFolder();
        String[] balancedOptions = new String[]{"im"};
        String[] clusterOptions = new String[]{"diff"};
        String[] projects = new String[]{"viz", "ide", "config", "tools", "marlin"};
        String[] features = new String[]{"11f","8f", "6f", "4f"};
        String[] codeLevels = new String[]{"fragment", "loc", "file"};
        String[] fileTypes = new String[]{"ps"};
        for (String balanced : balancedOptions) {
            for (String cluster : clusterOptions) {
                String combinedFileName = String.format("%s/combined_datasetstats_%s_%s.csv", rFolder.getAbsolutePath(), balanced, cluster);
                printDatasetStats(combinedFileName, balanced, cluster, rFolder, "datasetstats", null, null, null);
            }
        }

        //now combine proediction datasets
        for (String project : projects) {
            for (String codeLevel : codeLevels) {
                for (String fileType : fileTypes) {
                    //for(String feature:features){
                    for (String balanced : balancedOptions) {
                        String combinedPredictionFileName = String.format("%s/combined_%s_%s_%s_%s_reg.csv", rFolder.getAbsolutePath(), project, codeLevel, fileType, balanced);
                        printDatasetStats(combinedPredictionFileName, balanced, null, rFolder, fileType, project, codeLevel, null);
                    }

                    // }
                }
            }


        }
        //now combine measures for predictions
        for (String project : projects) {
            for (String codeLevel : codeLevels) {
                for (String fileType : fileTypes) {
                    for (String feature : features) {
                        for (String balanced : balancedOptions) {
                            String combinedMeasuresFileName = String.format("%s/combined_measures_%s_%s_%s_%s_reg_diff_%s.csv", rFolder.getAbsolutePath(), project, codeLevel, fileType, balanced, feature);
                            printDatasetStats(combinedMeasuresFileName, balanced, null, rFolder, fileType, project, codeLevel, feature);
                        }

                    }
                }
            }
        }
        //now combine measures for all projects grouped by nfeature

        for (String feature : features) {

            String combinedMeasuresFileName = String.format("%s/combined_project_measures_%s.csv", rFolder.getAbsolutePath(), feature);
            printDatasetStats(combinedMeasuresFileName, null, null, rFolder, "combined_measures", null, null, feature);


        }


        //now delete files with zero KB
        File[] rFiles = rFolder.listFiles();
        int count = 0;
        try (ProgressBar pb = new ProgressBar("deleting 0KB files:", rFiles.length)) {
            for (File file : rFiles) {
                pb.step();
                if (file.isDirectory()) {
                    continue;
                }

                if (file.length() <= 0) {
                    FileUtils.forceDelete(file);
                    count++;
                }

            }

        }
        System.out.printf("Deleted %s 0KB files\n", count);

    }

    private void printDatasetStats(String fileName, String balanced, String cluster, File rFolder, String fileType, String proj, String codeLevel, String nFeature) throws IOException {
        File statsFile = new File(fileName);
        if (statsFile.exists()) {
            FileUtils.forceDelete(statsFile);
        }
        PrintWriter printWriter = new PrintWriter(new FileWriter(fileName, true));
        boolean headerCreated = false;
        String level = null;
        String project = null;
        String bal = null, clu = null, feature = null, type = null;
        File[] rFiles = rFolder.listFiles();
        String extension = null;
        try (ProgressBar pb = new ProgressBar(String.format("combining %s:", fileType), rFiles.length)) {
            for (File file : rFiles) {
                pb.step();
                extension = FilenameUtils.getExtension(file.getName());
                if (!extension.equalsIgnoreCase("csv") || !file.getName().contains(fileType) || (!fileType.equalsIgnoreCase("combined_measures") && file.getName().contains("combined"))) {
                    continue;
                }
                String[] fileNameParts = file.getName().split("_");
                if (fileNameParts.length < 6) {
                    continue;
                }
                if (fileType.equalsIgnoreCase("combined_measures")) {
                    feature = fileNameParts[8].split(Pattern.quote("."))[0];

                } else {
                    level = fileNameParts[1];
                    project = fileNameParts[0];
                    type = fileNameParts[2];
                    bal = fileNameParts[3];
                    clu = fileNameParts[5].split(Pattern.quote("."))[0];

                    if (fileType.equalsIgnoreCase(type) && fileNameParts.length >= 7) {
                        feature = file.getName().split("_")[6].split(Pattern.quote("."))[0];
                    }
                }
                //now print data
                if (fileType.equalsIgnoreCase("datasetstats") && type.equalsIgnoreCase(fileType)) {
                    if (bal.equalsIgnoreCase(balanced) && clu.equalsIgnoreCase(cluster)) {
                        List<String> lines = FileUtils.readLines(file, configuration.getTextEncoding());
                        int size = lines.size();
                        //fist line is header
                        if (headerCreated == false) {
                            printWriter.println(String.format("%sLevel;project", lines.get(0)));
                            headerCreated = true;
                        }
                        for (int i = 1; i < size; i++) {//skip first line
                            printWriter.println(String.format("%s%s;%s", lines.get(i), level, project));
                        }


                    }
                } else if (fileType.equalsIgnoreCase("combined_measures") && file.getName().startsWith(fileType)) {
                    if (feature.equalsIgnoreCase(nFeature)) {
                        List<String> lines = FileUtils.readLines(file, configuration.getTextEncoding());
                        if(lines==null||lines.size()<=0){continue;}
                        int size = lines.size();
                        //fist line is header
                        if (headerCreated == false) {
                            printWriter.println(String.format("%s", lines.get(0)));
                            headerCreated = true;
                        }
                        for (int i = 1; i < size; i++) {//skip first line
                            printWriter.println(String.format("%s", lines.get(i)));
                        }


                    }
                } else if (nFeature != null && nFeature.equalsIgnoreCase(feature) && bal.equalsIgnoreCase(balanced) && project.equalsIgnoreCase(proj) && level.equalsIgnoreCase(codeLevel) && type.equalsIgnoreCase(fileType)) {
                    List<String> lines = FileUtils.readLines(file, configuration.getTextEncoding());
                    //System.out.println(file.getName());
                    int size = lines.size();
                    //fist line is header
                    if (headerCreated == false) {
                        printWriter.println(String.format("%s%sLevel;project;clusters;feature;measure;measurevalue", lines.get(0), lines.get(0).endsWith(";") ? "" : ";"));
                        headerCreated = true;
                    }
                    //precision
                    for (int i = 1; i < size; i++) {//skip first line
                        //System.out.println(lines.get(i));

                        printWriter.println(String.format("%s%s%s;%s;%s;%s;%s;%s", lines.get(i), lines.get(1).endsWith(";") ? "" : ";", level, project, clu, feature,
                                "precision", lines.get(i).split(Pattern.quote(";"))[18]));
                    }
                    //recall
                    for (int i = 1; i < size; i++) {//skip first line
                        //System.out.println(lines.get(i));

                        printWriter.println(String.format("%s%s%s;%s;%s;%s;%s;%s", lines.get(i), lines.get(1).endsWith(";") ? "" : ";", level, project, clu, feature,
                                "recall", lines.get(i).split(Pattern.quote(";"))[20]));
                    }
                    //fscore
                    for (int i = 1; i < size; i++) {//skip first line
                        //System.out.println(lines.get(i));

                        printWriter.println(String.format("%s%s%s;%s;%s;%s;%s;%s", lines.get(i), lines.get(1).endsWith(";") ? "" : ";", level, project, clu, feature,
                                "fscore", lines.get(i).split(Pattern.quote(";"))[22]));
                    }


                } else if (nFeature == null && !fileType.equalsIgnoreCase("datasetstats") && type.equalsIgnoreCase(fileType)) {
                    if (bal.equalsIgnoreCase(balanced) && project.equalsIgnoreCase(proj) && level.equalsIgnoreCase(codeLevel) && type.equalsIgnoreCase(fileType)) {
                        List<String> lines = FileUtils.readLines(file, configuration.getTextEncoding());
                        int size = lines.size();
                        //fist line is header
                        if (headerCreated == false) {
                            printWriter.println(String.format("%s%sLevel;project;clusters;feature", lines.get(0), lines.get(0).endsWith(";") ? "" : ";"));
                            headerCreated = true;
                        }
                        for (int i = 1; i < size; i++) {//skip first line
                            printWriter.println(String.format("%s%s%s;%s;%s;%s", lines.get(i), lines.get(1).endsWith(";") ? "" : ";", level, project, clu, feature));
                        }


                    }
                }


            }
        }
        if (printWriter != null) {
            printWriter.close();
        }

    }
}
