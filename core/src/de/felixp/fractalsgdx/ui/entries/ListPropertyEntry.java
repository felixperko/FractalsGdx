package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstLayer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

class ListPropertyEntry extends AbstractPropertyEntry {

    List<ParamValueType> types;
    List<?> content = new ArrayList<>();

    PropertyEntryFactory entryFactory;

    public ListPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, PropertyEntryFactory entryFactory, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, submitValue);
        types = parameterDefinition.getConfiguration().getListTypes(propertyName);
        this.entryFactory = entryFactory;
    }

    VisTextButton btn_add;
    VisTree tree;

    PopupMenu menu;

    @Override
    protected void generateViews() {

        views.put(VIEW_LIST, new EntryView() {


            @Override
            public void addToTable(Table table) {
                btn_add = new VisTextButton("+");
                tree = new VisTree();

                menu = new PopupMenu();
                int i = 0;
                for (ParamValueType type : types){
                    final int index = i;
                    MenuItem item = new MenuItem(type.getName(), new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            addNewElement(type);
                        }
                    });
                    menu.addItem(item);
                    i++;
                }

                btn_add.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        menu.showMenu(btn_add.getStage(), btn_add);
                    }
                });

                addContentToTree();

                table.add(btn_add).align(Align.top).row();
                table.add(tree).fillX().expandX().align(Align.topLeft).colspan(2).padBottom(2).row();

                table.add(new Separator()).colspan(2).expandX().fill().padBottom(4).row();
            }

            @Override
            public void removeFromTable() {
                btn_add.remove();
                menu.remove();
                tree.remove();
            }
        });
    }

    private void addNewElement(ParamValueType type){
        int index = menu.getRows();
        ParamDefinition parameterDefinition = new ParamDefinition("list_"+type.getName()+index, "PLACEHOLDER", StaticParamSupplier.class, type); //TODO ParameterDefinition again, see other TODO
        parameterDefinition.setConfiguration(parameterDefinition.getConfiguration());
        paramContainer.addClientParameter(new StaticParamSupplier(parameterDefinition.getName(), new BreadthFirstLayer()));

        AbstractPropertyEntry entry = entryFactory.getPropertyEntry(parameterDefinition, paramContainer);
        if (entry != null) {
            entry.init();

            VisTable headerTable = new VisTable();
            VisLabel testLabel = new VisLabel(type.getName());
            VisTextButton removeButton = new VisTextButton("-");
//                                headerTable.setFillParent(true);
            headerTable.add(testLabel);
            headerTable.add(removeButton);

            Table subTable = new VisTable();
            //TODO readd
            //entry.openView(VIEW_LIST, subTable);

//            VisLabel testLabel2 = new VisLabel("teest");

            Tree.Node node = new Tree.Node(headerTable){};
//            node.setSelectable(true);
            tree.add(node);
            Tree.Node node2 = new Tree.Node(subTable){};
//            node2.setSelectable(true);
            node.add(node2);
            node.setExpanded(true);
        }
    }


    public void setContent(List<?> content){
        this.content = content;
        if (tree != null) {
            addContentToTree();
        }
    }

    private void addContentToTree() {
        tree.clear();
        for (Object o : content){
            String className = o.getClass().getSimpleName();
            Optional<ParamValueType> type = types.stream().filter(t -> t.getName().equalsIgnoreCase(className)).findAny();
            if (type.isPresent()){
                addNewElement(type.get());
            }
        }
    }

    @Override
    public ParamSupplier getSupplier() {
        List<?> list = new ArrayList<>();
        //TODO add values to list
        return new StaticParamSupplier(propertyName, list);
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
        throw new NotImplementedException();
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
        throw new NotImplementedException();
    }

    @Override
    protected boolean checkValue(Object valueObj) {
        return false;
    }


    @Override
    protected void setCheckedValue(Object newValue) {
        throw new NotImplementedException();
    }

    @Override
    protected Object getDefaultObject() {
        return null;
    }
}
