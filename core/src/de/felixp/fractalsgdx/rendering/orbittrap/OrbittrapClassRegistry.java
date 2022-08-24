package de.felixp.fractalsgdx.rendering.orbittrap;

import de.felixperko.io.ClassKeyRegistry;

public class OrbittrapClassRegistry extends ClassKeyRegistry<Orbittrap> {

    @Override
    protected void initDefaultClassKeys() {

        classKeys.put(AxisOrbittrap.class, "axis");
        classKeys.put(CircleOrbittrap.class, "circle");
    }
}
