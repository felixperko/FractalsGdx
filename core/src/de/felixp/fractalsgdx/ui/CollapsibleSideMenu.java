package de.felixp.fractalsgdx.ui;

import static com.badlogic.gdx.utils.Align.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class CollapsibleSideMenu {

    protected VisTable collapsibleTable;
    CollapsibleWidget collapsibleWidget;
    VisTextButton collapseButton;
    int align = -1;

    public CollapsibleSideMenu(){

        collapsibleTable = new VisTable();
        collapsibleTable.add().expand(false, true).fill(false, true);
        collapsibleTable.row();

        collapsibleWidget = new CollapsibleWidget(collapsibleTable, true);

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

    public VisTable getCollapsibleTable(){
        return collapsibleTable;
    }
}
