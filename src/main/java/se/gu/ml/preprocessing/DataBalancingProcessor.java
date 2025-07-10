package se.gu.ml.preprocessing;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import se.gu.main.ProjectData;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DataBalancingProcessor  implements Serializable {
    private static final long serialVersionUID = 4446347288137948051L;
    private ProjectData projectData;
    private File dataFilesFolder;
    private List<ImbalancedData> imbalancedDataList;
    private String imbalancedFolderName,balancedFolderName;


    public DataBalancingProcessor(ProjectData projectData) {
        this.projectData = projectData;
        dataFilesFolder = new File(projectData.getConfiguration().getDataFilesImbalancedDirectory());//since we want imbalanced data
        imbalancedFolderName = dataFilesFolder.getName();
        balancedFolderName = new File(projectData.getConfiguration().getDataFilesSubDirectory()).getName();//assuming that now we are balancing
    }

    public void createBalancedDataSets() throws IOException {
        Mlsmote mlsmote = new Mlsmote();
        int labelCombination = projectData.getConfiguration().getmLSMOTELabelCombination();
        imbalancedDataList = new ArrayList<>();



        doBalanceDataSet(dataFilesFolder, mlsmote, imbalancedDataList);
        try (ProgressBar pb = new ProgressBar("Balancing datasets:", imbalancedDataList.size())) {
        for (ImbalancedData imbalancedData : imbalancedDataList) {
            pb.step();
            if (imbalancedData != null) {

                File outputFolder = new File(imbalancedData.getParentCommitFolder().getAbsolutePath().replace(imbalancedFolderName,balancedFolderName));
                Utilities.createOutputDirectory(outputFolder.getAbsolutePath(), true);

                mlsmote.execute(
                        imbalancedData.getParentCommitFolder().getAbsolutePath(),
                        outputFolder.getAbsolutePath(),
                        projectData.getConfiguration().getCodeAbstractionLevel() + ".arff",
                        imbalancedData.getXmlFile().getAbsolutePath(),
                        labelCombination);


            }
        }
    }




    }

    public void moveFiles() throws IOException {
        //copy files to output folder for use excluding arrf
        try (ProgressBar pb = new ProgressBar("copying files:", imbalancedDataList.size())) {
            for (ImbalancedData imbalancedData : imbalancedDataList) {
                pb.step();
                String targetFolder = imbalancedData.getParentCommitFolder().getAbsolutePath().replace(imbalancedFolderName,balancedFolderName);
                File outputFolder = new File(targetFolder);
                File[] commitFiles = imbalancedData.getParentCommitFolder().listFiles();
                for (File file : commitFiles) {
                    if (Utilities.isARFFFile(file) && !file.getName().contains("PREDICT")) {
                        continue;
                    }
                    if (file.isDirectory()) {
                        continue;
                    }
                    FileUtils.copyFileToDirectory(file, outputFolder);
                }


            }
        }


//        //backup folder for imbanaced datasets
//        String backup = projectData.getConfiguration().getAnalysisDirectory()+"/backup";
//        Utilities.createOutputDirectory(backup,true);
//        for(ImbalancedData imbalancedData: imbalancedDataList){
////            move old file to backup
//            try {
//                Path source = Paths.get(imbalancedData.getArffFile().toURI());
//                File target = new File(String.format("%s/%s/%s", backup,imbalancedData.getArffFile().getParentFile().getName(),imbalancedData.getArffFile().getName()));
//                FileUtils.moveFile(source.toFile(),target);
//               // Files.move(source, source.resolveSibling(imbalancedData.getArffFile().getName().replace(".arff", ".IMBALANCED")));
//            }catch (Exception ex){
//                ex.printStackTrace();
//            }
//            //now move balanced dataset
//            try {
//                //delete old arff
//                if(imbalancedData.getArffFile().exists()) {
//                    FileUtils.forceDelete(imbalancedData.getArffFile());
//                }
//                //now move balanced arrf
//            Path balanced = Paths.get(String.format("%s/out/%s",imbalancedData.getParentCommitFolder(),imbalancedData.getArffFile().getName()));
//            Path newdir = Paths.get(imbalancedData.getParentCommitFolder().toURI());
////            Path existingFilePath = newdir.resolve(balanced.getFileName());
////            File existingFile = balanced.toFile();
//
//            FileUtils.moveFileToDirectory(balanced.toFile(),newdir.toFile(),false);
//
////            boolean cantWrite = true;
////            do{
////                try{
////                    Files.move(balanced, existingFilePath, StandardCopyOption.REPLACE_EXISTING);
////                    cantWrite=false;
////                }catch (FileSystemException e){
////
////                    cantWrite=true;
////                    Thread.sleep(1000);
////                }
////            }while (cantWrite);
//
//            //Files.move(balanced, existingFilePath, StandardCopyOption.REPLACE_EXISTING);
//
//            //now delete output directory
//            FileUtils.deleteDirectory(new File(String.format("%s/out",imbalancedData.getParentCommitFolder())));
//            }catch (Exception ex){
//                ex.printStackTrace();
//            }

//        }
    }

    private void doBalanceDataSet(File folder, Mlsmote mlsmote, List<ImbalancedData> imbalancedDataList) throws IOException {
        ImbalancedData imbalancedData = null;
        if (!folder.getAbsolutePath().equalsIgnoreCase(dataFilesFolder.getAbsolutePath())&& !folder.getName().equalsIgnoreCase("out")) {
            imbalancedData = new ImbalancedData();
        }
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                doBalanceDataSet(file, mlsmote,imbalancedDataList);
            } else {
                if (imbalancedData == null) {
                    continue;
                }

                if (Utilities.isXMLFile(file) && file.getName().contains("FLAT")) {
                    imbalancedData.setXmlFile(file);
                    imbalancedData.setParentCommitFolder(file.getParentFile());
                } else if (Utilities.isARFFFile(file) && !file.getName().contains("PREDICT")) {
                    imbalancedData.setArffFile(file);
                }

            }
        }
        //now create
        if(imbalancedData!=null){
            imbalancedDataList.add(imbalancedData);
        }


    }
}

class ImbalancedData  implements Serializable {
    private static final long serialVersionUID = -3476290045557902286L;
    private File arffFile;
    private File xmlFile;

    public File getParentCommitFolder() {
        return parentCommitFolder;
    }

    public void setParentCommitFolder(File parentCommitFolder) {
        this.parentCommitFolder = parentCommitFolder;
    }

    private File parentCommitFolder;

    public File getArffFile() {
        return arffFile;
    }

    public void setArffFile(File arffFile) {
        this.arffFile = arffFile;
    }

    public File getXmlFile() {
        return xmlFile;
    }

    public void setXmlFile(File xmlFile) {
        this.xmlFile = xmlFile;
    }
}