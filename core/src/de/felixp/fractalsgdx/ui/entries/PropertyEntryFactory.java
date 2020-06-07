package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.LayerConfiguration;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamValueField;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.ParameterConfiguration;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class PropertyEntryFactory {

    //VisTree tree;
    Map<String, Tree.Node> categoryNodes;
    NumberFactory numberFactory;

    public PropertyEntryFactory(Map<String, Tree.Node> categoryNodes, NumberFactory numberFactory) {
        ///this.tree = tree;
        this.categoryNodes = categoryNodes;
        this.numberFactory = numberFactory;
    }

    static int ID_COUNTER_SUB_DEFINITIONS = 0;

    public AbstractPropertyEntry getPropertyEntry(ParameterDefinition parameterDefinition, ParamContainer paramContainer){

        ParameterConfiguration config = parameterDefinition.getConfiguration();

        List<AbstractPropertyEntry> subEntries = new ArrayList<>();

        loop:
        for (ParamValueType type : parameterDefinition.getPossibleValueTypes()) {

            for (ParamValueField field : type.getSubTypes()){
                ParamValueType subType =  field.getType();
                //ParameterDefinition subDefinition = config.getParameters().stream().filter(def -> def.getName().equalsIgnoreCase(subType.getName())).findFirst().get();
                ParameterDefinition subDefinition = new ParameterDefinition(field.getName(), "PLACEHOLDER", StaticParamSupplier.class);//TODO Why is a new ParameterDefinition created anyways? if necessary -> is category needed?
                subDefinition.setConfiguration(parameterDefinition.getConfiguration());
                AbstractPropertyEntry entry = getByType(subType, paramContainer, subDefinition);
                if (entry != null) {
                    entry.init();
                    subEntries.add(entry);
                } else {
                    System.out.println("Can't find sub-property class for parameter definition '"+subDefinition.getName()+"'");
                    continue loop;
                }
            }

            AbstractPropertyEntry entry = getByType(type, paramContainer, parameterDefinition);
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

    private AbstractPropertyEntry getByType(ParamValueType type, ParamContainer paramContainer, ParameterDefinition parameterDefinition){
        Tree.Node node = categoryNodes.get(parameterDefinition.getCategory());
        switch (type.getName()) {
            case ("integer"):
                return new IntTextPropertyEntry(node, paramContainer, parameterDefinition);
            case ("double"):
                if (parameterDefinition.getHintValue("ui-element[default]:field", false) != null)
                    return new DoubleTextPropertyEntry(node, paramContainer, parameterDefinition);
                if (parameterDefinition.getHintValue("ui-element[default]:slider", false) != null)
                    return new DoubleSliderPropertyEntry(node, paramContainer, parameterDefinition);
                return new DoubleTextPropertyEntry(node, paramContainer, parameterDefinition);
            case ("number"):
                return new NumberTextPropertyEntry(node, paramContainer, parameterDefinition, numberFactory, Validators.FLOATS); //TODO replace validator
            case ("string"):
                return new StringTextPropertyEntry(node, paramContainer, parameterDefinition);
            case ("complexnumber"):
                return new ComplexNumberPropertyEntry(node, paramContainer, parameterDefinition, numberFactory);
            case ("boolean"):
                return new BooleanPropertyEntry(node, paramContainer, parameterDefinition);
            case ("selection"):
                return new SelectionPropertyEntry(node, paramContainer, parameterDefinition);
            case ("list"):
                return new ListPropertyEntry(node, paramContainer, parameterDefinition, this);
            case ("BreadthFirstLayer"):
                return new BreadthFirstLayerPropertyEntry(node, paramContainer, parameterDefinition);
            case ("BreadthFirstUpsampleLayer"):
                return new BreadthFirstUpsampleLayerPropertyEntry(node, paramContainer, parameterDefinition);
            case ("LayerConfiguration"):
                return new CompositePropertyEntry(node, paramContainer, parameterDefinition){
                    @Override
                    public void addSubEntries(List<AbstractPropertyEntry> subEntries) {
                        LayerConfiguration current = paramContainer.getClientParameter(propertyName).getGeneral(LayerConfiguration.class);
                        if (current != null) {
                            ((ListPropertyEntry) subEntries.get(0)).setContent(current.getLayers());
                        }
                        super.addSubEntries(subEntries);
                    }

                    @Override
                    public ParamSupplier getSupplier() {
//                        List<BreadthFirstLayer> layers = subEntries.get(0).getSupplier().getGeneral(List.class);
//                        double simStep = subEntries.get(1).getSupplier().getGeneral(Double.class);
//                        int simCount = subEntries.get(2).getSupplier().getGeneral(Integer.class);
//                        long seed = subEntries.get(3).getSupplier().getGeneral(Long.class);
//                        return new StaticParamSupplier(getPropertyName(), new LayerConfiguration(layers, simStep, simCount, seed));
                        return null;
                    }

                    @Override
                    public void addChangeListener(ChangeListener changeListener) {
                        throw new NotImplementedException();
                    }

                    @Override
                    public void removeChangeListener(ChangeListener changeListener) {
                        throw new NotImplementedException();
                    }
                };
            default:
                return null;
        }
    }
}
