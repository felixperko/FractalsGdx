package de.felixp.fractalsgdx.rendering;

import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

public interface ParamListener {

    default String getUid() {
        return null;
    }

    void changed(ParamSupplier newSupp, ParamSupplier oldSupp);
}
