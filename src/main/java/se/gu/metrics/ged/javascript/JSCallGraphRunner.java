package se.gu.metrics.ged.javascript;

import org.json.JSONObject;
import se.gu.metrics.ged.CallNode;
import se.gu.metrics.ged.FunctionCall;

import java.util.Optional;

public class JSCallGraphRunner implements Runnable {
    private JSCallGraph jsCallGraph;
    private JSONObject jsonObject;

    public JSCallGraphRunner(JSCallGraph jsCallGraph, JSONObject jsonObject) {
        this.jsCallGraph = jsCallGraph;
        this.jsonObject = jsonObject;
    }

    @Override
    public void run() {
        MyJSONObject sourceJSONObject = new MyJSONObject(jsonObject.getJSONObject("source"), jsCallGraph.getConfiguration());
        CallNode sourceCallNode = createCallNode(sourceJSONObject);
        if (sourceCallNode.getCallers().size() == 0) {
            sourceCallNode.getCallers().add(jsCallGraph.getRootNode());
            jsCallGraph.getRootNode().getCallees().add(sourceCallNode);
        }
        MyJSONObject targetJSONObject = new MyJSONObject(jsonObject.getJSONObject("target"), jsCallGraph.getConfiguration());
        CallNode targetCallNode = createCallNode(targetJSONObject);
        targetCallNode.getCallers().add(sourceCallNode);
        sourceCallNode.getCallees().add(targetCallNode);


        FunctionCall functionCall = new FunctionCall(sourceCallNode, targetCallNode);
        jsCallGraph.getFunctionCalls().add(functionCall);
        jsCallGraph.getCallNodes().add(sourceCallNode);
        jsCallGraph.getCallNodes().add(targetCallNode);
    }
    private CallNode createCallNode(MyJSONObject jsonObject) {
        Optional<CallNode> callNode = jsCallGraph.getCallNodes().parallelStream().filter(c -> c.getFullyQualifiedName().equalsIgnoreCase(jsonObject.getFullyQualifiedName())).findFirst();
        if (callNode.isPresent()) {
            return callNode.get();
        } else {
            CallNode node = new CallNode(jsonObject.getFullyQualifiedName(), jsonObject.getFile(), jsonObject.getFunctionName(), jsonObject.getLineStart(), jsonObject.getLineEnd());
            jsCallGraph.getCallNodes().add(node);
            return node;
        }
    }
}
