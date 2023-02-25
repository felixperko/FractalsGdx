package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.Validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.rendering.ShaderSystemContext;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.ParamValueField;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.util.UIDGenerator;

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

    public AbstractPropertyEntry getPropertyEntry(ParamDefinition paramDef, ParamContainer paramContainer){

        ParamConfiguration config = paramDef.getConfiguration();

        List<AbstractPropertyEntry> subEntries = new ArrayList<>();

//        loop:
//        for (ParamValueType type : paramDef.getPossibleValueTypes()) {
        ParamValueType type = paramDef.getValueType();

            for (ParamValueField field : type.getSubTypes()){
                ParamValueType subType = field.getType();
                ParamDefinition subDefinition = new ParamDefinition(UIDGenerator.fromRandomBytes(6), field.getName(), "PLACEHOLDER", StaticParamSupplier.class, type, 1.0);//TODO Why is a new ParameterDefinition created anyways? if necessary -> is category needed?
                subDefinition.setConfiguration(paramDef.getConfiguration());
                AbstractPropertyEntry entry = createEntry(subType, paramContainer, subDefinition);
                if (entry != null) {
                    entry.init();
                    subEntries.add(entry);
                } else {
                    LOG.debug("Can't find sub-property class for parameter definition '"+subDefinition.getName()+"'");
//                    continue loop;
                    return null;
                }
            }

            AbstractPropertyEntry entry = createEntry(type, paramContainer, paramDef);
            if (entry != null) {
                entry.addSubEntries(subEntries);
                return entry;
            } else {
//                continue loop;
                return null;
            }
//        }
//        LOG.debug("Can't find property class for parameter definition '"+paramDef.getName()+"'");
//        return null;
    }

    private AbstractPropertyEntry createEntry(ParamValueType type, ParamContainer paramCont, ParamDefinition paramDef){
        Tree.Node node = categoryNodes.get(paramDef.getCategory());
        //TODO remove support for old types
        switch (type.getName()) {
            case ("integer"):
                return new IntTextPropertyEntry(node, paramCont, paramDef, submitValue);
            case ("double"):
                if (paramDef.getHintValue("ui-element[default]:field", false) != null)
                    return new DoubleTextPropertyEntry(node, paramCont, paramDef, submitValue);
                if (paramDef.getHintValue("ui-element[default]:slider", false) != null)
                    return new DoubleSliderPropertyEntry(node, paramCont, paramDef, submitValue);
                return new DoubleTextPropertyEntry(node, paramCont, paramDef, submitValue);
            case ("number"):
                return new NumberTextPropertyEntry(node, paramCont, paramDef, numberFactory, Validators.FLOATS, submitValue); //TODO replace validator
            case ("expressions"):
                return new ExpressionsPropertyEntry(node, paramCont, paramDef, submitValue);
            case ("complexnumber"):
                return new ComplexNumberPropertyEntry(node, paramCont, paramDef, numberFactory, submitValue);
            case ("boolean"):
                return new BooleanPropertyEntry(node, paramCont, paramDef, submitValue);
            case ("string"):
                return new StringPropertyEntry(node, paramCont, paramDef, submitValue);
            case ("selection"):
                return new SelectionPropertyEntry(node, paramCont, paramDef, submitValue);
            case ("list"):
                return new ListPropertyEntry(node, paramCont, paramDef, this, submitValue);
            case ("BreadthFirstLayer"):
                return new BreadthFirstLayerPropertyEntry(node, paramCont, paramDef, submitValue);
            case ("BreadthFirstUpsampleLayer"):
                return new BreadthFirstUpsampleLayerPropertyEntry(node, paramCont, paramDef, submitValue);
            case (ShaderSystemContext.PARAMNAME_ORBITTRAPS):
                return new OrbittrapPropertyEntry(node, paramCont, paramDef, submitValue);
            case ("color"):
                return new ColorPropertyEntry(node, paramCont, paramDef, submitValue);
//            case ("LayerConfiguration"):
//                return new CompositePropertyEntry(node, paramCont, paramDef){
//                    @Override
//                    public void addSubEntries(List<AbstractPropertyEntry> subEntries) {
//                        LayerConfiguration current = paramCont.getParam(propertyName).getGeneral(LayerConfiguration.class);
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
