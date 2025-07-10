package se.gu.metrics.ged;

import at.unisalzburg.dbresearch.apted.costmodel.StringUnitCostModel;
import at.unisalzburg.dbresearch.apted.distance.APTED;
import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import at.unisalzburg.dbresearch.apted.parser.BracketStringInputParser;

public class TreeEditDistance {
    public static double getTED(String sourceTree,String targetTree){
//        String targetTree = "{a{b{c{a}}}{d{c{a}}}}"; //"{Clafer{Server{Client{main.js{main.js:10}}}}}";
//        String sourceTree = "{c{a}}";//"{Clafer{Server{Client{main.js{main.js:20}}}}}";
        BracketStringInputParser inputParser = new BracketStringInputParser();
        Node<StringNodeData> t1 = inputParser.fromString(sourceTree);
        Node<StringNodeData> t2 = inputParser.fromString(targetTree);
        APTED<StringUnitCostModel,StringNodeData> apted = new APTED<>(new StringUnitCostModel());
        float result = apted.computeEditDistance(t1,t2);
        //System.out.printf("Cost is %.2f\n",result);
        return result;
    }
}
