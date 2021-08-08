package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.FocusManager;
import com.kotcrab.vis.ui.Focusable;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.actors.TraversableGroup;
import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixp.fractalsgdx.ui.entries.EntryView;
import de.felixp.fractalsgdx.ui.entries.PropertyEntryFactory;
import de.felixperko.expressions.ComputeExpressionBuilder;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.calculator.ComputeExpression;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.BFOrbitCommon;
import de.felixperko.fractals.system.systems.infra.Selection;

public class CollapsiblePropertyList extends CollapsibleSideMenu {

    protected static Logger LOG = LoggerFactory.getLogger(CollapsiblePropertyList.class);

    boolean expand_categories = false;

    List<AbstractPropertyEntry> propertyEntryList = new ArrayList<>();
    Map<AbstractPropertyEntry, EntryView> propertyEntryViews = new HashMap<>();
    Map<String, List<AbstractPropertyEntry>> propertyEntriesPerCategory = new LinkedHashMap<>();
    Map<String, List<CollapsiblePropertyListButton>> buttonsPerCategory = new HashMap<>();
    Map<String, Table> tablePerCategory = new LinkedHashMap<>();
    Map<String, Tree.Node> categoryNodes = new HashMap<>();
    VisTextButton submitButton;

    List<ChangeListener> allListeners = new ArrayList<>();

    ParamContainer paramContainer;

    TraversableGroup traversableGroup = new TraversableGroup();

    public CollapsiblePropertyList() {
        super();
        traversableGroup.setTree(tree);
    }

    Map<String, String> paramControlView = new HashMap<>();

    Map<String, Selection<?>> lastSelections = null;

    public void setParameterConfiguration(ParamContainer paramContainer, ParamConfiguration paramConfig, PropertyEntryFactory propertyEntryFactory){

        //allow force reset by first setting paramContainer null and then setting a new one
        //TODO proper reset mechanism
        if (paramContainer == null){
            this.paramContainer = null;
            return;
        }

        //need to reset property table?
        //first --> reset
        boolean reset = this.paramContainer == null;

        if (lastSelections != null){
            for (Selection<?> sel : paramConfig.getSelections().values()){
                Selection<?> old = lastSelections.get(sel.getName());
                if (old == null || !old.equals(sel)) {
                    reset = true;
                    break;
                }
            }
        }
        lastSelections = paramConfig.getSelections();

        if (!reset){
            //check PropertyEntries
            for (AbstractPropertyEntry e : propertyEntryList){
                if (!e.getParameterDefinition().equals(paramConfig.getParamDefinition(e.getPropertyName())))
                    reset = true;
                String controlView = getParamControlViewName(e.getPropertyName());
                if (controlView != null)
                    e.setCurrentControlView(controlView, true);
                if (e.isForceReset(true)) { //reset all force resets
                    reset = true;
                }
                if (!reset) {
                    ParamSupplier supp = paramContainer.getClientParameter(e.getPropertyName());
                    if (!(supp instanceof StaticParamSupplier))
                        continue;
                    if (!(e.getSupplier() instanceof StaticParamSupplier)) {
                        reset = true;
                        continue;
                    }
                    Object newVal = supp.getGeneral();
                    if (newVal != null && (e.getSupplier() == null || !newVal.equals(e.getSupplier().getGeneral())))
                        e.setValue(newVal);
                }
            }
        }
        if (!reset) {
            //check for new params
            for (ParamSupplier supp : paramContainer.getParameters()) {
                String name = supp.getName();
                ParamSupplier oldSupp = this.paramContainer.getClientParameter(name);
                if (oldSupp == null) {
                    //new param --> reset to maintain proper order
                    reset = true;
                    break;
                }
            }
        }
        if (!reset){
            //check for removed params
            for (ParamSupplier supp : this.paramContainer.getParameters()){
                String name = supp.getName();
                ParamSupplier newSupp = paramContainer.getClientParameter(name);
                if (newSupp == null){
                    //removed param --> reset to remove property entry
                    reset = true;
                    break;
                }
            }
        }

        if (reset) {
            tree.clear();
//            if (this.paramContainer != paramContainer) {
                closeEntryViews();
                propertyEntryList.clear();
                propertyEntriesPerCategory.clear();
                tablePerCategory.clear();
//            }
            if (submitButton != null)
                submitButton.remove();
        }

        this.paramContainer = paramContainer;

//        if (!reset)
//            return;


        //add calculator specific parameter definitions
        List<ParamDefinition> paramDefs = new ArrayList<>(paramConfig.getParameters());
        Map<String, ParamSupplier> newParams = paramContainer.getClientParameters();
        if (newParams.containsKey("calculator")) {
            List<ParamDefinition> calculatorParameterDefinitions = paramConfig.getCalculatorParameters(paramContainer.getClientParameter("calculator").getGeneral(String.class));
            if (calculatorParameterDefinitions != null)
                paramDefs.addAll(calculatorParameterDefinitions);
        }

        //parse expression from 'f(z)=' if set
        ComputeExpression expression = null;
        if (newParams.containsKey(BFOrbitCommon.PARAM_EXPRESSION)){
            String exprString = paramContainer.getClientParameter(BFOrbitCommon.PARAM_EXPRESSION).getGeneral(String.class);
            ComputeExpressionBuilder exprBuilder = new ComputeExpressionBuilder(exprString, "z", newParams);
            try {
                expression = exprBuilder.getComputeExpression();
            } catch (IllegalArgumentException e){
                LOG.info("couldn't parse expression "+exprString+": "+e.getMessage());
            }
            if (expression != null) {
                List<ParamSupplier> exprParams = expression.getParameterList();
                for (ParamSupplier exprParam : exprParams) {
                    if (expression.getExplicitValues().containsKey(exprParam.getName()))
                        continue;
                    boolean exists = false;
                    for (ParamDefinition paramDefinition : paramDefs) {
                        if (paramDefinition.getName().equals(exprParam.getName())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
//                if (!(paramContainer.getClientParameters().containsKey(exprParam.getName()))){
                        paramDefs.add(new ParamDefinition(exprParam.getName(), "Calculator", BFOrbitCommon.complexnumberType, StaticParamSupplier.class, CoordinateBasicShiftParamSupplier.class));
//                    LOG.debug("add ParamDefinition for "+exprParam.getName());
                        paramContainer.addClientParameter(exprParam);
                    }
                }
            }
        }

        //remove entries for old, missing parameters
        for (AbstractPropertyEntry entry : new ArrayList<>(propertyEntryList)){
            String name = entry.getPropertyName();
            boolean isStaticallyDefined = false;
            boolean isExprParam = false;
            for (ParamDefinition def : paramConfig.getParameters()){
                if (def.getName().equals(name)){
                    isStaticallyDefined = true;
                    break;
                }
            }
            if (expression != null) {
                for (ParamSupplier exprParam : expression.getParameterList()){
                    if (exprParam.getName().equals(name)){
                        isExprParam = true;
                        break;
                    }
                }
            }
            if (!isStaticallyDefined && !isExprParam){
//                LOG.debug("remove entry for "+ name);
                propertyEntriesPerCategory.get(entry.getParameterDefinition().getCategory()).remove(entry);
                propertyEntryList.remove(entry);
                Iterator<ParamDefinition> paramDefIt = paramDefs.iterator();
                while (paramDefIt.hasNext()){
                    ParamDefinition def = paramDefIt.next();
                    if (def.getName().equals(name)){
                        paramDefIt.remove();
                        break;
                    }
                }
                entry.closeView(propertyEntryViews.remove(entry));
            }
        }

        //update entries
        for (ParamDefinition paramDef : paramDefs) {
            if (!paramDef.isVisible())
                continue;
            boolean entryExists = false;
            //does entry exist?
            for (AbstractPropertyEntry entry : propertyEntryList){
                if (entry.getPropertyName().equals(paramDef.getName())){
                    //entry already exists -> update value
                    entryExists = true;
                    ParamSupplier supplier = paramContainer.getClientParameter(entry.getPropertyName());
                    if (supplier instanceof StaticParamSupplier)
                        entry.setValue(supplier.getGeneral());
                    break;
                }
            }
            //entry doesn't exist -> create new entry
            if (!entryExists) {
                AbstractPropertyEntry entry = propertyEntryFactory.getPropertyEntry(paramDef, paramContainer);
                if (entry != null) {
                    entry.init();
                    addEntry(entry);
                }
            }
        }

        if (reset) {
            for (String catName : propertyEntriesPerCategory.keySet()) {
                addNodeForCategory(catName);
            }

            for (Map.Entry<String, List<AbstractPropertyEntry>> e : propertyEntriesPerCategory.entrySet()) {
                String category = e.getKey();
                Table table = (Table) ((Tree.Node) categoryNodes.get(category).getChildren().get(0)).getActor();
                for (AbstractPropertyEntry entry : e.getValue()) {
                    entry.setTraversableGroup(traversableGroup);
                    closeEntryView(entry);
                    openEntryView(table, entry);
                }
            }
            for (Map.Entry<String, List<CollapsiblePropertyListButton>> e : buttonsPerCategory.entrySet()) {
                String category = e.getKey();
                Tree.Node catNode = categoryNodes.get(category);
                if (catNode == null)
                    catNode = addNodeForCategory(category);
                Table table = (Table) ((Tree.Node) catNode.getChildren().get(0)).getActor();
                for (CollapsiblePropertyListButton button : e.getValue()) {
                    table.add();
                    table.add();
                    table.add(button).row();
                }
            }
        }

//        final float finalMaxWidth;
//        for (Table table : tableMap.values()){
//            table.pack();
//            if (table.getWidth() > maxWidth)
//                maxWidth = table.getWidth();
//        }
//        finalMaxWidth = maxWidth;
//
//        for (Map.Entry<String, Table> e : tableMap.entrySet()) {
//            String category = e.getKey();
//            Table table = e.getValue();
//
//            if (table.getPrefWidth() != finalMaxWidth) {
//                Table newTable = new Table() {
//                    @Override
//                    public float getPrefWidth() {
//                        return finalMaxWidth;
//                    }
//                };
//
//                Tree.Node oldNode = categoryNodes.get(category);
//                boolean wasExpanded = oldNode.isExpanded();
//                oldNode.getChildren().forEach(n -> n.remove());
//
//                table = newTable;
//                Tree.Node newNode = new Tree.Node(table);
//                categoryNodes.get(category).add(newNode);
//                if (wasExpanded)
//                    categoryNodes.get(category).setExpanded(true);
//
//                for (AbstractPropertyEntry entry : propertyEntriesPerCategory.get(category)) {
//                    closeEntryView(entry);
//                    openEntryView(newTable, entry);
//                }
//
//                newTable.pack();
//            }
//        }

        if (reset) {
            submitButton = new VisTextButton("Submit");
            collapsibleTable.row();
            collapsibleTable.add(submitButton).colspan(3).row();
        }
    }

    protected void openEntryView(Table table, AbstractPropertyEntry entry) {
        EntryView view = entry.openView(entry.getPrefListView(), table);
        propertyEntryViews.put(entry, view);
    }

    protected void closeEntryViews() {
        for (AbstractPropertyEntry entry : propertyEntryList) {
            closeEntryView(entry);
        }
    }

    private void closeEntryView(AbstractPropertyEntry entry) {
        EntryView oldView = propertyEntryViews.remove(entry);
        if (oldView != null)
            entry.closeView(oldView);
    }

    public CollapsiblePropertyList addButton(CollapsiblePropertyListButton button){
        List<CollapsiblePropertyListButton> list = buttonsPerCategory.get(button.getCategory());
        if (list == null){
            list = new ArrayList<>();
            buttonsPerCategory.put(button.getCategory(), list);
        }
        list.add(button);
        return this;
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
        String storedControlViewName = getParamControlViewName(entry.getPropertyName());
        if (storedControlViewName != null)
            entry.setCurrentControlView(storedControlViewName, false);
        entry.setParentPropertyList(this);
        getPropertyCategoryList(entry).add(entry);
    }

    private List<AbstractPropertyEntry> getPropertyCategoryList(AbstractPropertyEntry entry) {
        return getPropertyCategoryList(entry.getParameterDefinition().getCategory());
    }

    private List<AbstractPropertyEntry> getPropertyCategoryList(String category){
        List<AbstractPropertyEntry> list = propertyEntriesPerCategory.get(category);
        if (list != null)
            return list;
        list = new ArrayList<>();
        propertyEntriesPerCategory.put(category, list);
        expand_categories = propertyEntriesPerCategory.size() <= 1;


        addNodeForCategory(category);

        return list;
    }

    private Tree.Node addNodeForCategory(String category) {
        VisLabel lbl = new VisLabel(category);
        Tree.Node node = new Tree.Node(lbl){};
        lbl.addListener(new ClickListener(0){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                node.setExpanded(!node.isExpanded());
            }
        });

        Tree.Node oldNode = categoryNodes.get(category);
        boolean wasExpanded = oldNode == null ? expand_categories : oldNode.isExpanded();
        if (oldNode != null)
            oldNode.remove();

        categoryNodes.put(category, node);
        node.setSelectable(true);
        node.setExpanded(wasExpanded);
        tree.add(node);

        VisTable table = new VisTable();
        tablePerCategory.put(category, table);
        Tree.Node catNode = new Tree.Node(table) {};
        node.add(catNode);
        node.setSelectable(true);
        return node;
    }

    public boolean focusFirstFocusableControl(){
        for (Table table : tablePerCategory.values()){
            for (Actor actor : table.getChildren()){
                if (actor instanceof VisTextField){
                    ((MainStage)FractalsGdxMain.stage).getFocusedRenderer().setFocused(false);
                    ((VisTextField)actor).focusField();
                    FractalsGdxMain.stage.setKeyboardFocus(actor);
                    FocusManager.switchFocus(FractalsGdxMain.stage, (Focusable)actor);
                    return true;
                }
            }
        }
        return false;
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

    public Map<String, Tree.Node> getCategoryNodes() {
        return categoryNodes;
    }

    public ParamContainer getParamContainer() {
        return paramContainer;
    }

    public String getParamControlViewName(String paramName){
        return paramControlView.get(paramName);
    }

    public void setParamControlViewName(String paramName, String controlViewName){
        this.paramControlView.put(paramName, controlViewName);
    }
}
