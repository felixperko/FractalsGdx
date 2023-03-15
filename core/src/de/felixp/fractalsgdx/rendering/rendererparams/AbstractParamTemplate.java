package de.felixp.fractalsgdx.rendering.rendererparams;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

public abstract class AbstractParamTemplate {

    public abstract ParamConfiguration getParamConfig();
    
    public ParamContainer getParamContainer() {

        List<ParamSupplier> params = new ArrayList<>();

        ParamConfiguration paramConfig = getParamConfig();
        for (ParamDefinition def : paramConfig.getParameters())
            params.add(paramConfig.getDefaultValue(null, def.getUID()));

        ParamContainer paramContainer = new ParamContainer(paramConfig);
        paramContainer.setParameters(params, true);
        return paramContainer;
    }
}
