package de.felixp.fractalsgdx.rendering.orbittrap;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.io.serializers.AbstractParamXMLSerializer;

public class OrbittrapsXMLSerializer extends AbstractParamXMLSerializer {

    OrbittrapClassRegistry clsRegistry = new OrbittrapClassRegistry();

    public OrbittrapsXMLSerializer(ParamValueType valueType, double version) {
        super(valueType, version);
    }

    @Override
    protected void serializeContent(Document document, Node node, ParamSupplier paramSupplier, double v) {
        checkStaticType(paramSupplier, OrbittrapContainer.class);
        OrbittrapContainer container = paramSupplier.getGeneral(OrbittrapContainer.class);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Orbittrap ot : container.getOrbittraps()){
            String clsKey = clsRegistry.getClassKey(ot.getClass());
            if (clsKey == null)
                throw new IllegalArgumentException("Class not registered in ClassRegistry: "+ot.getClass().getName());
            if (!first)
                sb.append(";");
            else
                first = false;

            sb.append(clsKey).append(":");
            if (AxisOrbittrap.class.isInstance(ot)){
                AxisOrbittrap o = (AxisOrbittrap) ot;
                sb.append(o.getId())
                        .append(":").append(o.getName())
                        .append(":").append(o.getOffset().toDouble())
                        .append(":").append(o.getWidth().toDouble())
                        .append(":").append(o.getAngle().toDouble());
            }
            else if (CircleOrbittrap.class.isInstance(ot)){
                CircleOrbittrap o = (CircleOrbittrap) ot;
                sb.append(o.getId())
                        .append(":").append(o.getName())
                        .append(":").append(o.getCenter().toString())
                        .append(":").append(o.getRadius().toDouble());
            }
        }
        setText(node, sb.toString());
    }
}
