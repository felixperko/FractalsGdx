package de.felixp.fractalsgdx.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import de.felixp.fractalsgdx.rendering.ShaderSystemContext;
import de.felixp.fractalsgdx.rendering.orbittrap.OrbittrapsXMLDeserializer;
import de.felixp.fractalsgdx.rendering.orbittrap.OrbittrapsXMLSerializer;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.io.ParamContainerDeserializer;
import de.felixperko.io.ParamContainerSerializer;
import de.felixperko.io.ParamSupplierTypeRegistry;
import de.felixperko.io.ParamXMLDeserializerRegistry;
import de.felixperko.io.ParamXMLSerializerRegistry;
import de.felixperko.io.deserializers.ComplexNumberXMLDeserializer;
import de.felixperko.io.deserializers.ExpressionsXMLDeserializer;
import de.felixperko.io.deserializers.NumberXMLDeserializer;
import de.felixperko.io.serializers.ComplexNumberXMLSerializer;
import de.felixperko.io.serializers.ExpressionParamXMLSerializer;
import de.felixperko.io.serializers.NumberXMLSerializer;

public class FractalsIOUtil {

    public static ParamContainer deserializeParamContainer(byte[] bytes, ParamConfiguration paramConfiguration, ParamContainer paramContainer){
        return getParamContainerDeserializer().deserialize(new ByteArrayInputStream(bytes), paramConfiguration, paramContainer, false);
    }

    public static ParamContainerDeserializer getParamContainerDeserializer(){
        ParamXMLDeserializerRegistry deserializerRegistry = new ParamXMLDeserializerRegistry();
        deserializerRegistry.registerDeserializers(
                new ComplexNumberXMLDeserializer(CommonFractalParameters.complexnumberType, 1.0),
                new NumberXMLDeserializer(CommonFractalParameters.numberType, 1.0),
                new ExpressionsXMLDeserializer(CommonFractalParameters.expressionsType, 1.0),
                new OrbittrapsXMLDeserializer(ShaderSystemContext.TYPE_ORBITTRAPS, 1.0)
        );
        ParamContainerDeserializer deserializer = new ParamContainerDeserializer(new ParamSupplierTypeRegistry(), deserializerRegistry);
        return deserializer;
    }

    public static String serializeParamContainer(ParamContainer paramContainer, ParamConfiguration paramConfiguration){
        ParamContainerSerializer serializer = getParamContainerSerializer();
        String paramText = new String(serializer.serialize(paramContainer, paramConfiguration, false, false,true, false));
        return paramText;
    }

    public static ParamContainerSerializer getParamContainerSerializer(){
        ParamXMLSerializerRegistry xmlSerializers = new ParamXMLSerializerRegistry();
        xmlSerializers.registerSerializers(
                new ExpressionParamXMLSerializer(CommonFractalParameters.expressionsType, 1.0),
                new ComplexNumberXMLSerializer(CommonFractalParameters.complexnumberType, 1.0),
                new NumberXMLSerializer(CommonFractalParameters.numberType, 1.0),
                new OrbittrapsXMLSerializer(ShaderSystemContext.TYPE_ORBITTRAPS, 1.0)
        );
        ParamContainerSerializer serializer = new ParamContainerSerializer(xmlSerializers, new ParamSupplierTypeRegistry());
        return serializer;
    }
}
