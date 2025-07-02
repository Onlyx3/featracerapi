package se.gu.metrics.text;

import java.util.List;

public class TfIdf {


    /**
     * Calculated the tf of term termToCheck
     * @param totalterms : Array of all the words under processing document
     * @param termToCheck : term of which tf is to be calculated.
     * @return tf(term frequency) of term termToCheck
     */
    public static double tfCalculator(String[] totalterms, String termToCheck) {
        double count = 0;  //to count the overall occurrence of the term termToCheck
        for (String s : totalterms) {
            if (s.equalsIgnoreCase(termToCheck)) {
                count++;
            }
        }
        return count / totalterms.length;
    }



    /**
     * Calculated idf of term termToCheck
     * @param documentVectors : all the terms of all the documents
     * @param termToCheck
     * @return idf(inverse document frequency) score
     */
    public static double idfCalculator(List<String[]> documentVectors, String termToCheck) {
        double count = 0;//how many documentVectors a term appears in
        for (String[] ss : documentVectors) {
            for (String s : ss) {
                if (s.equalsIgnoreCase(termToCheck)) {
                    count++;
                    break;//on meeting first occurece of a term in a document
                }
            }
        }
        double result = documentVectors.size() / count;
        return 1+Math.log(result);
    }

}