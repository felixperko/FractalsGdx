package de.felixp.fractalsgdx.ui;

import static com.badlogic.gdx.utils.Align.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTree;

public class CollapsibleSideMenu {

    protected VisTree tree;
    CollapsibleWidget collapsibleWidget;
    VisTextButton collapseButton;
    int align = -1;
    VisTable collapsibleTable;

    public CollapsibleSideMenu(){

        tree = new VisTree();
        //tree.add().expand(false, true).fill(false, true);
        //tree.row();

        //ScrollPane scrollPane = new ScrollPane(tree);

        collapsibleTable = new VisTable();
        collapsibleTable.add(tree).fillX().expandX();

        collapsibleWidget = new CollapsibleWidget(collapsibleTable);
        collapsibleWidget.setCollapsed(true);

        collapseButton = new VisTextButton(">", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                collapsibleWidget.setCollapsed(!collapsibleWidget.isCollapsed());
                collapsibleWidget.pack();
                collapseButton.layout();
                refreshButtonArrow();
                collapseButton.focusLost();
            }
        });
    }

    public void addToTable(Table parentTable, int align){
        if (this.align != -1)
            throw new IllegalStateException("SideMenu has already been added to a table.");
        if (align != left && align != right)
            throw new IllegalArgumentException("Align has to be left or right");

        this.align = align;

        if (align == left) {
            parentTable.add(collapseButton).align(align);
            parentTable.add(collapsibleWidget).align(align).expandY();
            refreshButtonArrow();
        } else {
            parentTable.add(collapsibleWidget).align(align).expandY();
            parentTable.add(collapseButton).align(align);
            refreshButtonArrow();
        }
    }

    private void refreshButtonArrow(){
        if (align == left){
            if (collapsibleWidget.isCollapsed()){
                collapseButton.setText(">");
            } else {
                collapseButton.setText("<");
            }
        } else {
            if (collapsibleWidget.isCollapsed()){
                collapseButton.setText("<");
            } else {
                collapseButton.setText(">");
            }
        }
    }

    public VisTree getTree(){
        return tree;
    }
}
