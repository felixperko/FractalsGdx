package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

public abstract class WindowPropertyEntry extends AbstractPropertyEntry {

    public WindowPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, submitValue);
    }

    public abstract void openWindow(Stage stage);

    @Override
    protected void generateViews() {
        views.put(VIEW_LIST, new EntryView() {

            VisLabel nameLbl = new VisLabel(propertyName);
            VisTextButton windowButton = null;

            @Override
            public void addToTable(Table table) {
                table.add(nameLbl);
                if (windowButton == null){
                    windowButton = new VisTextButton("...");
                    windowButton.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            openWindow(windowButton.getStage());
                        }
                    });
                }
                table.add(windowButton);
            }

            @Override
            public void removeFromTable() {
                nameLbl.remove();
                windowButton.remove();
            }
        });
    }

    @Override
    public abstract ParamSupplier getSupplier();

    @Override
    public void addChangeListener(ChangeListener changeListener) {

    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {

    }

    @Override
    protected boolean checkValue(Object valueObj) {
        return false;
    }

    @Override
    protected void setCheckedValue(Object newValue) {
        //doesn't care about values, just provides window button
    }
}
