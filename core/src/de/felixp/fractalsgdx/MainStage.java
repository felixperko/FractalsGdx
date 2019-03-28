package de.felixp.fractalsgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixp.fractalsgdx.ui.entries.ComplexNumberPropertyEntry;
import de.felixp.fractalsgdx.ui.entries.IntPropertyEntry;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.Numbers.DoubleComplexNumber;
import de.felixperko.fractals.system.Numbers.DoubleNumber;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;

public class MainStage extends Stage {

    private VisTable collapsibleTable;

    AbstractRenderer renderer;

    CollapsibleWidget collapsibleWidget;
    VisTextButton collapseButton;

    VisTextButton submitButton;

    List<AbstractPropertyEntry> propertyEntryList = new ArrayList<>();

    public MainStage(Viewport viewport, Batch batch){
        super(viewport, batch);
    }

    public void create(){
        renderer = new RemoteRenderer();
        renderer.init();


        VisTable ui = new VisTable();
        ui.setFillParent(true);
        addActor(ui);
//        ui.add().expand().fill();
        collapsibleTable = new VisTable();
        collapsibleTable.add().expand(false, true).fill(false, true);
        collapsibleWidget = new CollapsibleWidget(collapsibleTable, true);

        collapseButton = new VisTextButton(">", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                collapsibleWidget.setCollapsed(!collapsibleWidget.isCollapsed());
                collapsibleWidget.pack();
                if (collapsibleWidget.isCollapsed()){
                    collapseButton.setPosition(0, Gdx.graphics.getHeight()/2, Align.left);
                    collapseButton.setText(">");
                } else {
                    collapseButton.setPosition(collapsibleWidget.getWidth(), Gdx.graphics.getHeight()/2, Align.left);
                    collapseButton.setText("<");
//                    collapsibleWidget.setPosition(0, (Gdx.graphics.getHeight())*0.5f, Align.left);
                }
            }
        });
        ui.add(collapsibleWidget).align(Align.left);
        ui.add(collapseButton);

        collapsibleWidget.setPosition(0, Gdx.graphics.getHeight()/2, Align.left);
        collapseButton.setPosition(0, Gdx.graphics.getHeight()/2, Align.left);


//        collapsibleTable.align(Align.left);

        collapsibleTable.row();
//        collapsibleTable.add(new VisTextArea("test1")).row();
//        collapsibleTable.add(new VisTextArea("test2")).row();
//        collapsibleTable.add(new VisTextArea("test3")).row();
//        collapsibleTable.add(new VisTextArea("test4")).row();
//        collapsibleTable.add(new VisTextArea("test5")).row();
//        new IntPropertyEntry(collapsibleTable, systemClientData, "test1");
//        new IntPropertyEntry(collapsibleTable, systemClientData, "test2");
//        new IntPropertyEntry(collapsibleTable, systemClientData, "test3");
//        new IntPropertyEntry(collapsibleTable, systemClientData, "test4");
//        new IntPropertyEntry(collapsibleTable, systemClientData, "test5");

        addActor(renderer);
//        addActor(ui);
        addActor(collapsibleWidget);
        addActor(collapseButton);
    }

    public void addEntry(AbstractPropertyEntry entry){
        propertyEntryList.add(entry);
    }

    public void setSystemClientData(SystemClientData systemClientData){
        for (AbstractPropertyEntry entry : propertyEntryList) {
            entry.dispose();
        }

        if (submitButton != null)
            submitButton.remove();
        else {
            submitButton = new VisTextButton("Submit", new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    for (AbstractPropertyEntry entry : propertyEntryList){
                        entry.applyValue();
                    }

                    FractalsGdxMain.client.updateConfiguration();
                    renderer.reset();
                }
            });
        }

        NumberFactory numberFactory = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

        addEntry(new IntPropertyEntry(collapsibleTable, systemClientData, "iterations"));
        addEntry(new ComplexNumberPropertyEntry(collapsibleTable, systemClientData, "pow", numberFactory));

        collapsibleTable.add();
        collapsibleTable.add(submitButton).row();
    }

    public void resize(int width, int height){

    }

    @Override
    public void dispose(){
        super.dispose();
    }

    public RemoteRenderer getRenderer() {
        return (RemoteRenderer)renderer;//TODO replace
    }
}
