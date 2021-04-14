package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.Validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.ParamValueField;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class PropertyEntryFactory {

    final static Logger LOG = LoggerFactory.getLogger(PropertyEntryFactory.class);

    //VisTree tree;
    Map<String, Tree.Node> categoryNodes;
    NumberFactory numberFactory;

    boolean submitValue;

    public PropertyEntryFactory(Map<String, Tree.Node> categoryNodes, NumberFactory numberFactory, boolean submitValue) {
        ///this.tree = tree;
        this.categoryNodes = categoryNodes;
        this.numberFactory = numberFactory;
        this.submitValue = submitValue;
    }

    static int ID_COUNTER_SUB_DEFINITIONS = 0;

    public AbstractPropertyEntry getPropertyEntry(ParamDefinition parameterDefinition, ParamContainer paramContainer){

        ParamConfiguration config = parameterDefinition.getConfiguration();

        List<AbstractPropertyEntry> subEntries = new ArrayList<>();

        loop:
        for (ParamValueType type : parameterDefinition.getPossibleValueTypes()) {

            for (ParamValueField field : type.getSubTypes()){
                ParamValueType subType =  field.getType();
                //ParameterDefinition subDefinition = config.getParameters().stream().filter(def -> def.getName().equalsIgnoreCase(subType.getName())).findFirst().get();
                ParamDefinition subDefinition = new ParamDefinition(field.getName(), "PLACEHOLDER", StaticParamSupplier.class);//TODO Why is a new ParameterDefinition created anyways? if necessary -> is category needed?
                subDefinition.setConfiguration(parameterDefinition.getConfiguration());
                AbstractPropertyEntry entry = createEntry(subType, paramContainer, subDefinition);
                if (entry != null) {
                    entry.init();
                    subEntries.add(entry);
                } else {
                    LOG.debug("Can't find sub-property class for parameter definition '"+subDefinition.getName()+"'");
                    continue loop;
                }
            }

            AbstractPropertyEntry entry = createEntry(type, paramContainer, parameterDefinition);
            if (entry != null) {
                entry.addSubEntries(subEntries);
                return entry;
            } else {
                continue loop;
            }
        }
        LOG.debug("Can't find property class for parameter definition '"+parameterDefinition.getName()+"'");
        return null;
    }

    private AbstractPropertyEntry createEntry(ParamValueType type, ParamContainer paramContainer, ParamDefinition parameterDefinition){
        Tree.Node node = categoryNodes.get(parameterDefinition.getCategory());
        switch (type.getName()) {
            case ("integer"):
                return new IntTextPropertyEntry(node, paramContainer, parameterDefinition, submitValue);
            case ("double"):
                if (parameterDefinition.getHintValue("ui-element[default]:field", false) != null)
                    return new DoubleTextPropertyEntry(node, paramContainer, parameterDefinition, submitValue);
                if (parameterDefinition.getHintValue("ui-element[default]:slider", false) != null)
                    return new DoubleSliderPropertyEntry(node, paramContainer, parameterDefinition, submitValue);
                return new DoubleTextPropertyEntry(node, paramContainer, parameterDefinition, submitValue);
            case ("number"):
                return new NumberTextPropertyEntry(node, paramContainer, parameterDefinition, numberFactory, Validators.FLOATS, submitValue); //TODO replace validator
            case ("string"):
                return new ExpressionTextPropertyEntry(node, paramContainer, parameterDefinition, submitValue);
            case ("complexnumber"):
                return new ComplexNumberPropertyEntry(node, paramContainer, parameterDefinition, numberFactory, submitValue);
            case ("boolean"):
                return new BooleanPropertyEntry(node, paramContainer, parameterDefinition, submitValue);
            case ("selection"):
                return new SelectionPropertyEntry(node, paramContainer, parameterDefinition, submitValue);
            case ("list"):
                return new ListPropertyEntry(node, paramContainer, parameterDefinition, this, submitValue);
            case ("BreadthFirstLayer"):
                return new BreadthFirstLayerPropertyEntry(node, paramContainer, parameterDefinition, submitValue);
            case ("BreadthFirstUpsampleLayer"):
                return new BreadthFirstUpsampleLayerPropertyEntry(node, paramContainer, parameterDefinition, submitValue);
//            case ("LayerConfiguration"):
//                return new CompositePropertyEntry(node, paramContainer, parameterDefinition){
//                    @Override
//                    public void addSubEntries(List<AbstractPropertyEntry> subEntries) {
//                        LayerConfiguration current = paramContainer.getClientParameter(propertyName).getGeneral(LayerConfiguration.class);
//                        if (current != null) {
//                            ((ListPropertyEntry) subEntries.get(0)).setContent(current.getLayers());
//                        }
//                        super.addSubEntries(subEntries);
//                    }
//
//                    @Override
//                    public ParamSupplier getSupplier() {
////                        List<BreadthFirstLayer> layers = subEntries.get(0).getSupplier().getGeneral(List.class);
////                        double simStep = subEntries.get(1).getSupplier().getGeneral(Double.class);
////                        int simCount = subEntries.get(2).getSupplier().getGeneral(Integer.class);
////                        long seed = subEntries.get(3).getSupplier().getGeneral(Long.class);
////                        return new StaticParamSupplier(getPropertyName(), new LayerConfiguration(layers, simStep, simCount, seed));
//                        return null;
//                    }
//
//                    @Override
//                    public void addChangeListener(ChangeListener changeListener) {
//                        throw new NotImplementedException();
//                    }
//
//                    @Override
//                    public void removeChangeListener(ChangeListener changeListener) {
//                        throw new NotImplementedException();
//                    }
//                };
            default:
                return null;
        }
    }
}
