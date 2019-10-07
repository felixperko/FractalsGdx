package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class CollapsibleSideMenu {

    private VisTable collapsibleTable;
    CollapsibleWidget collapsibleWidget;
    VisTextButton collapseButton;

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
                if (collapsibleWidget.isCollapsed()){
                    collapseButton.setText(">");
                } else {
                    collapseButton.setText("<");
                }
            }
        });
    }

    public void addToTable(Table parentTable, int align){
        parentTable.add(collapseButton).align(align);
        parentTable.add(collapsibleWidget).align(align).expandY();
    }
}
