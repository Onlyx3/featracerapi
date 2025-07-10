package se.gu.metrics;

public enum MLAttribute {
    //ADD-Normalized lines added, DEL--nmormalized lines deleted
    //COMMITS--count of commits in whihc the asset has been changed
    //NEA--Number of existing annotations for feature in which asset is included
    //CSM--Cosine similarity metric
    //GED--Graph Edit Distance obtained for each asset
    //SLD--Source code location distance (based on tree edit distance for each asset
    SLD,GED,CSM,NEA,COMMITS,ADD,DEL
}
