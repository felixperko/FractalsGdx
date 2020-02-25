package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixp.fractalsgdx.ui.entries.PropertyEntryFactory;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParameterConfiguration;
import de.felixperko.fractals.system.parameters.ParameterDefinition;

public class CollapsiblePropertyList extends CollapsibleSideMenu {

    boolean expand_categories = false;

    List<AbstractPropertyEntry> propertyEntryList = new ArrayList<>();
    Map<String, List<AbstractPropertyEntry>> propertyEntryPerCategory = new HashMap<>();
    Map<String, List<CollapsiblePropertyListButton>> buttonsPerCategory = new HashMap<>();
    Map<String, Table> tablePerCategory = new HashMap<>();
    Map<String, Tree.Node> categoryNodes = new HashMap<>();
    VisTextButton submitButton;

    List<ChangeListener> allListeners = new ArrayList<>();

    public void setParameterConfiguration(ParamContainer paramContainer, ParameterConfiguration parameterConfiguration, PropertyEntryFactory propertyEntryFactory){
        for (AbstractPropertyEntry entry : propertyEntryList) {
            entry.closeView(AbstractPropertyEntry.VIEW_LIST);
        }
        propertyEntryList.clear();
        propertyEntryPerCategory.clear();

        tree.clear();

        //NumberFactory numberFactory = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

        //addEntry(new IntTextPropertyEntry(tree, systemClientData, "iterations"));
        //addEntry(new ComplexNumberPropertyEntry(tree, systemClientData, "pow", numberFactory));

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
                addEntry(entry);
            }
        }

        float maxWidth = 0;
        Map<String, Table> tableMap = new HashMap<>();

        for (Map.Entry<String, List<AbstractPropertyEntry>> e : propertyEntryPerCategory.entrySet()){
            String category = e.getKey();
            Table table = (Table)categoryNodes.get(category).getChildren().get(0).getActor();
            tableMap.put(category, table);
            for (AbstractPropertyEntry entry : e.getValue()){
                entry.openView(AbstractPropertyEntry.VIEW_LIST, table);
            }
        }
        for (Map.Entry<String, List<CollapsiblePropertyListButton>> e : buttonsPerCategory.entrySet()){
            String category = e.getKey();
            Tree.Node catNode = categoryNodes.get(category);
            if (catNode == null)
                catNode = addNodeForCatgory(category);
            Table table = (Table)catNode.getChildren().get(0).getActor();
            for (CollapsiblePropertyListButton button : e.getValue()){
                table.add();
                table.add();
                table.add(button).row();
            }
        }

        final float finalMaxWidth;
        for (Table table : tableMap.values()){
            table.pack();
            if (table.getWidth() > maxWidth)
                maxWidth = table.getWidth();
        }
        finalMaxWidth = maxWidth;

        for (Map.Entry<String, Table> e : tableMap.entrySet()) {
            String category = e.getKey();
            Table table = e.getValue();

            if (table.getPrefWidth() != finalMaxWidth) {
                Table newTable = new Table() {
                    @Override
                    public float getPrefWidth() {
                        return finalMaxWidth;
                    }
                };

                Tree.Node oldNode = categoryNodes.get(category);
                boolean wasExpanded = oldNode.isExpanded();
                oldNode.getChildren().forEach(n -> n.remove());

                table = newTable;
                Tree.Node newNode = new Tree.Node(table);
                categoryNodes.get(category).add(newNode);
                if (wasExpanded)
                    categoryNodes.get(category).setExpanded(true);

                for (AbstractPropertyEntry entry : propertyEntryPerCategory.get(category)) {
                    entry.openView(AbstractPropertyEntry.VIEW_LIST, newTable);
                }

                table.pack();
            }
        }

        submitButton = new VisTextButton("Submit");
        collapsibleTable.row();
        collapsibleTable.add(submitButton).colspan(3).row();
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
        getPropertyCategoryList(entry).add(entry);
    }

    private List<AbstractPropertyEntry> getPropertyCategoryList(AbstractPropertyEntry entry) {
        return getPropertyCategoryList(entry.getParameterDefinition().getCategory());
    }

    private List<AbstractPropertyEntry> getPropertyCategoryList(String category){
        List<AbstractPropertyEntry> list = propertyEntryPerCategory.get(category);
        if (list != null)
            return list;
        list = new ArrayList<>();
        propertyEntryPerCategory.put(category, list);
        expand_categories = propertyEntryPerCategory.size() <= 1;


        addNodeForCatgory(category);

        return list;
    }

    private Tree.Node addNodeForCatgory(String category) {
        VisLabel lbl = new VisLabel(category);
        Tree.Node node = new Tree.Node(lbl);
        lbl.addListener(new ClickListener(0){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                node.setExpanded(!node.isExpanded());
            }
        });

        Tree.Node oldNode = categoryNodes.get(category);
        boolean wasExpanded = oldNode == null ? expand_categories : oldNode.isExpanded();

        categoryNodes.put(category, node);
        node.setSelectable(true);
        node.setExpanded(wasExpanded);
        tree.add(node);

        VisTable table = new VisTable();
        node.add(new Tree.Node(table));
        node.setSelectable(true);
        return node;
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
}
