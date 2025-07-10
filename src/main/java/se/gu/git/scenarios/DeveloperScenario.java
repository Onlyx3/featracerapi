package se.gu.git.scenarios;

import se.gu.assets.Asset;
import se.gu.assets.AssetType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DeveloperScenario implements Serializable {
    private static final long serialVersionUID = 6786412123770926744L;
    private String scenarioID;
    private DeveloperOperation operation;
    private AnnotationPresence folderAnnotation, fileAnnotation, fragmentAnnonation, lineAnnotation;
    private ActionPrecondition precondition;
    private PredictionAction action;
    private List<String> commits;
    private List<Asset> assets;

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    private AssetType assetType;
    private long assetCount;

    public long getAssetCount() {
        return assetCount;
    }

    public void setAssetCount(long assetCount) {
        this.assetCount = assetCount;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public List<String> getCommits() {
        return commits;
    }

    public void addCommit(String commit) {
        if(!commits.contains(commit)){
            commits.add(commit);
        }
    }

    public DeveloperScenario(AssetType assetType,DeveloperOperation operation, AnnotationPresence folderAnnotation, AnnotationPresence fileAnnotation, AnnotationPresence fragmentAnnonation, AnnotationPresence lineAnnotation) {
        this.operation = operation;
        this.folderAnnotation = folderAnnotation;
        this.fileAnnotation = fileAnnotation;
        this.fragmentAnnonation = fragmentAnnonation;
        this.lineAnnotation = lineAnnotation;
        this.assetType = assetType;
    }

    public DeveloperScenario(AssetType assetType, String scenarioID, DeveloperOperation operation, AnnotationPresence folderAnnotation, AnnotationPresence fileAnnotation, AnnotationPresence fragmentAnnonation, AnnotationPresence lineAnnotation, ActionPrecondition precondition, PredictionAction action) {
        this.assetType = assetType;
        this.scenarioID = scenarioID;
        this.operation = operation;
        this.folderAnnotation = folderAnnotation;
        this.fileAnnotation = fileAnnotation;
        this.fragmentAnnonation = fragmentAnnonation;
        this.lineAnnotation = lineAnnotation;
        this.precondition = precondition;
        this.action = action;
        commits = new ArrayList<>();
        assets = new ArrayList<>();
    }

    public String getScenarioID() {
        return scenarioID;
    }

    public void setScenarioID(String scenarioID) {
        this.scenarioID = scenarioID;
    }

    public DeveloperOperation getOperation() {
        return operation;
    }

    public void setOperation(DeveloperOperation operation) {
        this.operation = operation;
    }

    public AnnotationPresence getFolderAnnotation() {
        return folderAnnotation;
    }

    public void setFolderAnnotation(AnnotationPresence folderAnnotation) {
        this.folderAnnotation = folderAnnotation;
    }

    public AnnotationPresence getFileAnnotation() {
        return fileAnnotation;
    }

    public void setFileAnnotation(AnnotationPresence fileAnnotation) {
        this.fileAnnotation = fileAnnotation;
    }

    public AnnotationPresence getFragmentAnnonation() {
        return fragmentAnnonation;
    }

    public void setFragmentAnnonation(AnnotationPresence fragmentAnnonation) {
        this.fragmentAnnonation = fragmentAnnonation;
    }

    public AnnotationPresence getLineAnnotation() {
        return lineAnnotation;
    }

    public void setLineAnnotation(AnnotationPresence lineAnnotation) {
        this.lineAnnotation = lineAnnotation;
    }

    public ActionPrecondition getPrecondition() {
        return precondition;
    }

    public void setPrecondition(ActionPrecondition precondition) {
        this.precondition = precondition;
    }

    public PredictionAction getAction() {
        return action;
    }

    public void setAction(PredictionAction action) {
        this.action = action;
    }
}
