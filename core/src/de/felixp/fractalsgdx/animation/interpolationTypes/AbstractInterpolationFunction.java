package de.felixp.fractalsgdx.animation.interpolationTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;

public abstract class AbstractInterpolationFunction implements InterpolationFunction {

    List<Number> defValues = new ArrayList<>();
    List<Number> controlValues = new ArrayList<>();
    List<Number> controlDerivatives = new ArrayList<>();
    NumberFactory numberFactory;
    int activeDefValueSet = 0;

    Map<String, Number> bufferedNumbers = new HashMap<>();

    boolean isReal = true;

    @Override
    public void setValues(NumberFactory numberFactory, List<Number> defValues, List<Number> controlValues, List<Number> controlDerivatives) {
        this.numberFactory = numberFactory;
        this.defValues = defValues;
        this.controlValues = controlValues;
        this.controlDerivatives = controlDerivatives;
    }

    @Override
    public void setNumberFactory(NumberFactory numberFactory) {
        if (numberFactory != null) {
            this.numberFactory = numberFactory;
            bufferedNumbers.clear();
        }
    }

    @Override
    public int getActiveDefValueSet() {
        return activeDefValueSet;
    }

    @Override
    public Map<String, Number> getDefValueDefaultsForActiveSet() {
        Map<String, Number> defValueMap = new LinkedHashMap<>();
        List<List<String>> namesForSets = toNestedList(getDefValueNames());
        List<String> names = namesForSets.isEmpty() ? new ArrayList<>() : namesForSets.get(activeDefValueSet);
        List<Number> defaults = namesForSets.isEmpty() ? new ArrayList<>() : Arrays.asList(getDefValueDefaults()[activeDefValueSet]);
        for (int i = 0 ; i < names.size() ; i++) {
            defValueMap.put(names.get(i), defaults.get(i));
        }
        return defValueMap;
    }

    private <T> List<List<T>> toNestedList(T[][] array){
        List<List<T>> list = new ArrayList<>();
        for (T[] subArr : array){
            List<T> sublist = new ArrayList<>();
            for (T val : subArr){
                sublist.add(val);
            }
            list.add(sublist);
        }
        return list;
    }

    @Override
    public String[][] getDefValueNames() {
        return new String[][]{};
    }

    @Override
    public Number[][] getDefValueDefaults() {
        return new Number[][]{};
    }

    @Override
    public int getNoCullingDrawSegments() {
        return 50;
    }

    protected Number getValueForIndex(int index){
        return controlValues.get(index);
    }

    protected Number getDerivativeForIndex(int index){
        return controlDerivatives.get(index);
    }

    public NumberFactory getNumberFactory() {
        return numberFactory;
    }

    public boolean isReal() {
        return isReal;
    }

    public boolean isImag() {
        return !isReal;
    }

    @Override
    public void setImag(boolean imag){
        isReal = !imag;
    }

    public Number getNumber(String str){
        Number num = bufferedNumbers.get(str);
        if (num == null){
            num = numberFactory.createNumber(str);
            bufferedNumbers.put(str, num);
        }
        return num;
    }
}
