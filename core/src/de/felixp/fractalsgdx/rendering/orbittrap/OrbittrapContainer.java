package de.felixp.fractalsgdx.rendering.orbittrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import de.felixperko.fractals.system.parameters.attributes.ParamAttribute;
import de.felixperko.fractals.system.parameters.attributes.ParamAttributeContainer;
import de.felixperko.fractals.system.parameters.attributes.ParamAttributeHolder;

public class OrbittrapContainer implements Serializable, ParamAttributeHolder {

    List<Orbittrap> orbittraps = new ArrayList<>();
    ParamAttributeContainer paramAttributeContainer = new ParamAttributeContainer();

    public OrbittrapContainer(List<Orbittrap> orbittraps) {
        for (Orbittrap trap : orbittraps)
            addOrbittrap(trap);
    }

    public OrbittrapContainer copy(){
        List<Orbittrap> newList = new ArrayList<>();
        for (Orbittrap orbittrap : orbittraps)
            newList.add(orbittrap.copy());
        return new OrbittrapContainer(newList);
    }

    public List<Orbittrap> getOrbittraps() {
        return Collections.unmodifiableList(orbittraps);
    }

    public void addOrbittrap(Orbittrap orbittrap){
        orbittraps.add(orbittrap);
        for (ParamAttribute<?> attr : orbittrap.getParamAttributes()){
            paramAttributeContainer.addAttribute(attr);
        }
    }

    public void removeOrbittrap(Orbittrap orbittrap){
        orbittraps.remove(orbittrap);
        for (ParamAttribute<?> attr : orbittrap.getParamAttributes()){
            paramAttributeContainer.removeAttribute(attr);
        }
    }

    public boolean needsShaderRecompilation(OrbittrapContainer other){
        if (this == other)
            return false;
        if (orbittraps.size() != other.orbittraps.size())
            return true;
        for (int i = 0 ; i < orbittraps.size() ; i++){
            Orbittrap o1 = orbittraps.get(i);
            Orbittrap o2 = other.orbittraps.get(i);
            if (!o1.getName().equals(o2.getName()))
                return true;
            if (!o1.getTypeName().equals(o2.getTypeName()))
                return true;
            if (!o1.getClass().equals(o2.getClass()))
                return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrbittrapContainer that = (OrbittrapContainer) o;
        return Objects.equals(orbittraps, that.orbittraps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orbittraps);
    }

    @Override
    public ParamAttributeContainer getParamAttributeContainer() {
        return paramAttributeContainer;
    }
}
