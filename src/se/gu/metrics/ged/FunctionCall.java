package se.gu.metrics.ged;

import java.io.Serializable;

public class FunctionCall implements Serializable {
    private CallNode sourceNode,targetNode;

    public CallNode getSourceNode() {
        return sourceNode;
    }

    public void setSourceNode(CallNode sourceNode) {
        this.sourceNode = sourceNode;
    }

    public CallNode getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(CallNode targetNode) {
        this.targetNode = targetNode;
    }

    public FunctionCall(CallNode sourceNode, CallNode targetNode) {
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
    }

    @Override
    public String toString() {
        return String.format("%s -> %s",sourceNode.getFullyQualifiedName(),targetNode.getFullyQualifiedName());
    }
}
