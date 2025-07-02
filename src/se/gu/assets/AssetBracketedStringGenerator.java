package se.gu.assets;

import se.gu.main.ProjectData;
import se.gu.utils.Utilities;
import sun.plugin2.message.Serializer;

import java.io.Serializable;

public class AssetBracketedStringGenerator implements Runnable, Serializable {
    private static final long serialVersionUID = -9200866991391639832L;
    private Asset asset;

    public AssetBracketedStringGenerator(Asset asset, ProjectData projectData) {
        this.asset = asset;
        this.projectData = projectData;
    }

    private ProjectData projectData;

    @Override
    public void run() {
        asset.setBracketedString(Utilities.getBracketedString(asset.getFunctionCalls()));
    }
}
