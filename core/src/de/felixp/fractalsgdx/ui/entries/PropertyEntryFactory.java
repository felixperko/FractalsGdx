package de.felixp.fractalsgdx.ui.entries;

import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamValueField;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.ParameterConfiguration;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstLayer;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.LayerConfiguration;
import de.felixperko.fractals.system.task.Layer;

public class PropertyEntryFactory {

    VisTable table;
    NumberFactory numberFactory;

    public PropertyEntryFactory(VisTable table, NumberFactory numberFactory) {
        this.table = table;
        this.numberFactory = numberFactory;
    }

    static int ID_COUNTER_SUB_DEFINITIONS = 0;

    public AbstractPropertyEntry getPropertyEntry(ParameterDefinition parameterDefinition, SystemClientData systemClientData){
        if (!(systemClientData.getClientParameter(parameterDefinition.getName()) instanceof StaticParamSupplier))
            return null;

        ParameterConfiguration config = parameterDefinition.getConfiguration();

        List<AbstractPropertyEntry> subEntries = new ArrayList<>();

        loop:
        for (ParamValueType type : parameterDefinition.getPossibleValueTypes()) {

            for (ParamValueField field : type.getSubTypes()){
                ParamValueType subType =  field.getType();
                //ParameterDefinition subDefinition = config.getParameters().stream().filter(def -> def.getName().equalsIgnoreCase(subType.getName())).findFirst().get();
                ParameterDefinition subDefinition = new ParameterDefinition(field.getName(), StaticParamSupplier.class);
                subDefinition.setConfiguration(parameterDefinition.getConfiguration());
                AbstractPropertyEntry entry = getByType(subType, systemClientData, subDefinition);
                if (entry != null) {
                    entry.init();
                    subEntries.add(entry);
                } else {
                    System.out.println("Can't find sub-property class for parameter definition '"+subDefinition.getName()+"'");
                    continue loop;
                }
            }

            AbstractPropertyEntry entry = getByType(type, systemClientData, parameterDefinition);
            if (entry != null) {
                entry.addSubEntries(subEntries);
                return entry;
            } else {
                continue loop;
            }
        }
        System.out.println("Can't find property class for parameter definition '"+parameterDefinition.getName()+"'");
        return null;
    }

    private AbstractPropertyEntry getByType(ParamValueType type, SystemClientData systemClientData, ParameterDefinition parameterDefinition){
        switch (type.getName()) {
            case ("integer"):
                return new IntTextPropertyEntry(table, systemClientData, parameterDefinition);
            case ("double"):
                return new DoubleTextPropertyEntry(table, systemClientData, parameterDefinition);
            case ("number"):
                return new NumberTextPropertyEntry(table, systemClientData, parameterDefinition, numberFactory, Validators.FLOATS); //TODO replace validator
            case ("complexnumber"):
                return new ComplexNumberPropertyEntry(table, systemClientData, parameterDefinition, numberFactory);
            case ("boolean"):
                return new BooleanPropertyEntry(table, systemClientData, parameterDefinition);
            case ("selection"):
                return new SelectionPropertyEntry(table, systemClientData, parameterDefinition);
            case ("list"):
                return new ListPropertyEntry(table, systemClientData, parameterDefinition, this);
            case ("BreadthFirstLayer"):
                return new BreadthFirstLayerPropertyEntry(table, systemClientData, parameterDefinition);
            case ("BreadthFirstUpsampleLayer"):
                return new BreadthFirstUpsampleLayerPropertyEntry(table, systemClientData, parameterDefinition);
            case ("LayerConfiguration"):
                return new CompositePropertyEntry(table, systemClientData, parameterDefinition){
                    @Override
                    public void addSubEntries(List<AbstractPropertyEntry> subEntries) {
                        LayerConfiguration current = systemClientData.getClientParameter(propertyName).getGeneral(LayerConfiguration.class);
                        if (current != null) {
                            ((ListPropertyEntry) subEntries.get(0)).setContent(current.getLayers());
                        }
                        super.addSubEntries(subEntries);
                    }

                    @Override
                    protected ParamSupplier getSupplier() {
//                        List<BreadthFirstLayer> layers = subEntries.get(0).getSupplier().getGeneral(List.class);
//                        double simStep = subEntries.get(1).getSupplier().getGeneral(Double.class);
//                        int simCount = subEntries.get(2).getSupplier().getGeneral(Integer.class);
//                        long seed = subEntries.get(3).getSupplier().getGeneral(Long.class);
//                        return new StaticParamSupplier(getPropertyName(), new LayerConfiguration(layers, simStep, simCount, seed));
                        return null;
                    }
                };
            default:
                return null;
        }
    }
}
