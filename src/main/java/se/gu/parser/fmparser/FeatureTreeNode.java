package se.gu.parser.fmparser;

import se.gu.assets.FeatureType;
import se.gu.main.Configuration;
import se.gu.utils.Utilities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeatureTreeNode implements Serializable {
    private static final long serialVersionUID = 8456942789244867814L;

    public FeatureTreeNode(String text, int depth, FeatureTreeNode parent)
    {
        this(text,depth);
        setParent(parent);
        setFullyQualifiedName(String.format("%s%s%s",parent.fullyQualifiedName, Configuration.getInstance().getFeatureQualifiedNameSeparator(), text));
    }
    public FeatureTreeNode(String text, int depth, FeatureTreeNode parent,FeatureType featureType)
    {
        this(text,depth,parent);
        setFeatureType(featureType);
    }
    public FeatureTreeNode(String text, int depth) {
        setText(text);
        setDepth(depth);
        setFullyQualifiedName(text);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public FeatureTreeNode getParent() {
        return parent;
    }

    public void setParent(FeatureTreeNode parent) {
        this.parent = parent;
    }

    public List<FeatureTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<FeatureTreeNode> children) {
        this.children = children;
    }

    private String text;
    private int depth;
    private FeatureTreeNode parent;
    private List<FeatureTreeNode> children  = new ArrayList<FeatureTreeNode>();

    public FeatureType getFeatureType() {
        return featureType;
    }

    public void setFeatureType(FeatureType featureType) {
        this.featureType = featureType;
    }

    private FeatureType featureType;

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    private String fullyQualifiedName;


    public void addChild(FeatureTreeNode child)
    {
        if (child != null) children.add(child);
    }

    public Stream<FeatureTreeNode> flattened() {
        return Stream.concat(
                Stream.of(this),
                children.stream().flatMap(FeatureTreeNode::flattened));
    }

    public List<String> fullNameToList(String seperator){
        return Utilities.textToList(fullyQualifiedName,seperator);
    }


    public String getXMLModel(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("<labels xmlns=\"http://mulan.sourceforge.net/labels\">");
        stringBuilder.append(System.lineSeparator());
        List<FeatureTreeNode> rootFeatures = this.children;//exlude the root feature e.g., Clafer Moo Visuaizer and only get its childrn
        for(FeatureTreeNode feature:rootFeatures) {
            doXML(feature, stringBuilder, "");
        }
        stringBuilder.append("</labels>");

        return stringBuilder.toString();

    }
    private void doXML(FeatureTreeNode treeNode, StringBuilder sb, String indent) {
        sb.append(String.format("%s <label name=\"%s\">", indent, treeNode.getText()));
        sb.append(System.lineSeparator());
        for (FeatureTreeNode child : treeNode.getChildren()) {
            doXML(child, sb, indent + "    ");
        }
        sb.append(String.format("%s </label>",indent));
        sb.append(System.lineSeparator());
    }

    public List<FeatureTreeNode> getAncestry(){
        List<FeatureTreeNode> ancestors = new ArrayList<>();
        ancestors.add(this);
        if(parent!=null){
            return doGetAncestry(parent,ancestors);
        }else {
            return ancestors;
        }

    }
    private List<FeatureTreeNode> doGetAncestry(FeatureTreeNode child,List<FeatureTreeNode> ancestors){
        ancestors.add(child);
        if(child.getParent()!=null){
            return doGetAncestry(child.getParent(),ancestors);
        }else {
            return ancestors;
        }
    }

    public  String dump() {
        StringBuilder sb = new StringBuilder();

        for (FeatureTreeNode root :children ) {
            doDump(root, sb, "");
        }

        return sb.toString();
    }
    private  void doDump(FeatureTreeNode treeNode, StringBuilder sb, String indent) {
        sb.append(indent + treeNode.getText());
        sb.append(System.lineSeparator());
        for (FeatureTreeNode child : treeNode.getChildren()) {
            doDump(child, sb, indent + "    ");
        }
    }

    public String getNewFullyQualifiedName(String separator){
        List<String> names = getAncestry().stream().map(FeatureTreeNode::getText).collect(Collectors.toList());
        Collections.reverse(names);
      return   names.stream().collect(Collectors.joining(separator));

    }

    @Override
    public boolean equals(Object obj) {
        return ((FeatureTreeNode)obj).getFullyQualifiedName().equalsIgnoreCase(this.getFullyQualifiedName());
    }

    @Override
    public String toString() {
        return getText();
    }
}
