package de.felixp.fractalsgdx.ui;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public class CalcMultKeybinding extends Keybinding {

    String paramName;
    double factor;

    public CalcMultKeybinding(String description, String paramName, double factor, int... keys) {
        super(description, keys);
        this.paramName = paramName;
        this.factor = factor;
    }

    @Override
    public void apply() {
        MainStage stage = (MainStage) FractalsGdxMain.stage;
        SystemContext systemContext = stage.getFocusedRenderer().getSystemContext();
        ParamContainer paramContainer = systemContext.getParamContainer();
        Object val = paramContainer.getClientParameter(paramName).getGeneral();
        //TODO clamp CalcMultKeybinding resulting value?
        AbstractPropertyEntry propertyEntry = stage.getParamUI().serverParamsSideMenu.getPropertyEntry(paramName);
        if (val instanceof Number) {
            Number n = (Number) val;
            Number newN = systemContext.getNumberFactory().createNumber(factor);
            newN.mult(n);
            paramContainer.addClientParameter(new StaticParamSupplier(paramName, newN));
            propertyEntry.setValue(newN);
            try {
                propertyEntry.submit();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        else if (val instanceof Integer){
            Integer n = (Integer) val;
            Integer newN = (int)Math.round(n*factor);
            paramContainer.addClientParameter(new StaticParamSupplier(paramName, newN));
            propertyEntry.setValue(newN);
            try {
                propertyEntry.submit();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getDescription() {
        return super.getDescription();
    }

    public void setFactor(double factor){
        this.factor = factor;
    }

}
