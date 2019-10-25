package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.MainStage;
import de.felixperko.fractals.network.ParamContainer;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public abstract class AbstractDoubleTextPropertyEntry extends AbstractPropertyEntry {

    InputValidator validator1;
    InputValidator validator2;

    String text1;
    String text2;

    protected Class<? extends ParamSupplier> selectedSupplierClass;
    int selectedIntex = 0;
    List<Class<? extends ParamSupplier>> possibleSupplierClasses;

    List<Actor> contentFields = new ArrayList<Actor>();
    List<ChangeListener> listeners = new ArrayList<>();

    public AbstractDoubleTextPropertyEntry(VisTable table, ParamContainer paramContainer, ParameterDefinition parameterDefinition, InputValidator validator1, InputValidator validator2) {
        super(table, paramContainer, parameterDefinition);

        this.validator1 = validator1;
        this.validator2 = validator2;

        this.possibleSupplierClasses = parameterDefinition.getPossibleClasses();

        ParamSupplier param = paramContainer.getClientParameter(parameterDefinition.getName());
        if (param != null && possibleSupplierClasses.contains(param.getClass()))
            this.selectedSupplierClass = param.getClass();
        else
            this.selectedSupplierClass = possibleSupplierClasses.get(0);

    }

    public abstract String getParameterValue1(StaticParamSupplier paramSupplier);
    public abstract String getParameterValue2(StaticParamSupplier paramSupplier);

    @Override
    protected void generateViews() {
        views.put(VIEW_LIST, new EntryView() {

            protected VisValidatableTextField field1;
            protected VisValidatableTextField field2;
            VisLabel label;
            VisTextButton optionButton;

            @Override
            public void drawOnTable(Table table) {

                label = new VisLabel(propertyName);

                optionButton = new VisTextButton("...");

                field1 = new VisValidatableTextField(validator1);
                field2 = new VisValidatableTextField(validator2);


                ParamSupplier paramSupplier = paramContainer.getClientParameter(propertyName);

                if (paramSupplier instanceof StaticParamSupplier) {

                    field1.setText(text1 = getParameterValue1((StaticParamSupplier) paramSupplier));
                    field2.setText(text2 = getParameterValue2((StaticParamSupplier) paramSupplier));

                    field1.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            text1 = field1.getText();
                        }
                    });
                    field2.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            text2 = field2.getText();
                        }
                    });

                    for (ChangeListener listener : listeners) {
                        field1.addListener(listener);
                        field2.addListener(listener);
                    }
                }

                contentFields.add(field1);
                contentFields.add(field2);

//                optionButton.addListener(new ChangeListener() {
//                    @Override
//                    public void changed(ChangeEvent event, Actor actor) {
//                        selectedIntex++;
//                        if (selectedIntex >= possibleSupplierClasses.size())
//                            selectedIntex = 0;
//                        optionButton.setText(selectedIntex+"");
//                        selectedSupplierClass = possibleSupplierClasses.get(selectedIntex);
//                    }
//                });

                table.add(label).padRight(3);
                table.add(optionButton).padRight(10);
                table.add(field1).fillX().row();
                table.add();
                table.add();
                table.add(field2).fillX().padBottom(2).row();


                optionButton.addListener(new ChangeListener(){
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
//                        if (event.getButton() == Input.Buttons.RIGHT){

                            PopupMenu menu = new PopupMenu();
                            MenuItem typeStaticItem = new MenuItem("Set fixed");
                            MenuItem typeViewItem = new MenuItem("Set view based");
                            MenuItem typeScreenItem = new MenuItem("Set screen based");

                            typeStaticItem.setDisabled(selectedSupplierClass == StaticParamSupplier.class);
                            typeViewItem.setDisabled(selectedSupplierClass == CoordinateBasicShiftParamSupplier.class);
                            typeScreenItem.setDisabled(true);

                            typeStaticItem.addListener(new ChangeListener() {
                                @Override
                                public void changed(ChangeEvent event, Actor actor) {
                                    selectedSupplierClass = StaticParamSupplier.class;
                                    ((MainStage)FractalsGdxMain.stage).submitServer(paramContainer);

                                    typeStaticItem.setDisabled(true);
                                    typeViewItem.setDisabled(false);
                                    typeScreenItem.setDisabled(true);
                                }
                            });
                            typeViewItem.addListener(new ChangeListener() {
                                @Override
                                public void changed(ChangeEvent event, Actor actor) {
                                    selectedSupplierClass = CoordinateBasicShiftParamSupplier.class;
                                    ((MainStage)FractalsGdxMain.stage).submitServer(paramContainer);

                                    typeStaticItem.setDisabled(false);
                                    typeViewItem.setDisabled(true);
                                    typeScreenItem.setDisabled(true);
                                }
                            });
                            typeScreenItem.setDisabled(true);

                            menu.addItem(typeStaticItem);
                            menu.addItem(typeViewItem);
                            menu.addItem(typeScreenItem);

                            menu.showMenu(optionButton.getStage(), Gdx.input.getX(), Gdx.graphics.getHeight()-Gdx.input.getY());
                        }
//                    }
                });
            }
            @Override
            public void removeFromTable() {
                field1.remove();
                field2.remove();
                label.remove();
                optionButton.remove();
            }
        });
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
        listeners.add(changeListener);
        for (Actor field : contentFields)
            field.addListener(changeListener);
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
        listeners.remove(changeListener);
        for (Actor field : contentFields)
            field.removeListener(changeListener);
    }
}
