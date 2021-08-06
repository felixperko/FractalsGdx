package de.felixp.fractalsgdx.ui.actors;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisTree;

import java.util.ArrayList;
import java.util.List;

public class TraversableGroup {

    List<VisTraversableValidateableTextField> fields = new ArrayList<>();

    VisTree tree;

    public void setTree(VisTree tree){
        this.tree = tree;
    }

    public void addField(VisTraversableValidateableTextField field){
        fields.add(field);
        field.setTraversableGroup(this);
    }

    public void removeField(VisTraversableValidateableTextField field){
        fields.remove(field);
        field.setTraversableGroup(null);
    }

    public VisTraversableValidateableTextField getNextField(VisTraversableValidateableTextField currentField, boolean up){
        VisTraversableValidateableTextField res;
        //search in nodes first
        if (tree != null) {
            res = nextInNodes(currentField, up);
            if (res != null)
                return res;
        }
        return nextInList(currentField, up);
    }

    public VisTraversableValidateableTextField nextInNodes(VisTraversableValidateableTextField currentField, boolean up) {
        resetTempNodeVars();
        int expanded = 0;
        for (Object obj : tree.getRootNodes()){
            Tree.Node node = (Tree.Node)obj;
            if (node.isExpanded())
                expanded++;
        }
        boolean autoExpand = expanded < tree.getRootNodes().size;
        boolean autoCollapse = expanded == 1;
        VisTraversableValidateableTextField res = nextInNodesRec(currentField, tree.getRootNodes(), up, autoExpand, autoCollapse);
        //if looping from last -> first or first -> last, redo search
        if (temp_next || (up && res == null)){
            res = nextInNodesRec(currentField, tree.getRootNodes(), up, autoExpand, autoCollapse);
        }
        return res;
    }

    boolean temp_next = false;
    VisTraversableValidateableTextField temp_previousActor = null;
    Tree.Node temp_previousNode = null;
    Tree.Node temp_currentKatNode = null;

    private void resetTempNodeVars() {
        temp_next = false;
        temp_previousActor = null;
        temp_previousNode = null;
        temp_currentKatNode = null;
    }

    private VisTraversableValidateableTextField nextInNodesRec(VisTraversableValidateableTextField currentField, Array<Tree.Node> nodes, boolean up, boolean autoExpand, boolean autoCollapse) {

        for (Tree.Node node : nodes){
            Object value = node.getActor();
            if (value != null){
                if (value instanceof Group){
                    for (Actor actor : ((Group)value).getChildren()){
                        Tree.Node katNode = node.getParent();
                        if (temp_next && actor instanceof VisTraversableValidateableTextField){
                            if (autoExpand && katNode != temp_currentKatNode && !katNode.isExpanded()) {
                                if (autoCollapse)
                                    temp_currentKatNode.setExpanded(false);
                                if (autoExpand)
                                    katNode.setExpanded(true);
                            }
                            temp_next = false;
                            return (VisTraversableValidateableTextField) actor;
                        }
                        if (actor == currentField){ //found current node
                            if (up){
                                if (temp_previousNode != null && temp_previousNode != katNode && !temp_previousNode.isExpanded()) {
                                    if (autoExpand)
                                        temp_previousNode.setExpanded(true);
                                    if (autoCollapse)
                                        katNode.setExpanded(false);
                                }
                                return temp_previousActor;
                            } else {
                                temp_currentKatNode = katNode;
                                temp_next = true;
                            }
                        }
                        if (actor instanceof VisTraversableValidateableTextField) {
                            temp_previousActor = (VisTraversableValidateableTextField) actor;
                            temp_previousNode = katNode;
                        }
                    }
                }
            }
            VisTraversableValidateableTextField recursiveRes = nextInNodesRec(currentField, node.getChildren(), up, autoExpand, autoCollapse);
            if (recursiveRes != null)
                return recursiveRes;
        }
        return null;
    }

    public VisTraversableValidateableTextField nextInList(VisTraversableValidateableTextField currentField, boolean up) {
        boolean next = currentField == fields.get(fields.size()-1);
        VisTraversableValidateableTextField previous = null;
        for (VisTraversableValidateableTextField field : fields){

            if (field.isTraversalPaused()){
                continue;
            }

            if (!up && next)
                return field;
            if (!up && field == currentField)
                next = true;

            if (up) {
                if (field == currentField)
                    return previous != null ? previous : fields.get(fields.size()-1);
                previous = field;
            }
        }
        return next ? fields.get(0) : null;
    }

    public void clear() {
        fields.clear();
    }
}
