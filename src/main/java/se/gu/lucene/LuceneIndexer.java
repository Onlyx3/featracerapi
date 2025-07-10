package se.gu.lucene;

import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import se.gu.assets.Asset;
import se.gu.assets.AssetType;
import se.gu.assets.FeatureAssetMap;
import se.gu.main.ProjectData;
import se.gu.utils.Utilities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class LuceneIndexer {
    private ProjectData projectData;
    private SimpleAnalyzer analyzer;
    private List<String> featureNames;
    private File indexPath;
    List<Asset> allMappedAssets;
    private int commitIndex;
    private String currentCommit;

    public LuceneIndexer(ProjectData projectData, int commitIndex, String currentCommit) throws IOException {
        this.projectData = projectData;
        analyzer = new SimpleAnalyzer();
        featureNames = projectData.getAssetFeatureMap().stream().map(FeatureAssetMap::getFeatureName).distinct().collect(Collectors.toList());
        indexPath = new File(String.format("%s/lucene/%s/luceneIndex", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getProjectRepository().getName()));
        Utilities.createOutputDirectory(indexPath.getAbsolutePath(), commitIndex == 0);
        allMappedAssets = projectData.getAssetFeatureMap().stream().map(FeatureAssetMap::getMappedAsset).collect(Collectors.toList());
        this.currentCommit = currentCommit;
        this.commitIndex = commitIndex;
    }

    private void cleanFeatures() {
        List<String> cleanedFeatures = new ArrayList<>();
        for (String feature : featureNames) {
            cleanedFeatures.add(feature.replace(String.format("%s::", projectData.getConfiguration().getAnalysisDirectory().getName()), "").replace("::", ""));
        }
        featureNames = cleanedFeatures;
    }

    private String cleanFeature(String feature) {
        return feature.replace(String.format("%s::", projectData.getConfiguration().getAnalysisDirectory().getName()), "").replace("::", "");

    }

    public void indexAssetsForLucene() throws IOException {
        //index assets
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        Directory indexDirectory = FSDirectory.open(Paths.get(indexPath.toURI()));
        IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);
        int size = allMappedAssets.size();
        try (ProgressBar pb = new ProgressBar("Indexing assets:", size)) {
            //first get all file assets and delete them if they already exist in the index
            List<Asset> fileAssets = allMappedAssets.stream().filter(a->a.getAssetType()== AssetType.FILE).collect(Collectors.toList());
            for(Asset file:fileAssets){
                try {
                    deleteDocumentsFromIndexUsingTerm(file.getFullyQualifiedName());
                }catch (Exception ex){

                }
            }
            //now index the assets
            for (Asset asset : allMappedAssets) {
                pb.step();
                pb.setExtraMessage(asset.getAssetName());
                addAssetToIndex(asset, indexWriter);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            indexWriter.close();
        }

    }

    public void deleteDocumentsFromIndexUsingTerm(String path) throws IOException, ParseException {


        Term term = new Term("path", path);
        Term parentTerm = new Term("parent", path);//delete an indexed file together with it's indexed lines of code

        System.out.println("Deleting documents with field 'path'" + path + "'");
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        Directory indexDirectory = FSDirectory.open(Paths.get(indexPath.toURI()));
        IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);
        indexWriter.deleteDocuments(term, parentTerm);
        indexWriter.close();

    }

    public void searchAssets() throws IOException, ParseException {
        File resultsFile = new File(String.format("%s/lucene/%s/searchResults.csv", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getProjectRepository().getName()));
        File mappingsFile = new File(String.format("%s/lucene/%s/assetFeatureMapping.csv", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getProjectRepository().getName()));
        File summaryFile = new File(String.format("%s/lucene/%s/resultSummary.csv", projectData.getConfiguration().getAnalysisDirectory(), projectData.getConfiguration().getProjectRepository().getName()));
        boolean isPrintHeader = commitIndex == 0;
        double luceneThreshold = projectData.getConfiguration().getLuceneThreshold();
        if (commitIndex == 0) {
            Utilities.deleteFile(resultsFile.getAbsolutePath());
            Utilities.deleteFile(mappingsFile.getAbsolutePath());
            Utilities.deleteFile(summaryFile.getAbsolutePath());
        }
        //==============write mapping file=====================
        System.out.println("Writing mapping of features to assets");
        PrintWriter writer = new PrintWriter(new FileWriter(mappingsFile, true));
        try {
            if (isPrintHeader) {
                writer.println("assetParent;assetFullName;assetType;featureName;commitIndex;commit");
            }
            for (FeatureAssetMap assetMap : projectData.getAssetFeatureMap()) {
                writer.printf("%s;%s;%s;%s;%d;%s\n", assetMap.getMappedAsset().getParent() != null ? assetMap.getMappedAsset().getParent().getFullyQualifiedName() : "",
                        assetMap.getMappedAsset().getFullyQualifiedName(), assetMap.getMappedAsset().getAssetType(), assetMap.getFeatureName(), commitIndex + 1, currentCommit);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            writer.close();
        }
        //===================SEARCH RESULTS=======================
        Directory indexDirectory = FSDirectory.open(Paths.get(indexPath.toURI()));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(new String[]{"contents", "path"}, analyzer);
        //write detailed search results
        writer = new PrintWriter(new FileWriter(resultsFile, true));
        PrintWriter summaryWriter = new PrintWriter(new FileWriter(summaryFile, true));
        if (isPrintHeader) {
            summaryWriter.println("feature;mappedAssets;foundAssets;macthedAssets;precision;recall;fscore;commitIndex;commit");
        }
        try {
            if (isPrintHeader) {
                writer.println("assetParent;assetFullName;assetType;score;featureName;commitIndex;commit");
            }
            int numFeatures = featureNames.size();
            try (ProgressBar pb = new ProgressBar("Searching:", numFeatures)) {
                for (String feature : featureNames) {
                    if (feature.contains("/")) {
                        continue;
                    }
                    pb.step();
                    pb.setExtraMessage(feature);
                    String cleanFeature = feature.split("//")[0];
                    cleanFeature = cleanFeature.replace(String.format("%s::", projectData.getConfiguration().getAnalysisDirectory().getName()), "").replace(":", " ")
                            .replace("/", " ").replace("\"", " ").replace("'", " ");

                    String query = Arrays.stream(cleanFeature.split(projectData.getConfiguration().getCamelCaseSplitRegex())).collect(Collectors.joining(" "));
                    List<String> featureAssets = projectData.getAssetFeatureMap().stream().filter(m -> m.getFeatureName().equalsIgnoreCase(feature)).map(FeatureAssetMap::getMappedAsset)
                            .map(Asset::getFullyQualifiedName).distinct().collect(Collectors.toList());
                    int totalMappedDocs = featureAssets.size();
                    Map<List<Document>, List<Float>> searchResult = locateAssets(query, queryParser, searcher);
                    List<Document> allDocs = searchResult.entrySet().stream().findFirst().get().getKey();
                    List<Float> allScores = searchResult.entrySet().stream().findFirst().get().getValue();
                    List<Document> docs = new ArrayList<>();
                    List<Float> scores = new ArrayList<>();
                    List<String> addedPaths = new ArrayList<>();
                    for(int i=0;i<allDocs.size();i++){

                        if(!addedPaths.contains(allDocs.get(i).get("path"))){
                            addedPaths.add(allDocs.get(i).get("path"));
                            docs.add(allDocs.get(i));
                            scores.add(allScores.get(i));
                        }
                    }
                    int totalFoundDocs = docs.size();
                    int matchedDocs = 0;

                    for (int i = 0; i < docs.size(); i++) {
                        writer.printf("%s;%s;%s;%.4f;%s;%d;%s\n", docs.get(i).get("parent"), docs.get(i).get("path"), docs.get(i).get("assetType"), scores.get(i), feature, commitIndex + 1, currentCommit);
                        //============SUMMARY RESULTS=========================
                        if (featureAssets.contains(docs.get(i).get("path"))) {// && scores.get(i)>=luceneThreshold
                            matchedDocs++;
                        }

                    }
                    //============SUMMARY RESULTS=========================
                    double precision = (double) matchedDocs / (double) totalFoundDocs;
                    double recall = (double) matchedDocs / (double) totalMappedDocs;
                    double fscore = (2 * precision * recall) / (precision + recall);
                    summaryWriter.printf("%s;%d;%d;%d;%.4f;%.4f;%.4f;%d;%s\n", feature, totalMappedDocs, totalFoundDocs, matchedDocs, precision, recall, fscore, commitIndex + 1, currentCommit);


                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            writer.close();
            summaryWriter.close();
        }


    }

    public Map<List<Document>, List<Float>> locateAssets(String queryString, QueryParser queryParser, IndexSearcher searcher) throws ParseException, IOException {

        //queryParser.setDefaultOperator(QueryParser.Operator.AND);
        Query query = queryParser.parse(queryString);

        TopDocs topDocs = searcher.search(query, 2000);

        List<Float> scores = Arrays.stream(topDocs.scoreDocs).map(sd -> sd.score)
                .collect(Collectors.toList());

        List<Document> docs = Arrays.stream(topDocs.scoreDocs).map(scoreDoc -> {
            Document mydoc = null;
            try {
                mydoc = searcher.doc(scoreDoc.doc);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return mydoc;
        })
                .collect(Collectors.toList());

        Map<List<Document>, List<Float>> result = new HashMap<>();
        result.put(docs, scores);
        return result;

    }


    private String stripFeaturesFromString(String content) {
        for (String feature : featureNames) {
            if (!StringUtils.isBlank(content)) {
                feature = feature.replace(String.format("%s::", projectData.getConfiguration().getAnalysisDirectory().getName()), "");
                content = content.replace(feature, "");
            }
        }
        return content;
    }

    public void addAssetToIndex(Asset asset, IndexWriter indexWriter) throws IOException {
        String assetContent = stripFeaturesFromString(asset.getAssetContent());
        if (StringUtils.isBlank(assetContent)) {
            return;
        }

        Document document = new Document();
        document.add(new TextField("contents", assetContent, Field.Store.YES));
        document.add(new TextField("path", asset.getFullyQualifiedName(), Field.Store.YES));
        document.add(new StringField("assetType", asset.getAssetType().toString(), Field.Store.YES));
        document.add(new TextField("parent", asset.getParent() != null ? asset.getAssetType()== AssetType.LOC?asset.getParent().getParent().getFullyQualifiedName(): asset.getParent().getFullyQualifiedName() : "", Field.Store.YES));
        indexWriter.addDocument(document);

    }
}
