package se.gu.assets;

public enum AssetType  {
    REPOSITORY,FOLDER,FILE,CLASS,METHOD, FRAGMENT,LOC, CLUSTER,
    //these below are only relevant for developer scenarios
    //&begin [DeveloperScenarios]
    ANY,FEATURE,ANNOTATION_FILE

    //&end [DeveloperScenarios]
}
