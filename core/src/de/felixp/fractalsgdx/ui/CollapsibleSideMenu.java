package de.felixp.fractalsgdx.ui;

import static com.badlogic.gdx.utils.Align.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTree;

import de.felixp.fractalsgdx.FractalsGdxMain;

public class CollapsibleSideMenu {

    protected VisTree tree;
    CollapsibleWidget collapsibleWidget;
    VisTextButton collapseButton;
    int align = -1;
    VisTable collapsibleTable;

    public CollapsibleSideMenu(){

        tree = new VisTree();

        collapsibleTable = new VisTable();

        collapsibleTable.add(tree).fillY().expandY().left();

        collapsibleWidget = new CollapsibleWidget(collapsibleTable);
        collapsibleWidget.setCollapsed(true);

        collapseButton = new VisTextButton(">", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                collapsibleWidget.setCollapsed(!collapsibleWidget.isCollapsed());
//                collapsibleWidget.pack();
                collapseButton.layout();
                refreshButtonArrow();
                collapseButton.focusLost();
                ((MainStage)FractalsGdxMain.stage).resetKeyboardFocus();
            }
        });
    }

    public void addToTable(Table parentTable, int align){
        if (this.align != -1)
            throw new IllegalStateException("SideMenu has already been added to a infoTable.");
        if (align != left && align != right)
            throw new IllegalArgumentException("Align has to be left or right");

        this.align = align;

        if (align == left) {
            parentTable.add(collapseButton).align(left);
            parentTable.add(collapsibleWidget).align(left).expandY();
        } else {
            parentTable.add(collapsibleWidget).align(right).expandY();
            parentTable.add(collapseButton).align(right);
        }
        refreshButtonArrow();
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

    public VisTextButton getCollapseButton() {
        return collapseButton;
    }
}
