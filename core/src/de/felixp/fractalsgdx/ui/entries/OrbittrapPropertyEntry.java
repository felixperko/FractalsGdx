package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;

import de.felixp.fractalsgdx.rendering.orbittrap.AxisOrbittrap;
import de.felixp.fractalsgdx.rendering.orbittrap.CircleOrbittrap;
import de.felixp.fractalsgdx.rendering.orbittrap.Orbittrap;
import de.felixp.fractalsgdx.rendering.orbittrap.OrbittrapContainer;
import de.felixp.fractalsgdx.ui.propertyattribute.BooleanPropertyAttributeAdapterUI;
import de.felixp.fractalsgdx.ui.propertyattribute.ComplexNumberPropertyAttributeAdapterUI;
import de.felixp.fractalsgdx.ui.propertyattribute.NumberPropertyAttributeAdapterUI;
import de.felixp.fractalsgdx.ui.propertyattribute.PropertyAttributeAdapterUI;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.attributes.ParamAttribute;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

public class OrbittrapPropertyEntry extends WindowPropertyEntry {

    public OrbittrapPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, submitValue);
    }

    @Override
    public void openWindow(Stage stage) {
        VisWindow window = new VisWindow("Edit orbit traps");

        ParamSupplier contSupp = paramContainer.getClientParameter(propertyName);
        OrbittrapContainer cont = contSupp.getGeneral(OrbittrapContainer.class);
        NumberFactory nf = paramContainer.getClientParameter("numberFactory").getGeneral(NumberFactory.class);

        VisTable contentTable = new VisTable();

        populateContentTable(window, cont, nf, contentTable);
        window.add(contentTable).row();

        VisTextButton addBtn = new VisTextButton("add orbit trap");
        addBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openAddWindow(window, contentTable, cont, nf);
            }
        });
        window.add(addBtn).row();

        VisTable btnTable = new VisTable();
        VisTextButton okButton = new VisTextButton("ok");
        btnTable.add(okButton);
        window.add(btnTable);

        window.addCloseButton();
        stage.addActor(window);
        window.pack();
        window.centerWindow();
    }

    public void populateContentTable(VisWindow window, OrbittrapContainer cont, NumberFactory nf, VisTable contentTable) {
        contentTable.clear();
        int counter = 1;
        for (Orbittrap trap : cont.getOrbittraps()){
            addOrbittrapUI(window, contentTable, nf, counter, trap, true);
            counter++;
        }
        window.pack();
        window.centerWindow();
    }

    public void addOrbittrapUI(VisWindow window, Table contentTable, NumberFactory nf, int counter, Orbittrap trap, boolean addControls){

        VisTable valueTable = prepareOrbittrapTables(window, contentTable, counter, trap, addControls);

        for(ParamAttribute attr : trap.getParamAttributes()){
            PropertyAttributeAdapterUI adapter = getAdapterUI(attr, nf);
            adapter.addToTable(valueTable);
        }

        if (addControls)
            valueTable.addSeparator(false).expandX().fillX();
    }

    private Orbittrap createOrbittrap(Class<? extends Orbittrap> cls, NumberFactory nf, OrbittrapContainer cont) {
        int biggestId = 0;
        for (Orbittrap trap : cont.getOrbittraps()){
            if (cls.isInstance(trap)){
                if (trap.getId() > biggestId)
                    biggestId = trap.getId();
            }
        }
        int newId = biggestId+1;
        if (cls.equals(AxisOrbittrap.class))
            return new AxisOrbittrap(newId, nf, nf.createNumber("0.0"), nf.createNumber("0.02"), nf.createNumber("0.0"));
        else if (cls.equals(CircleOrbittrap.class))
            return new CircleOrbittrap(newId, nf.createComplexNumber(0f,0f), nf.createNumber("0.1f"));
        else
            return null;
    }

    private PropertyAttributeAdapterUI getAdapterUI(ParamAttribute attr, NumberFactory nf){
        if (attr.getAttributeClass().isAssignableFrom(Number.class)){
            return new NumberPropertyAttributeAdapterUI(attr.getName(), nf, (Number) attr.getValue()){
                @Override
                public void valueChanged(Number newVal) {
                    attr.applyValue(newVal);
                }
            };
        }
        else if (attr.getAttributeClass().isAssignableFrom(ComplexNumber.class)){
            return new ComplexNumberPropertyAttributeAdapterUI(attr.getName(), nf, (ComplexNumber) attr.getValue()){
                @Override
                public void valueChanged(ComplexNumber newVal) {
                    attr.applyValue(newVal);
                }
            };
        }
        return null;
    }

    Orbittrap newTrap = null;

    public void openAddWindow(VisWindow superWindow, VisTable contentTable, OrbittrapContainer cont, NumberFactory nf){

        VisWindow window = new VisWindow("Add orbit trap");

        VisSelectBox<Class<? extends Orbittrap>> orbittrapClassSelect = new VisSelectBox<>();
        orbittrapClassSelect.setItems(AxisOrbittrap.class, CircleOrbittrap.class);
        Class<? extends Orbittrap> otClass = orbittrapClassSelect.getSelected();

        VisTable addContentTable = new VisTable(true);
        newTrap = createOrbittrap(otClass, nf, cont);
        addOrbittrapUI(null, addContentTable, nf, -1, newTrap, false);

        orbittrapClassSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                addContentTable.clear();
                newTrap = createOrbittrap(orbittrapClassSelect.getSelected(), nf, cont);
                addOrbittrapUI(null, addContentTable, nf, -1, newTrap, false);
            }
        });

        window.add(orbittrapClassSelect).row();
        window.add(addContentTable).expandX().fillX().left().row();

        VisTable buttonTable = new VisTable(true);
        VisTextButton cancelButton = new VisTextButton("cancel", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                window.remove();
            }
        });
        VisTextButton okButton = new VisTextButton("ok", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cont.addOrbittrap(newTrap);
                window.remove();
                newTrap = null;
                if (superWindow.getParent() != null)
                    populateContentTable(superWindow, cont, nf, contentTable);
            }
        });
        buttonTable.add(cancelButton);
        buttonTable.add(okButton);
        window.add(buttonTable);

        window.addCloseButton();
        superWindow.getStage().addActor(window);
        window.pack();
        window.centerWindow();

        contentTable.clear();
        populateContentTable(superWindow, cont, nf, contentTable);
    }



    public VisTable prepareOrbittrapTables(VisWindow window, Table table, int counter, Orbittrap orbittrap, boolean addControls) {
        VisTable valueTable = new VisTable(true);
        if (!addControls) {
            table.add(valueTable).left().expandX().fillX().row();
            return valueTable;
        }
        VisLabel topLbl = new VisLabel("Orbit trap #"+counter+" ("+orbittrap.getTypeName()+")");
        VisTextButton removeBtn = new VisTextButton("-");
        removeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ParamSupplier contSupp = paramContainer.getClientParameter(propertyName);
                OrbittrapContainer cont = contSupp.getGeneral(OrbittrapContainer.class);
                cont.removeOrbittrap(orbittrap);
                Stage stage = window.getStage();
                window.remove();
                openWindow(stage);

            }
        });
        VisTable topTable = new VisTable(true);
        topTable.add(topLbl).left();
        topTable.add(removeBtn);
        table.add(topTable).row();
        table.add(valueTable).left().expandX().fillX().row();
        return valueTable;
    }

    @Override
    public ParamSupplier getSupplier() {
        return paramContainer.getClientParameter(propertyName);
    }
}
