package se.gu.assets;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class AssetContentExtractor implements Runnable, Serializable {
    private static final long serialVersionUID = 1890143458225896660L;
    private Asset asset;

    public AssetContentExtractor(Asset asset) {
        this.asset = asset;
    }

    @Override
    public void run() {
        asset.setAssetContent(asset.getCombinedContent());
    }
}
