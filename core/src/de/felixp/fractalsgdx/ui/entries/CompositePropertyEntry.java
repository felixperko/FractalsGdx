package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;

import java.util.List;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParameterDefinition;

public abstract class CompositePropertyEntry<O> extends AbstractPropertyEntry {


    public CompositePropertyEntry(VisTable table, SystemClientData systemClientData, ParameterDefinition parameterDefinition) {
        super(table, systemClientData, parameterDefinition);
    }

    @Override
    protected void generateViews() {
        String name = getPropertyName();
        views.put(VIEW_LIST, new EntryView() {

            VisLabel label;
            VisTextButton button;

            @Override
            public void drawOnTable(Table table) {
                label = new VisLabel(name);
                button = new VisTextButton("...");
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        VisWindow window = new VisWindow("Details: "+name);
                        window.setWidth(325);
                        window.setHeight(400);
                        window.setResizable(true);
                        window.setPosition(Gdx.input.getX(), Gdx.input.getY());
                        window.align(Align.topLeft);

                        VisTable contentTable = new VisTable();
                        contentTable.align(Align.topLeft);
                        VisScrollPane scrollPane = new VisScrollPane(contentTable);

                        window.add(scrollPane).expandX().fill().align(Align.topLeft);
                        openView(VIEW_DETAILS, contentTable);
                        window.addCloseButton();
//                        window.pack();
                        button.getStage().addActor(window);
                    }
                });

                table.add(label);
                table.add(button).padBottom(2).row();
            }

            @Override
            public void removeFromTable() {
                label.remove();
                button.remove();
            }
        });

        List<AbstractPropertyEntry> subEntries = this.subEntries;

        views.put(VIEW_DETAILS, new EntryView() {

            List<AbstractPropertyEntry> entries = subEntries;

            @Override
            public void drawOnTable(Table table) {
                for (AbstractPropertyEntry entry : entries)
                    entry.openView(VIEW_LIST, table);
//                table.pack();
            }

            @Override
            public void removeFromTable() {
                for (AbstractPropertyEntry entry : entries)
                    entry.closeView(VIEW_LIST);
            }
        });
    }
}
