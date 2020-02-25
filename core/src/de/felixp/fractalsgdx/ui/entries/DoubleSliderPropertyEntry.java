package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class DoubleSliderPropertyEntry extends AbstractPropertyEntry {

    double value;

    List<Actor> contentFields = new ArrayList<Actor>();
    List<ChangeListener> listeners = new ArrayList<>();

    public DoubleSliderPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParameterDefinition parameterDefinition) {
        super(node, paramContainer, parameterDefinition);
    }

    @Override
    protected void generateViews() {

        views.put(VIEW_LIST, new EntryView() {

            protected VisSlider slider;
            VisLabel label;

            @Override
            public void addToTable(Table table) {
                label = new VisLabel(propertyName);

                List<String> hints = parameterDefinition.getHints();

                Double minD = parameterDefinition.getHintAttributeDoubleValue("ui-element[default]:slider", "min");
                Double maxD = parameterDefinition.getHintAttributeDoubleValue("ui-element[default]:slider", "max");
                if (minD == null)
                    minD = parameterDefinition.getHintAttributeDoubleValue("ui-element:slider", "min");
                if (maxD == null)
                    maxD = parameterDefinition.getHintAttributeDoubleValue("ui-element:slider", "max");

                float min = minD != null ? (float)(double)minD : 0;
                float max = maxD != null ? (float)(double)maxD : (min < 1 ? 1 : min+1);
                float step = 0.01f;
                slider = new VisSlider(min, max, step, false);

                ParamSupplier supplier = paramContainer.getClientParameter(propertyName);
                if (supplier != null) {
                    value = (double) supplier.getGeneral(Double.class);
                    slider.setValue((float)value);
                }

                slider.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        value = slider.getValue();
                    }
                });

                for (ChangeListener listener : listeners)
                    slider.addListener(listener);
                contentFields.add(slider);

                table.add(label).left().padRight(3);
                table.add();
                table.add(slider).expandX().fillX().padBottom(2).row();
            }

            @Override
            public void removeFromTable() {
                label.remove();
                slider.remove();
                contentFields.remove(slider);
            }
        });
    }

    @Override
    public ParamSupplier getSupplier() {
        return new StaticParamSupplier(propertyName, (Double)value);
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
