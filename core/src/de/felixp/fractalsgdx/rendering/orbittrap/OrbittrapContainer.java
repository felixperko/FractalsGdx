package de.felixp.fractalsgdx.rendering.orbittrap;

import java.util.List;

public class OrbittrapContainer {

    List<Orbittrap> orbittraps;

    public OrbittrapContainer(List<Orbittrap> orbittraps) {
        this.orbittraps = orbittraps;
    }

    public List<Orbittrap> getOrbittraps() {
        return orbittraps;
    }
}
