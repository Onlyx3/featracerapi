package se.gu.metrics.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Cosine similarity calculator class
 * @author Mukelabai Mukelabai
 * @date 2019-06-24
 * With inspiration from http://computergodzilla.blogspot.com/2013/07/how-to-calculate-tf-idf-of-document.html
 */
public class CosineSimilarity {


    public static double getAverageCosineSimilarityForFeatureAssets(List<String> assetsContents)  {

        List<String[]> termsDocsArray = new ArrayList<String[]>();
        List<String> allTerms = new ArrayList<String>(); //to hold all terms
        List<double[]> tfidfDocsVector = new ArrayList<double[]>();

        for(String content:assetsContents){
            addTokenizedText(content, termsDocsArray, allTerms);
        }
        //create tf-idf scores
        calculateTFIDF(termsDocsArray, allTerms, tfidfDocsVector);

        //calculate average score for all documents belonging to one feature
        double sum = 0.0,score=0.0;
        int size = tfidfDocsVector.size();
        int count=0;
        for(int i=0;i<size;i++){
            for(int j=i;j<size;j++){
                sum+=cosineSimilarity(tfidfDocsVector.get(i), tfidfDocsVector.get(j));
                count++;
            }
        }
        score = sum/count;
        score = Math.round(score*100.0)/100.0;

        return score;

    }
    /**
     * Gets cosine similarity between two texts
     * @param textA
     * @param textB
     * @return
     */
    public static double getCosineSimilarity(String textA, String textB)  {

        List<String[]> termsDocsArray = new ArrayList<String[]>();
        List<String> allTerms = new ArrayList<String>(); //to hold all terms
        List<double[]> tfidfDocsVector = new ArrayList<double[]>();
        addTokenizedText(textA, termsDocsArray, allTerms);
        addTokenizedText(textB, termsDocsArray, allTerms);
        //create tf-idf scores
        calculateTFIDF(termsDocsArray, allTerms, tfidfDocsVector);
        double score = cosineSimilarity(tfidfDocsVector.get(0), tfidfDocsVector.get(1));

        return score;

    }

    private static void addTokenizedText(String text, List<String[]> termsDocsArray, List<String> allTerms) {
        String[] tokenizedTerms = tokenizeText(text);  //to get individual terms
        addToAllTerms(tokenizedTerms, allTerms);
        termsDocsArray.add(tokenizedTerms);

    }

    private static void addToAllTerms(String[] tokenizedTerms, List<String> all) {
        for (String term : tokenizedTerms) {
            if (!all.contains(term)) {  //avoid duplicate entry
                all.add(term);
            }
        }
    }

    private static String[] tokenizeText(String text) {
        return text.replaceAll("[\\W&&[^\\s]]", "").split("\\W+");
    }

    /**
     * Method to create termVector according to its tfidf score.
     */
    private static void calculateTFIDF(List<String[]> termsDocsArray, List<String> allTerms, List<double[]> tfidfDocsVector) {
        double tf; //term frequency
        double idf; //inverse document frequency
        double tfidf; //term requency inverse document frequency
        for (String[] docTermsArray : termsDocsArray) {
            double[] tfidfvectors = new double[allTerms.size()];
            int count = 0;
            for (String terms : allTerms) {
                tf =  TfIdf.tfCalculator(docTermsArray, terms);
                idf = TfIdf.idfCalculator(termsDocsArray, terms);
                tfidf = tf * idf;
                tfidfvectors[count] = tfidf;
                count++;
            }
            tfidfDocsVector.add(tfidfvectors);  //storing document vectors;
        }
    }
    /**
     * Method to calculate cosine similarity between two documents.
     * @param docVector1 : document vector 1 (a)
     * @param docVector2 : document vector 2 (b)
     * @return
     */
    private static double cosineSimilarity(double[] docVector1, double[] docVector2) {
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;
        double cosineSimilarity = 0.0;

        for (int i = 0; i < docVector1.length; i++) //docVector1 and docVector2 must be of same length
        {
            dotProduct += docVector1[i] * docVector2[i];  //a.b
            magnitude1 += Math.pow(docVector1[i], 2);  //(a^2)
            magnitude2 += Math.pow(docVector2[i], 2); //(b^2)
        }

        magnitude1 = Math.sqrt(magnitude1);//sqrt(a^2)
        magnitude2 = Math.sqrt(magnitude2);//sqrt(b^2)

        if (magnitude1 != 0.0 | magnitude2 != 0.0) {
            cosineSimilarity = dotProduct / (magnitude1 * magnitude2);
        } else {
            return 0.0;
        }
        return cosineSimilarity;
    }
}
