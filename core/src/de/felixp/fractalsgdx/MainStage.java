package de.felixp.fractalsgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixp.fractalsgdx.ui.entries.PropertyEntryFactory;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.Numbers.DoubleComplexNumber;
import de.felixperko.fractals.system.Numbers.DoubleNumber;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.ParameterConfiguration;
import de.felixperko.fractals.system.parameters.ParameterDefinition;

public class MainStage extends Stage {

    private VisTable collapsibleTable;

    AbstractRenderer renderer;

    CollapsibleWidget collapsibleWidget;
    VisTextButton collapseButton;

    VisTextButton submitButton;

    PropertyEntryFactory propertyEntryFactory;
    List<AbstractPropertyEntry> propertyEntryList = new ArrayList<>();

    public MainStage(Viewport viewport, Batch batch){
        super(viewport, batch);
    }

    public void create(){

        Gdx.graphics.setContinuousRendering(false);

        renderer = new RemoteRenderer();
        renderer.init();


        VisTable ui = new VisTable();
        ui.align(Align.topLeft);
        ui.setFillParent(true);
//        addActor(ui);

        collapsibleTable = new VisTable();

        propertyEntryFactory = new PropertyEntryFactory(collapsibleTable, new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class));//TODO dynamic number factory

        //Topline
        VisTable topButtons = new VisTable();
        //topButtons.align(Align.top);

        VisTextButton connect = new VisTextButton("Connect to Server");
        VisTextButton screenshot = new VisTextButton("Screenshot");
        screenshot.addListener(new ClickListener(0){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                renderer.setScreenshot(true);
                super.clicked(event, x, y);
            }
        });

        topButtons.add(connect).pad(1);
        topButtons.add(screenshot).pad(1);

        //collapsible left
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

        //add
        ui.add();
        ui.add(topButtons).align(Align.top).expandX().row();
        ui.add(collapsibleWidget).align(Align.left).expandY();
//        ui.add(collapseButton).align(Align.left);

//        collapsibleWidget.setPosition(0, Gdx.graphics.getHeight()/2, Align.left);
//        collapseButton.setPosition(0, Gdx.graphics.getHeight()/2, Align.left);
        collapsibleTable.row();

        addActor(renderer);
//        topButtons.align(Align.top);
//        addActor(topButtons);
        addActor(ui);
//        addActor(collapsibleWidget);
        collapseButton.align(Align.topLeft);
        addActor(collapseButton);
    }

    public void addEntry(AbstractPropertyEntry entry){
        propertyEntryList.add(entry);
    }

    public void setParameterConfiguration(SystemClientData systemClientData, ParameterConfiguration parameterConfiguration){
        for (AbstractPropertyEntry entry : propertyEntryList) {
            entry.closeView(AbstractPropertyEntry.VIEW_LIST);
        }
        propertyEntryList.clear();

        collapsibleTable.clear();

        //NumberFactory numberFactory = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

        //addEntry(new IntTextPropertyEntry(collapsibleTable, systemClientData, "iterations"));
        //addEntry(new ComplexNumberPropertyEntry(collapsibleTable, systemClientData, "pow", numberFactory));

        if (submitButton != null)
            submitButton.remove();

        for (ParameterDefinition parameterDefinition : parameterConfiguration.getParameters()) {
            AbstractPropertyEntry entry = propertyEntryFactory.getPropertyEntry(parameterDefinition, systemClientData);
            if (entry != null) {
                entry.init();
                entry.openView(AbstractPropertyEntry.VIEW_LIST, collapsibleTable);
                addEntry(entry);
            }
        }

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

        collapsibleTable.add();
        collapsibleTable.add(submitButton).row();

        renderer.setRefresh();
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
