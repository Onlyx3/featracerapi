package se.gu.parser.fmparser;

import se.gu.assets.FeatureType;
import se.gu.utils.Utilities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IndentedTextToTreeParser  implements Serializable {
    private static final long serialVersionUID = -886450723103812336L;
    //Created based on  https://stackoverflow.com/questions/21735468/parse-indented-text-tree-in-java

    public static List<FeatureTreeNode> parse(List<String> lines, int rootDepth) {
        List<FeatureTreeNode> roots = new ArrayList<FeatureTreeNode>();

        FeatureTreeNode prev = null;

        for (String line : lines) {
            //for the root feature, if it contains a colon e.g, ClaferMooVisualizer:ClaferWebTools, remove the last part
            if (line.contains(":")) {
                line = line.split(":")[0].trim();
            }
            if (line.trim().isEmpty())
                continue;

            int currentDepth = countWhiteSpacesAtBeginningOfLine(line);

            boolean isORGroupFeature = featureIsORGroup(line);
            FeatureType featureType = getFeatureType(line);
            FeatureTreeNode root = null;

            if (currentDepth == rootDepth) {
                root = new FeatureTreeNode(line, rootDepth);
                root.setFeatureType(featureType);
                prev = root;

                roots.add(root);
            } else {
                if (prev == null)
                    continue;// Continue if you meet with unexpected indention that has no parent;
//                if (currentDepth > prev.getDepth() + 1)
//                    continue;// not well formed feature model"Unexpected indention (children were skipped).");

                if (currentDepth > prev.getDepth()) {

                    FeatureTreeNode node = new FeatureTreeNode(isORGroupFeature ? renameFeature(line) : line.trim(), currentDepth, prev);
                    node.setFeatureType(featureType);
                    prev.addChild(node);

                    prev = node;
                } else if (currentDepth == prev.getDepth()) {
                    FeatureTreeNode node = new FeatureTreeNode(isORGroupFeature ? renameFeature(line) : line.trim(), currentDepth, prev.getParent());
                    node.setFeatureType(featureType);
                    prev.getParent().addChild(node);

                    prev = node;
                } else {
                    while (currentDepth < prev.getDepth()) prev = prev.getParent();

                    // at this point, (currentDepth == prev.Depth) = true
                    FeatureTreeNode node = new FeatureTreeNode(isORGroupFeature ? renameFeature(line) : line.trim(), currentDepth, prev.getParent()==null?root:prev.getParent());
                    node.setFeatureType(featureType);
                    prev.getParent().addChild(node);
                    prev = node;
                }
            }
        }

        // --

        return roots;
    }

    private static String renameFeature(String line) {
        String[] result = line.trim().split(" ");
        line = result.length > 1 ? result[1] : result[0];

        return line.trim();
    }

    private static boolean featureIsORGroup(String line) {
        String stripedLine = Utilities.removeLeadingSpaces(line);
        return (stripedLine.length() > 2 && stripedLine.toLowerCase().startsWith("or") && Character.isWhitespace(stripedLine.charAt(2))) ||
                (stripedLine.length() > 3 && stripedLine.toLowerCase().startsWith("xor") && Character.isWhitespace(stripedLine.charAt(3)));
    }

    private static FeatureType getFeatureType(String line) {
        String stripedLine = Utilities.removeLeadingSpaces(line);
        FeatureType featureType;
        if (stripedLine.length() > 2 && stripedLine.toLowerCase().startsWith("or") && Character.isWhitespace(stripedLine.charAt(2))) {
            featureType = FeatureType.OR;
        } else if (stripedLine.length() > 3 && stripedLine.toLowerCase().startsWith("xor") && Character.isWhitespace(stripedLine.charAt(3))) {
            featureType = FeatureType.XOR;
        } else {
            featureType = FeatureType.SINGLE;
        }
        return featureType;
    }

    public static String dump(List<FeatureTreeNode> roots) {
        StringBuilder sb = new StringBuilder();

        for (FeatureTreeNode root : roots) {
            doDump(root, sb, "");
        }

        return sb.toString();
    }

    private static int countWhiteSpacesAtBeginningOfLine(String line) {
        //check if line has tab characters and convert them to spaces
        int countTABS = 0;
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i))) {
                break;
            }
            if (line.charAt(i) == '\t') {
                countTABS++;
            }

        }

        int lengthBefore = line.length();
        lengthBefore = lengthBefore - countTABS + (countTABS * 4);
        int lengthAfter = Utilities.removeLeadingSpaces(line).length();
        return lengthBefore - lengthAfter;
    }

    private static void doDump(FeatureTreeNode treeNode, StringBuilder sb, String indent) {
        sb.append(indent + treeNode.getText());
        sb.append(System.lineSeparator());
        for (FeatureTreeNode child : treeNode.getChildren()) {
            doDump(child, sb, indent + "    ");
        }
    }
}
