package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.RemoteRenderer;
import de.felixp.fractalsgdx.client.ClientSystem;
import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixp.fractalsgdx.ui.entries.PropertyEntryFactory;
import de.felixperko.fractals.network.ParamContainer;
import de.felixperko.fractals.system.parameters.ParameterConfiguration;
import de.felixperko.fractals.system.parameters.ParameterDefinition;

public class CollapsiblePropertyList extends CollapsibleSideMenu {

    List<AbstractPropertyEntry> propertyEntryList = new ArrayList<>();
    VisTextButton submitButton;

    List<ChangeListener> allListeners = new ArrayList<>();

    public void setParameterConfiguration(ParamContainer paramContainer, ParameterConfiguration parameterConfiguration, PropertyEntryFactory propertyEntryFactory){
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

        List<ParameterDefinition> parameterDefinitions = new ArrayList<>(parameterConfiguration.getParameters());

        if (paramContainer.getClientParameters().containsKey("calculator")) {
            List<ParameterDefinition> calculatorParameterDefinitions = parameterConfiguration.getCalculatorParameters(paramContainer.getClientParameter("calculator").getGeneral(String.class));
            parameterDefinitions.addAll(calculatorParameterDefinitions);
        }

        for (ParameterDefinition parameterDefinition : parameterDefinitions) {
            AbstractPropertyEntry entry = propertyEntryFactory.getPropertyEntry(parameterDefinition, paramContainer);
            if (entry != null) {
                entry.init();
                entry.openView(AbstractPropertyEntry.VIEW_LIST, collapsibleTable);
                addEntry(entry);
            }
        }


        submitButton = new VisTextButton("Submit");

        collapsibleTable.add(submitButton).colspan(3).row();
    }

    public void addAllListener(ChangeListener changeListener){
        this.allListeners.add(changeListener);
        for (AbstractPropertyEntry entry : propertyEntryList)
            entry.addChangeListener(changeListener);
    }

    public void removeAllListener(ChangeListener changeListener){
        this.allListeners.remove(changeListener);
        for (AbstractPropertyEntry entry : propertyEntryList)
            entry.removeChangeListener(changeListener);
    }

    public void addSubmitListener(ChangeListener submitListener){
        this.submitButton.addListener(submitListener);
    }

    public void removeSubmitListener(ChangeListener submitListener){
        this.submitButton.removeListener(submitListener);
    }

    public void addEntry(AbstractPropertyEntry entry){
        propertyEntryList.add(entry);
    }

    public List<AbstractPropertyEntry> getPropertyEntries() {
        return  propertyEntryList;
    }

    public AbstractPropertyEntry getPropertyEntry(String paramName){
        for (AbstractPropertyEntry entry : propertyEntryList)
            if (entry.getPropertyName().equals(paramName))
                return entry;
        return null;
    }
}
