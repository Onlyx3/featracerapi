package se.gu.assets;

import org.apache.commons.lang3.StringUtils;
import se.gu.git.CodeChange;
import se.gu.metrics.ged.FunctionCall;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Asset implements Serializable {
    private static final long serialVersionUID = -8942708534986824809L;
    private String assetName;
    private String fullyQualifiedName;

    public String getFileRelativePath() {
        return fileRelativePath;
    }

    public void setFileRelativePath(String fileRelativePath) {
        this.fileRelativePath = fileRelativePath;
    }

    public int getNloc() {
        return nloc;
    }

    public void setNloc(int nloc) {
        this.nloc = nloc;
    }

    private int nloc;//number of lines of code comprising asset
    private String fileRelativePath;
    private AssetType assetType;
   private Asset parent;
   private List<CodeChange> codeChanges;

   public List<Integer> getFragmentLines(){
       List<Integer> lines = new ArrayList<>();
       for(int i=startLine;i<=endLine;i++){
           lines.add(i);
       }
       return lines;
   }
    public List<CodeChange> getCodeChanges() {
        return codeChanges;
    }

    public void setCodeChanges(List<CodeChange> codeChanges) {
        this.codeChanges = codeChanges;
    }

    public long getAssetId() {
        return assetId;
    }

    public void setAssetId(long assetId) {
        this.assetId = assetId;
    }

    private long assetId;

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public Asset getParent() {
        return parent;
    }

    public void setParent(Asset parent) {
        this.parent = parent;
    }

    public List<Asset> getChildren() {
        return children;
    }

    public void setChildren(List<Asset> children) {
        this.children = children;
    }

    List<Asset> children;

    public Asset() {
        children = new ArrayList<>();
        linesOfCode=new ArrayList<>();
    }
public Asset(String lineContent,int lineNumber){
        setAssetContent(lineContent);
        setLineNumber(lineNumber);
}
    public Asset(String assetName, String fullyQualifiedName, AssetType assetType) {
        this();
        this.assetName = assetName;
        this.fullyQualifiedName = fullyQualifiedName;
        this.assetType = assetType;

    }
    public Asset(String assetName, String fullyQualifiedName, AssetType assetType, Asset parent) {
        this(assetName,fullyQualifiedName, assetType);
        this.parent = parent;
    }

    /**
     * Constructor for LOC assets
     * @param lineNumber
     * @param assetName
     * @param fullyQualifiedName
     * @param assetType
     * @param parent
     */
    public Asset(int lineNumber, String assetName, String fullyQualifiedName, AssetType assetType, Asset parent) {
        this(assetName,fullyQualifiedName, assetType,parent);
        this.lineNumber = lineNumber;
    }

    /**
     * Consrtuctor for Code Fragment assets
     * @param startLine
     * @param endLine
     * @param annotationType
     * @param assetName
     * @param fullyQualifiedName
     * @param assetType
     * @param parent
     */
    public Asset(int startLine,int endLine, AnnotationType annotationType, String assetName, String fullyQualifiedName, AssetType assetType, Asset parent) {
        this(assetName,fullyQualifiedName, assetType,parent);
        this.endLine = endLine;
        this.startLine = startLine;
        this.annotationType = annotationType;
    }

    public String getAssetContent() {
        return assetContent;
    }

    public void setAssetContent(String assetContent) {
        this.assetContent = assetContent;
    }

    private String assetContent;

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    private int lineNumber;

    public List<Asset> getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(List<Asset> linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    private List<Asset> linesOfCode;

    public String dump() {
        StringBuilder sb = new StringBuilder();
        doDump(this, sb, "");
        return sb.toString();
    }
    private void doDump(Asset asset, StringBuilder sb, String indent) {
        sb.append(indent + asset.getAssetName());
        sb.append(System.lineSeparator());
        for (Asset child : asset.getChildren()) {
            doDump(child, sb, indent + "    ");
        }
    }

    public List<String> getChildrenFullyQualifiedNames(){
        List<String> names = new ArrayList<>();
        for(Asset asset:children){
            names.add(asset.fullyQualifiedName);
        }
        return names;
    }

    public List<Asset> getAncestry(){
        List<Asset> ancestors = new ArrayList<>();
        ancestors.add(this);
        if(parent!=null){
            return doGetAncestry(parent,ancestors);
        }else {
            return ancestors;
        }

    }
    private List<Asset> doGetAncestry(Asset child,List<Asset> ancestors){
        ancestors.add(child);
        if(child.getParent()!=null){
            return doGetAncestry(child.getParent(),ancestors);
        }else {
            return ancestors;
        }
    }

    public Stream<Asset> flattened() {
        return Stream.concat(
                Stream.of(this),
                children.stream().flatMap(Asset::flattened));
    }

    private int startLine,endLine;
    private AnnotationType annotationType;

    public List<Asset> getCodeFragments(){
        return children.stream().filter(a->a.getAssetType()==AssetType.FRAGMENT).collect(Collectors.toList());
    }


    /**
     * SHould be used for assets of folder Type
     * @return combined contents for all files if this asset is a folder, otherwise returns the content of the particular asset (e.g., file or line of code)
     */
    public String getCombinedContent(){
        if(assetType==AssetType.FOLDER||assetType == AssetType.CLUSTER){
            return children.stream().map(Asset::getAssetContent).collect(Collectors.joining());
        }else if (assetType==AssetType.FRAGMENT){
            return parent.getLinesOfCode().stream().filter(l->l.getLineNumber()>=startLine&&l.getLineNumber()<=endLine).map(Asset::getAssetContent).collect(Collectors.joining());
        }
        else {
            return assetContent;
        }

}

    /**
     * keeps track of all commits in which this asset has changed
     */
    private int commitCount;

    public int getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(int commitCount) {
        this.commitCount = commitCount;
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

    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(AnnotationType annotationType) {
        this.annotationType = annotationType;
    }

    private List<FunctionCall> functionCalls;

    public List<FunctionCall> getFunctionCalls() {
        return functionCalls;
    }

    public void setFunctionCalls(List<FunctionCall> functionCalls) {
        this.functionCalls = functionCalls;
    }

    private String bracketedString;

    public String getBracketedString() {
        return bracketedString;
    }

    public void setBracketedString(String bracketedString) {
        this.bracketedString = bracketedString;
    }

    @Override
    public String toString() {
        return fullyQualifiedName;
    }

    @Override
    public boolean equals(Object obj) {
        return this.getFullyQualifiedName().equalsIgnoreCase(((Asset)obj).getFullyQualifiedName());
    }
}
