package se.gu.git.scenarios;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import se.gu.assets.Asset;
import se.gu.assets.AssetType;
import se.gu.assets.FeatureAssetMap;
import se.gu.main.ProjectData;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ScenarioHandler implements Serializable {
    private static final long serialVersionUID = 8340385564325198501L;
    private ProjectData projectData;

    public ScenarioHandler(ProjectData projectData) {
        this.projectData = projectData;
    }

    private DeveloperScenario getScenario(AssetType assetType, DeveloperOperation operation) {
        return scenarios.stream().filter(d -> d.getAssetType() == assetType && d.getOperation() == operation).findFirst().get();
    }

    private DeveloperScenario getScenario(AssetType assetType, DeveloperOperation operation, AnnotationPresence folderAnnotation, AnnotationPresence fileAnnotation, AnnotationPresence fragmentAnnotation, AnnotationPresence lineAnnotation, ActionPrecondition precondition) {
        return scenarios.stream().filter(d -> d.getAssetType() == assetType && d.getOperation() == operation
                && d.getFolderAnnotation() == folderAnnotation && d.getFileAnnotation() == fileAnnotation && d.getFragmentAnnonation() == fragmentAnnotation
                && d.getLineAnnotation() == lineAnnotation
                && d.getPrecondition() == precondition).findFirst().get();
    }

    public void updateScenario(AssetType assetType, DeveloperOperation operation, String currentCommit, String assetName) {
        if (StringUtils.isBlank(assetName)) {
            return;
        }
        DeveloperScenario scenario = getScenario(assetType, operation);
        DeveloperScenarioMap developerScenarioMap = new DeveloperScenarioMap(scenario, projectData.getConfiguration().getProjectRepository().getAbsolutePath(), currentCommit, assetName);
        projectData.getDeveloperScenarios().add(developerScenarioMap);
    }

    public void updateScenario(DeveloperScenario ds, Asset asset, String currentCommit) {
        if (StringUtils.isBlank(asset.getFullyQualifiedName())) {
            return;
        }
        ds = getSetScenario(asset, ds);
        DeveloperScenario scenario = getScenario(ds.getAssetType(), ds.getOperation(), ds.getFolderAnnotation(), ds.getFileAnnotation(), ds.getFragmentAnnonation(), ds.getLineAnnotation(), ds.getPrecondition());
        DeveloperScenarioMap developerScenarioMap = new DeveloperScenarioMap(scenario, projectData.getConfiguration().getProjectRepository().getAbsolutePath(), currentCommit, asset.getFullyQualifiedName());
        projectData.getDeveloperScenarios().add(developerScenarioMap);
    }

    public void printScenarios() {
        PrintWriter printWriter = null;
        try {
            String outputFile = String.format("%s\\%s.csv", projectData.getConfiguration().getAnalysisDirectory(), "DeveloperScenarios");
            File output = new File(outputFile);
            if (output.exists()) {
                output.delete();
            }


            printWriter = new PrintWriter(new FileWriter(outputFile, true));

            //print header
            printWriter.print("ScenarioID" + ";");
            printWriter.print("AssetType" + ";");
            printWriter.print("Operation" + ";");
            printWriter.print("Folder Annotations" + ";");
            printWriter.print("File Annotations" + ";");
            printWriter.print("Fragment Annotations" + ";");
            printWriter.print("Line Annotations" + ";");
            printWriter.print("PreCondition" + ";");
            printWriter.print("Prediction Action" + ";");
            printWriter.print("Asset Count" + ";");
            printWriter.print("Commit Count" + ";");
            printWriter.print("Commit List" + ";");
            printWriter.println();

            //now print scenrios
            for (DeveloperScenario scenario : scenarios) {
                printWriter.print(scenario.getScenarioID() + ";");
                printWriter.print(scenario.getAssetType() + ";");
                printWriter.print(scenario.getOperation() + ";");
                printWriter.print(scenario.getFolderAnnotation() + ";");
                printWriter.print(scenario.getFileAnnotation() + ";");
                printWriter.print(scenario.getFragmentAnnonation() + ";");
                printWriter.print(scenario.getLineAnnotation() + ";");
                printWriter.print(scenario.getPrecondition() + ";");
                printWriter.print(scenario.getAction() + ";");
                printWriter.print(projectData.getDeveloperScenarios().stream().filter(s -> s.getDeveloperScenario().getScenarioID().equalsIgnoreCase(scenario.getScenarioID())).count() + ";");
                printWriter.print(projectData.getDeveloperScenarios().stream().filter(s -> s.getDeveloperScenario().getScenarioID().equalsIgnoreCase(scenario.getScenarioID())).map(DeveloperScenarioMap::getCommit).distinct().count() + ";");
                printWriter.print(projectData.getDeveloperScenarios().stream().filter(s -> s.getDeveloperScenario().getScenarioID().equalsIgnoreCase(scenario.getScenarioID())).map(DeveloperScenarioMap::getCommit).distinct().collect(Collectors.joining(",")) + ";");
                printWriter.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }

        }
    }

    private
    List<DeveloperScenario> scenarios = new ArrayList<>(
            Arrays.asList(
                    new DeveloperScenario[]{
                            //folder scenrios
                            new DeveloperScenario(AssetType.FOLDER, "FO1", DeveloperOperation.ADD, AnnotationPresence.YES, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.NONE, PredictionAction.DO_NOTHING),
                            new DeveloperScenario(AssetType.FOLDER, "FO2", DeveloperOperation.ADD, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.PARENT_ANNOTATED_AND_NO_ANNOTATED_SIBLINGS_EXIST, PredictionAction.ASSUME_PARENT_FOLDER_MAPPING),
                            new DeveloperScenario(AssetType.FOLDER, "FO3", DeveloperOperation.ADD, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.PARENT_ANNOTATED_AND_ANNOTATED_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_ANNOTATED_SIBLINGS),
                            new DeveloperScenario(AssetType.FOLDER, "FO4", DeveloperOperation.ADD, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.PARENT_UNANNOTATED_AND_ANNOTATED_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_ANNOTATED_SIBLINGS),
                            new DeveloperScenario(AssetType.FOLDER, "FO5", DeveloperOperation.ADD, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.PARENT_UNANNOTATED_AND_NO_ANNOTATED_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_ALL_SIBLINGS),
                            new DeveloperScenario(AssetType.FOLDER, "FO6", DeveloperOperation.ADD, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.PARENT_UNANNOTATED_AND_NO_OTHER_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_PARENTS_SIBLINGS),
                            new DeveloperScenario(AssetType.FOLDER, "FO7", DeveloperOperation.ADD, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.PARENT_NOT_EXISTS_AND_NO_OTHER_SIBLINGS_EXIST, PredictionAction.DO_NOTHING),
                            new DeveloperScenario(AssetType.FOLDER, "FO8", DeveloperOperation.MAPFEATURE, AnnotationPresence.YES, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.NONE, PredictionAction.RETRAIN_MODEL),
                            //file scenarios
                            new DeveloperScenario(AssetType.FILE, "FL1", DeveloperOperation.ADD, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.NONE, PredictionAction.DO_NOTHING),
                            new DeveloperScenario(AssetType.FILE, "FL2", DeveloperOperation.ADD, AnnotationPresence.YES, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST, PredictionAction.ASSUME_FOLDER_MAPPING),
                            new DeveloperScenario(AssetType.FILE, "FL3", DeveloperOperation.ADD, AnnotationPresence.YES, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.ANNOTATED_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_ANNOTATED_SIBLINGS),
                            new DeveloperScenario(AssetType.FILE, "FL4", DeveloperOperation.ADD, AnnotationPresence.NO, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.ANNOTATED_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_ANNOTATED_SIBLINGS),
                            new DeveloperScenario(AssetType.FILE, "FL5", DeveloperOperation.ADD, AnnotationPresence.NO, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST_AND_OTHER_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_ALL_SIBLINGS),
                            new DeveloperScenario(AssetType.FILE, "FL6", DeveloperOperation.ADD, AnnotationPresence.NO, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST_AND_NO_OTHER_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_CHILDREN_OF_PARENTS_SIBLINGS),
                            new DeveloperScenario(AssetType.FILE, "FL7", DeveloperOperation.ADD, AnnotationPresence.NO, AnnotationPresence.NO, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST_AND_NO_OTHER_SIBLINGS_EXIST_IN_ANCESTRY, PredictionAction.DO_NOTHING),
                            new DeveloperScenario(AssetType.FILE, "FL8", DeveloperOperation.MAPFEATURE, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.NONE, PredictionAction.RETRAIN_MODEL),

                            //fragment scenarios
                            new DeveloperScenario(AssetType.FRAGMENT, "FR1", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, AnnotationPresence.UNDEFINED, ActionPrecondition.NONE, PredictionAction.DO_NOTHING),
                            new DeveloperScenario(AssetType.FRAGMENT, "FR2", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, AnnotationPresence.NO, AnnotationPresence.YES, ActionPrecondition.NOT_ALL_LINES_IN_CLUSTER_ARE_ANNOTATED, PredictionAction.USE_LINE_SCENARIOS),
                            new DeveloperScenario(AssetType.FRAGMENT, "FR3", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, AnnotationPresence.NO, AnnotationPresence.NO, ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST, PredictionAction.ASSUME_FILE_MAPPING),
                            new DeveloperScenario(AssetType.FRAGMENT, "FR4", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, AnnotationPresence.NO, AnnotationPresence.NO, ActionPrecondition.ANNOTATED_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_ANNOTATED_SIBLINGS),
                            new DeveloperScenario(AssetType.FRAGMENT, "FR5", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.NO, AnnotationPresence.NO, AnnotationPresence.NO, ActionPrecondition.ANNOTATED_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_ANNOTATED_SIBLINGS),
                            new DeveloperScenario(AssetType.FRAGMENT, "FR6", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.NO, AnnotationPresence.NO, AnnotationPresence.NO, ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST_AND_CLUSTERS_ARE_TWO_OR_MORE, PredictionAction.PREDICT_WITH_ALL_CLUSTERS),
                            new DeveloperScenario(AssetType.FRAGMENT, "FR7", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.NO, AnnotationPresence.NO, AnnotationPresence.NO, ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST_AND_CLUSTERS_ARE__LESSTHAN_TWO_AND_PARENT_SIBLING_FRAGMENTS_EXIST, PredictionAction.PREDICT_WITH_PARENTS_SIBLING_FRAGMENTS),
                            new DeveloperScenario(AssetType.FRAGMENT, "FR8", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.NO, AnnotationPresence.NO, AnnotationPresence.NO, ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST_AND_CLUSTERS_ARE__LESSTHAN_TWO_AND_PARENT_SIBLING_FRAGMENTS_NOT_EXISTS, PredictionAction.PREDICT_WITH_PARENTS_SIBLING_CLUSTERS),
                            new DeveloperScenario(AssetType.FRAGMENT, "FR9", DeveloperOperation.MAPFEATURE, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, AnnotationPresence.UNDEFINED, ActionPrecondition.NONE, PredictionAction.RETRAIN_MODEL),

                            //LOC scenarios
                            new DeveloperScenario(AssetType.LOC, "LN1", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, ActionPrecondition.NONE, PredictionAction.DO_NOTHING),
                            new DeveloperScenario(AssetType.LOC, "LN2", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, AnnotationPresence.YES, AnnotationPresence.NO, ActionPrecondition.NONE, PredictionAction.ASSUME_FRAGMENT_MAPPING),
                            new DeveloperScenario(AssetType.LOC, "LN3", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, AnnotationPresence.NO, AnnotationPresence.NO, ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST, PredictionAction.ASSUME_FILE_MAPPING),
                            new DeveloperScenario(AssetType.LOC, "LN4", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, AnnotationPresence.NO, AnnotationPresence.NO, ActionPrecondition.ANNOTATED_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_ANNOTATED_SIBLINGS),
                            new DeveloperScenario(AssetType.LOC, "LN5", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.NO, AnnotationPresence.NO, AnnotationPresence.NO, ActionPrecondition.ANNOTATED_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_ANNOTATED_SIBLINGS),
                            new DeveloperScenario(AssetType.LOC, "LN6", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.NO, AnnotationPresence.NO, AnnotationPresence.NO, ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST, PredictionAction.PREDICT_WITH_ALL_SIBLINGS),
                            new DeveloperScenario(AssetType.LOC, "LN7", DeveloperOperation.MAPFEATURE, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.YES, ActionPrecondition.NONE, PredictionAction.RETRAIN_MODEL),

                            //General scenarios
                            new DeveloperScenario(AssetType.FEATURE, "GN10", DeveloperOperation.MOVE, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.NONE, PredictionAction.DO_NOTHING),
                            new DeveloperScenario(AssetType.FEATURE, "GN9", DeveloperOperation.DELETE, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.NONE, PredictionAction.RETRAIN_MODEL),
                            new DeveloperScenario(AssetType.ANNOTATION_FILE, "GN8", DeveloperOperation.ADDorEDIT, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, AnnotationPresence.UNDEFINED, ActionPrecondition.NONE, PredictionAction.DO_NOTHING),

                    }
            ));

    private List<Asset> getSiblings(Asset asset, AssetType assetType){

            return  asset.flattened().filter(a -> a.getAssetType() == assetType && !a.getFullyQualifiedName().equalsIgnoreCase(asset.getFullyQualifiedName())).collect(Collectors.toList());

    }

    private List<Asset> doGetSiblings(Asset asset,AssetType assetType){
        List<Asset> siblings = null;
        for(Asset ancestor:asset.getAncestry()){
            siblings = getSiblings(ancestor,assetType);
            if(siblings.size()>0){
                break;
            }
        }
        return siblings;
    }

    private List<Asset> getAnnotatedSiblings(List<Asset> siblings){
        return projectData.getAssetFeatureMap().stream().map(FeatureAssetMap::getMappedAsset).filter(siblings::contains).collect(Collectors.toList());
    }

    private DeveloperScenario getSetScenario(Asset asset, DeveloperScenario scenario) {
        Asset parent = asset.getParent();
        List<Asset> siblings = null;

        List<Asset> annotatedSiblings = null;
        int annotatedSiblingsSize = 0;

        //by default set precodntion to none
        scenario.setPrecondition(ActionPrecondition.NONE);
//folder scenarios
        if (asset.getAssetType() == AssetType.FOLDER) {
        //if annotations have been added for folder then no need to do anything else
            if (scenario.getFolderAnnotation() == AnnotationPresence.YES) {
                scenario.setPrecondition(ActionPrecondition.NONE);
            } else {
                if (parent != null) {
                    boolean parentIsAnnotated = projectData.getAssetFeatureMap().stream().anyMatch(m -> m.getMappedAsset().getFullyQualifiedName().equalsIgnoreCase(parent.getFullyQualifiedName()));
                    siblings = doGetSiblings(asset,AssetType.FOLDER);
                    annotatedSiblings = getAnnotatedSiblings(siblings);
                    annotatedSiblingsSize = annotatedSiblings.size();
                    if (parentIsAnnotated && annotatedSiblingsSize > 0) {
                        scenario.setPrecondition(ActionPrecondition.PARENT_ANNOTATED_AND_ANNOTATED_SIBLINGS_EXIST);
                    } else if (parentIsAnnotated && annotatedSiblingsSize <= 0) {
                        scenario.setPrecondition(ActionPrecondition.PARENT_ANNOTATED_AND_NO_ANNOTATED_SIBLINGS_EXIST);
                    } else if (!parentIsAnnotated && annotatedSiblingsSize > 0) {
                        scenario.setPrecondition(ActionPrecondition.PARENT_UNANNOTATED_AND_ANNOTATED_SIBLINGS_EXIST);
                    } else if (!parentIsAnnotated && annotatedSiblingsSize <= 0 && siblings.size() > 0) {
                        scenario.setPrecondition(ActionPrecondition.PARENT_UNANNOTATED_AND_NO_ANNOTATED_SIBLINGS_EXIST);
                    } else if (!parentIsAnnotated && siblings.size() <= 0) {
                        scenario.setPrecondition(ActionPrecondition.PARENT_UNANNOTATED_AND_NO_OTHER_SIBLINGS_EXIST);
                    } else {
                        scenario.setPrecondition(ActionPrecondition.NONE);
                    }
                } else {
                    scenario.setPrecondition(ActionPrecondition.PARENT_NOT_EXISTS_AND_NO_OTHER_SIBLINGS_EXIST);
                }
            }
        }
        else if(asset.getAssetType() == AssetType.FILE) {
            setCondition(asset,AssetType.FILE,scenario);

        }
        else if(asset.getAssetType()==AssetType.FRAGMENT){
            setCondition(asset,AssetType.FRAGMENT,scenario);
        }
        else if(asset.getAssetType()==AssetType.LOC){
            setCondition(asset,AssetType.LOC,scenario);
        }
        return scenario;
    }

    private void setCondition(Asset asset,AssetType assetType,DeveloperScenario scenario){
       List<Asset> siblings = doGetSiblings(asset,assetType);
        List<Asset> annotatedSiblings = getAnnotatedSiblings(siblings);
       long annotatedSiblingsSize = annotatedSiblings.size();
        if((assetType== AssetType.FOLDER && scenario.getFolderAnnotation()==AnnotationPresence.YES)||
                (assetType== AssetType.FILE && scenario.getFileAnnotation()==AnnotationPresence.YES)||
                (assetType== AssetType.FRAGMENT && scenario.getFragmentAnnonation()==AnnotationPresence.YES)||
                (assetType== AssetType.LOC && scenario.getLineAnnotation()==AnnotationPresence.YES)){
            scenario.setPrecondition(ActionPrecondition.NONE);
        }else if ( annotatedSiblingsSize > 0) {
            scenario.setPrecondition(ActionPrecondition.ANNOTATED_SIBLINGS_EXIST);
        } else if (annotatedSiblingsSize <= 0) {
            scenario.setPrecondition(ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST);
        } else if (annotatedSiblingsSize <= 0 && siblings.size()>0) {
            scenario.setPrecondition(ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST_AND_OTHER_SIBLINGS_EXIST);
        } else if (annotatedSiblingsSize <= 0 && siblings.size()<=0) {
            scenario.setPrecondition(ActionPrecondition.NO_ANNOTATED_SIBLINGS_EXIST_AND_NO_OTHER_SIBLINGS_EXIST);
        }  else {
            scenario.setPrecondition(ActionPrecondition.NONE);
        }
    }

}
