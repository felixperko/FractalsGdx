package de.felixp.fractalsgdx.rendering.palette;

import com.badlogic.gdx.graphics.Color;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.io.deserializers.AbstractParamXMLDeserializer;
import de.felixperko.fractals.system.parameters.ParamValueType;

public class ColorXMLDeserializer extends AbstractParamXMLDeserializer {

    public ColorXMLDeserializer(ParamValueType valueType, double version) {
        super(valueType, version);
    }

    @Override
    public Object deserializeContent(String content, ParamContainer container) {
        String[] channels = content.split(",");
        float[] fs = new float[4];
        for (int i = 0 ; i < 4 ; i++){
            fs[i] = Integer.parseInt(channels[i])/255f;
        }
        return new Color(fs[0], fs[1], fs[2], fs[3]);
    }
}
