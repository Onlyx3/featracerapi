package se.gu.metrics;

import org.repodriller.domain.Modification;
import org.repodriller.scm.BlamedLine;
import se.gu.assets.Asset;
import se.gu.assets.AssetType;
import se.gu.main.ProjectData;
import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MetricCalculator {
    private Asset asset;
    private ProjectData projectData;
    private List<BlamedLine> blamedLines;
    AbstractStringMetric stringMetric;


    public MetricCalculator(Asset asset, ProjectData projectData, List<BlamedLine> blamedLines, AbstractStringMetric stringMetric) {
        this.asset = asset;
        this.projectData = projectData;
        this.blamedLines = blamedLines;
        this.stringMetric = stringMetric;
    }
    public static void createMetricsForAsset(String relativeFilePath, String assetFullPath, ProjectData projectData, List<BlamedLine> blamedLines, AbstractStringMetric stringMetric, List<Modification> modifications){
        List<String> allDevs = blamedLines.parallelStream().map(BlamedLine::getAuthor).distinct().collect(Collectors.toList());
        List<String> commits = blamedLines.parallelStream().map(BlamedLine::getCommit).distinct().collect(Collectors.toList());
        Map<String,Double> contributions = getDeveloperContributions(allDevs,blamedLines);
        double csvDev = csDev(relativeFilePath,allDevs,stringMetric);
        double ddev = allDevs.size();
        double comm = commits.size();
        double dcont = contributions.values().parallelStream().mapToDouble(Double::doubleValue).average().getAsDouble();
        double hdcont = contributions.values().parallelStream().mapToDouble(Double::doubleValue).max().getAsDouble();
        double ccc = modifications.size();


        System.out.printf("file: %s: CSDEV: %.4f",relativeFilePath,csvDev);

    }
    //CSDEV
    //get cosine similarity netween all developers controbuting to asset
    private static double csDev(String fileName, List<String> allDevs, AbstractStringMetric stringMetric){
        double csDevValue = 0;
        if(true){
            //get all deves who contributed to the file

            //compare developer names
            if(allDevs.size()==1){
                csDevValue=1;
            }else {

                List<Double> values = new ArrayList<>();
                for (int i = 0; i < allDevs.size(); i++) {
                    for (int j = i + 1; j < allDevs.size(); j++) {
                        double value = stringMetric.getSimilarity(allDevs.get(i), allDevs.get(j));
                        values.add(value);

                    }

                }
                csDevValue = values.parallelStream().filter(d -> d < 0.0).mapToDouble(Double::doubleValue).average().orElse(0);
            }
        }
        return csDevValue;
    }

    private static  Map<String,Double> getDeveloperContributions(List<String> allDevs,List<BlamedLine>blamedLines){
        Map<String,Double> devExp = new HashMap<>();
        for(String dev:allDevs){
            double lines = (blamedLines.parallelStream().filter(b->b.getAuthor().equalsIgnoreCase(dev)).count())/blamedLines.size();
            devExp.put(dev,lines);
        }
        return devExp;
    }


    //EXP
    //OEXP
    //CCC
    //ACCC
    //NLOC
    //NFCCA
    //CSFCCA
    //ACSFCCA
}
