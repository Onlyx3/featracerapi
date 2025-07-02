package se.gu.ml.preprocessing;

import org.apache.commons.math3.util.Precision;
import se.gu.assets.Asset;
import se.gu.assets.AssetType;
import se.gu.metrics.MLAttribute;
import se.gu.parser.fmparser.FeatureTreeNode;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Utils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DataSaver  {

    private FeatureTreeNode feature;
    private List<Asset> assets;
    private List<DataInstance> data;
    private int otherAssetsCount;

    private AssetType assetType;//type of asset being used for instances

    public DataSaver(MetricsExecutorService metricsExecutorService, FeatureTreeNode feature, List<Asset> assets, List<DataInstance> data, int otherAssetsCount) {

        this.feature = feature;
        this.assets = assets;
        this.data = data;
        this.otherAssetsCount = otherAssetsCount;

        this.assetType = assetType;
    }


    public void run() {
//        metricsExecutorService.waitForTaskToFinish();
//        double sld = 0.0, csm = 0.0;
//        //now add all metrics calclulated
//        for (Asset asset : assets) {
//            sld = getMetricValueFromFutures(MLAttribute.SLD, asset, otherAssetsCount);
//            csm = getMetricValueFromFutures(MLAttribute.CSM, asset, otherAssetsCount);
//
////            if (assetType == AssetType.LOC) {
////                for (Asset loc : asset.getLinesOfCode()) {
////                    setValuesArray(feature, sld, 0.0);
////                }
////            } else if (assetType == AssetType.FRAGMENT) {
////                List<Asset> fragments = asset.getCodeFragments();
////                for (Asset fragment : fragments) {
////                    setValuesArray(feature, sld, 0.0);
////                }
////            } else {
////                setValuesArray(feature, sld, csm);
////            }
//            setValuesArray(feature,asset, sld, csm);
//
//        }


    }

//    private double getMetricValueFromFutures(MLAttribute mlAttribute, Asset asset, int countOfOtherAssets) {
//        double value = metricsExecutorService.getFutures().stream().map(metricFuture -> {
//            try {
//                return metricFuture.get();
//            } catch (InterruptedException | ExecutionException e) {
//                throw new RuntimeException(e);
//            }
//        }).filter(m -> m.getMlAttribute() == mlAttribute && m.getAsset().equals(asset)).map(MetricValue::getScore).collect(Collectors.summingDouble(Double::doubleValue));
//
//        return getAverageScore(countOfOtherAssets, value);
//
//
//    }




}
