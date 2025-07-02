package se.gu.assets;

import se.gu.main.ProjectData;
import se.gu.utils.Utilities;

import java.io.Serializable;

public class AssetFunctionCallGenerator implements  Runnable, Serializable {
    private static final long serialVersionUID = -5366693176266882794L;
    Asset asset;

    public AssetFunctionCallGenerator(Asset asset, ProjectData projectData) {
        this.asset = asset;
        this.projectData = projectData;
    }

    ProjectData projectData;
    @Override
    public void run() {
        asset.setFunctionCalls(projectData.getFunctionCallsForAsset(asset));
    }
}
