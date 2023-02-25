package de.felixp.fractalsgdx.rendering.orbittrap;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.fractals.io.deserializers.AbstractParamXMLDeserializer;

public class OrbittrapsXMLDeserializer extends AbstractParamXMLDeserializer {

    OrbittrapClassRegistry clsRegistry = new OrbittrapClassRegistry();

    public OrbittrapsXMLDeserializer(ParamValueType valueType, double version) {
        super(valueType, version);
    }

    @Override
    public Object deserializeContent(String content, ParamContainer paramContainer) {

        if (content.isEmpty())
            return new OrbittrapContainer(new ArrayList<>());

        List<Orbittrap> list = new ArrayList<>();
        String[] s = content.split(";");
        NumberFactory nf = paramContainer.getParam(CommonFractalParameters.PARAM_NUMBERFACTORY).getGeneral(NumberFactory.class);
        for (String otStr : s){
            String[] s2 = otStr.split(":");
            Class<? extends Orbittrap> cls = clsRegistry.getClass(s2[0]);
            if (cls == null)
                throw new IllegalArgumentException("Class key not registered in ClassRegistry: "+s2[0]+ "\n"
                        +"Content: "+content);

            if (AxisOrbittrap.class.isAssignableFrom(cls)){
                AxisOrbittrap ot = new AxisOrbittrap(Integer.parseInt(s2[1]), nf, nf.cn(s2[3]), nf.cn(s2[4]), nf.cn(s2[5]));
                ot.setName(s2[2]);
                list.add(ot);
            }
            else if (CircleOrbittrap.class.isAssignableFrom(cls)){
                String[] cns = s2[3].split(",");
                CircleOrbittrap ot = new CircleOrbittrap(Integer.parseInt(s2[1]), nf.ccn(cns[0], cns[1]), nf.cn(s2[4]));
                ot.setName(s2[2]);
                list.add(ot);
            }
        }
        OrbittrapContainer cont = new OrbittrapContainer(list);
        return cont;
    }
}
