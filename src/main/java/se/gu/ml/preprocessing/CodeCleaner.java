package se.gu.ml.preprocessing;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeCleaner  implements Serializable {

    private static final long serialVersionUID = 358099122765402309L;

    public static String getCleanedContent(String content){
        return content.replaceAll("[\\W&&[^\\s]]", "");
    }
    public static String getTokenizedCleanedString(String content) {
        // ArrayList<String> finalStrings = new ArrayList<>();
        StringBuilder finalStringSB = new StringBuilder();
        Map<String, Integer> wordCount = new HashMap<>();
        Pattern p = Pattern.compile("[\\w']+");
        Matcher m = p.matcher(content);

        while ( m.find() ) {
            String nextWord = content.substring(m.start(), m.end()).trim();
            if ((!nextWord.isEmpty()) && (nextWord.length() > 2)) {
                List<String> splitword = splitStringByUpperCaseAsList(nextWord);
                if ((splitword != null) && (splitword.size() > 1)) {

                    for (String s : splitword) {
                        s = s.trim();
                        if (!(isInStopWords(s)) && (!s.isEmpty())) {
                            // finalStringSB.append(s + " ");
                            addToHashMap(s, wordCount);
                        }
                    }
                }

                else {
                    addToHashMap(nextWord, wordCount);
                }
            }
        }

        // System.out.println(finalStringSB.toString().toLowerCase().replaceAll("\\s+","
        // "));
        wordCount = sortByValue(wordCount);

        int count = 0;
        for (String string : wordCount.keySet()) {
            finalStringSB.append(string + " ");

            if(count++ > 50){
                break;
            }
        }

        return finalStringSB.toString().toLowerCase().replaceAll("\\s+", " ");
    }

    /** Split camel cased words */
    public static List<String> splitStringByUpperCaseAsList(String string) {
        List<String> results = Arrays.asList(string.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"));

        return results;
    }

    private static boolean isInStopWords(String word) {
        String stopwordsString = "LSB,RCB,RRB,RSB,LCB,LRB,PX";
        ArrayList<String> stopwords = new ArrayList<>();
        stopwords.addAll(Arrays.asList(stopwordsString.split(",")));

        return stopwords.contains(word.trim());
    }
    private static void addToHashMap(String string, Map<String, Integer> wordCount) {
        if (wordCount.containsKey(string)) {
            wordCount.put(string, wordCount.get(string) + 1);
        }

        else {
            wordCount.put(string, 1);
        }
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

}
