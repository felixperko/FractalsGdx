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
