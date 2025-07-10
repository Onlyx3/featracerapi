package se.gu.metrics.ged;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CallNode implements Serializable {
    private static final long serialVersionUID = 604984239628792247L;
    private String fullyQualifiedName,fileName,functionName;
    private int startLine,endLine;
    private List<CallNode> callees;//list of all callees (methods or code fragments called by this node


    public CallNode(String fileName, String functionName, int startLine, int endLine) {
        this.fileName = fileName;
        this.functionName = functionName;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public CallNode(String fullyQualifiedName, String fileName, String functionName, int startLine, int endLine) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.fileName = fileName;
        this.functionName = functionName;
        this.startLine = startLine;
        this.endLine = endLine;
        callees = new ArrayList<>();
        callers = new ArrayList<>();
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public List<CallNode> getCallees() {
        return callees;
    }

    public void setCallees(List<CallNode> callees) {
        this.callees = callees;
    }


    public List<CallNode> getCallers() {
        return callers;
    }

    public void setCallers(List<CallNode> callers) {
        this.callers = callers;
    }

    /**
     * List of all callers to this node
     */
    private List<CallNode> callers;

    public List<Integer> getLinesInRange(){
        List<Integer> list = new ArrayList<>();
        for(int i=startLine;i<=endLine;i++){
            list.add(i);
        }
        return list;
    }

    public String getBracketedString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append(fullyQualifiedName);
        for(CallNode callee:callees){
            stringBuilder.append(String.format("{%s}",callee.getFullyQualifiedName()));
        }
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
