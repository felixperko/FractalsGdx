package de.felixp.fractalsgdx.rendering.palette;

import com.badlogic.gdx.graphics.Color;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.felixperko.fractals.io.serializers.AbstractParamXMLSerializer;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

public class ColorXMLSerializer extends AbstractParamXMLSerializer {

    public ColorXMLSerializer(ParamValueType valueType, double version) {
        super(valueType, version);
    }

    @Override
    protected void serializeContent(Document document, Node paramRootNode, ParamSupplier paramSupp, double version) {
        checkStaticType(paramSupp, Color.class);
        Color val = paramSupp.getGeneral(Color.class);
        float[] fs = new float[]{val.r, val.g, val.b, val.a};
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < 4 ; i++){
            int v = (int)(fs[i]*255f);
            sb.append(v);
            if (i < 3)
                sb.append(",");
        }
        setText(paramRootNode, sb.toString());
    }
}
