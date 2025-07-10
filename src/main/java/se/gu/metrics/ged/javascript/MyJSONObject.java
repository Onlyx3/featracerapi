package se.gu.metrics.ged.javascript;

import org.json.JSONObject;
import se.gu.main.Configuration;

public class MyJSONObject {
    private JSONObject jsonObject;
    private Configuration configuration;

    public MyJSONObject(JSONObject jsonObject, Configuration configuration) {
        this.jsonObject = jsonObject;
        this.configuration = configuration;
    }

    public String getFile(){
       return jsonObject.getString("file");
    }
    public int getLineStart(){
      return getLine("start");
    }
    public int getLineEnd(){
        return getLine("end");
    }

    public String getFunctionName(){
        String functionName = jsonObject.getString("label");
        functionName = configuration.getCallGraphNonLabeledNodeNames().contains(functionName)?String.format("%d-%d",
                getLineStart(),getLineEnd()):functionName;
        return functionName;
    }
    public String getFullyQualifiedName(){
        return String.format("%s%s%s",getFile(),configuration.getFeatureQualifiedNameSeparator(),getFunctionName());
    }
    private int getLine(String position){
        Object row = jsonObject.getJSONObject(position).get("row");
        return row==null||configuration.getCallGraphNonLabeledLineNames().contains(row.toString())?-1:Integer.parseInt(row.toString());
    }
}
