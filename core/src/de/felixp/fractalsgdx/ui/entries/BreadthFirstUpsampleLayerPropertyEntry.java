package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.data.ArrayChunkFactory;
import de.felixperko.fractals.network.ParamContainer;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstUpsampleLayer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

class BreadthFirstUpsampleLayerPropertyEntry extends AbstractPropertyEntry {

    static int ID_COUNTER = 0;
    int id;

    public BreadthFirstUpsampleLayerPropertyEntry(VisTable table, ParamContainer paramContainer, ParameterDefinition parameterDefinition) {
        super(table, paramContainer, parameterDefinition);
        this.id = ID_COUNTER++;
    }

    VisTextField field_upsample;
    VisCheckBox field_culling;
    VisTextField field_priority_shift;
    VisTextField field_priority_multiplier;

    @Override
    protected void generateViews() {

        /*
        new ParamValueField("priority_shift", doubleType, 0d),
        new ParamValueField("priority_multiplier", doubleType, 0d),
        new ParamValueField("samples", integerType, 1));
         */

        views.put(VIEW_LIST, new EntryView() {

            List<Actor> list = new ArrayList<>();

            VisLabel lbl_upsample;
            VisLabel lbl_culling;
            VisLabel lbl_priority_shift;
            VisLabel lbl_priority_multiplier;

            @Override
            public void drawOnTable(Table table) {
                list.add(lbl_upsample = new VisLabel("upsample"));
                list.add(lbl_culling = new VisLabel("culling"));
                list.add(lbl_priority_shift = new VisLabel("priority shift"));
                list.add(lbl_priority_multiplier = new VisLabel("priority multiplier"));

                list.add(field_upsample = new VisTextField());
                list.add(field_culling = new VisCheckBox("enabled"));
                list.add(field_priority_shift = new VisTextField());
                list.add(field_priority_multiplier = new VisTextField());

                table.add(lbl_upsample);
                table.add(field_upsample).row();
                table.add(lbl_culling);
                table.add(field_culling).row();
                table.add(lbl_priority_shift);
                table.add(field_priority_shift).row();
                table.add(lbl_priority_multiplier);
                table.add(field_priority_multiplier).row();
            }

            @Override
            public void removeFromTable() {
                for (Actor a : list)
                    a.remove();
            }
        });
    }

    @Override
    public ParamSupplier getSupplier() {
        int upsample;
        boolean culling;
        double priority_shift;
        double priority_multiplier;
        try {
            upsample = Integer.parseInt(field_upsample.getText());
            culling = field_culling.isChecked();
            priority_shift = Double.parseDouble(field_priority_shift.getText());
            priority_multiplier = Double.parseDouble(field_priority_multiplier.getText());
        } catch (NumberFormatException e){
            System.out.println("NFE while getting supplier for layer "+id);
            e.printStackTrace();
            return null;
        }
        int chunkSize = paramContainer.getClientParameter("chunkFactory").getGeneral(ArrayChunkFactory.class).getChunkSize();
        return new StaticParamSupplier("layer_"+id, new BreadthFirstUpsampleLayer(upsample, chunkSize).with_priority_shift(priority_shift).with_priority_multiplier(priority_multiplier).with_culling(culling));
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
        throw new NotImplementedException();
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
        throw new NotImplementedException();
    }

}
