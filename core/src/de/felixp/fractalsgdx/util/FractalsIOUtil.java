package de.felixp.fractalsgdx.util;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import de.felixp.fractalsgdx.rendering.rendererparams.ClientParamsEscapeTime;
import de.felixp.fractalsgdx.rendering.renderers.ShaderSystemContext;
import de.felixp.fractalsgdx.rendering.orbittrap.OrbittrapsXMLDeserializer;
import de.felixp.fractalsgdx.rendering.orbittrap.OrbittrapsXMLSerializer;
import de.felixp.fractalsgdx.rendering.palette.ColorXMLDeserializer;
import de.felixp.fractalsgdx.rendering.palette.ColorXMLSerializer;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.io.deserializers.BooleanXMLDeserializer;
import de.felixperko.fractals.io.deserializers.IntegerXMLDeserializer;
import de.felixperko.fractals.io.deserializers.StringXMLDeserializer;
import de.felixperko.fractals.io.serializers.BooleanXMLSerializer;
import de.felixperko.fractals.io.serializers.IntegerXMLSerializer;
import de.felixperko.fractals.io.serializers.StringXMLSerializer;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.fractals.io.ParamContainerDeserializer;
import de.felixperko.fractals.io.ParamContainerSerializer;
import de.felixperko.fractals.io.ParamSupplierTypeRegistry;
import de.felixperko.fractals.io.ParamXMLDeserializerRegistry;
import de.felixperko.fractals.io.ParamXMLSerializerRegistry;
import de.felixperko.fractals.io.deserializers.ComplexNumberXMLDeserializer;
import de.felixperko.fractals.io.deserializers.ExpressionsXMLDeserializer;
import de.felixperko.fractals.io.deserializers.NumberXMLDeserializer;
import de.felixperko.fractals.io.serializers.ComplexNumberXMLSerializer;
import de.felixperko.fractals.io.serializers.ExpressionParamXMLSerializer;
import de.felixperko.fractals.io.serializers.NumberXMLSerializer;

public class FractalsIOUtil {

    static ParamContainerDeserializer deserializer;
    static ParamContainerSerializer serializer;

    public static ParamContainer[] deserializeParamContainers(byte[] bytes, ParamConfiguration computeConfig, ParamContainer computeBaseContainer,
                                                              ParamConfiguration drawConfig, ParamContainer drawBaseContainer){
        return getParamContainerDeserializer().deserialize(new ByteArrayInputStream(bytes), computeConfig, computeBaseContainer, drawConfig, drawBaseContainer, false);
    }

    public static String serializeParamContainers(ParamContainer computeParamContainer, ParamConfiguration computeParamConfig,
                                                  ParamContainer drawParamContainer, ParamConfiguration drawParamConfig){
        ParamContainerSerializer serializer = getParamContainerSerializer();
        String paramText = new String(serializer.serialize(computeParamContainer, computeParamConfig, drawParamContainer, drawParamConfig, false, false,true, false));

        Scanner scanner = new Scanner(paramText);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        String line;
        while (scanner.hasNextLine()){
            line = scanner.nextLine();
            if (!first){
                sb.append(line).append("\n");
            }
            if (first){
                first = false;
            }
        }
        paramText = sb.toString();
        return paramText;
    }

    public static ParamContainerDeserializer getParamContainerDeserializer(){
        if (deserializer != null)
            return deserializer;
        ParamXMLDeserializerRegistry deserializerRegistry = new ParamXMLDeserializerRegistry();
        deserializerRegistry.registerDeserializers(
                new ComplexNumberXMLDeserializer(CommonFractalParameters.complexnumberType, 1.0),
                new NumberXMLDeserializer(CommonFractalParameters.numberType, 1.0),
                new ExpressionsXMLDeserializer(CommonFractalParameters.expressionsType, 1.0),
                new IntegerXMLDeserializer(CommonFractalParameters.integerType, 1.0),
                new BooleanXMLDeserializer(CommonFractalParameters.booleanType, 1.0),
                new StringXMLDeserializer(CommonFractalParameters.selectionType, 1.0),
                new StringXMLDeserializer(CommonFractalParameters.stringType, 1.0),
                new ColorXMLDeserializer(ClientParamsEscapeTime.TYPE_COLOR, 1.0),
                new OrbittrapsXMLDeserializer(ShaderSystemContext.TYPE_ORBITTRAPS, 1.0)
        );
        deserializer = new ParamContainerDeserializer(new ParamSupplierTypeRegistry(), deserializerRegistry);
        return deserializer;
    }

    public static ParamContainerSerializer getParamContainerSerializer(){
        if (serializer != null)
            return serializer;
        ParamXMLSerializerRegistry xmlSerializers = new ParamXMLSerializerRegistry();
        xmlSerializers.registerSerializers(
                new ExpressionParamXMLSerializer(CommonFractalParameters.expressionsType, 1.0),
                new ComplexNumberXMLSerializer(CommonFractalParameters.complexnumberType, 1.0),
                new NumberXMLSerializer(CommonFractalParameters.numberType, 1.0),
                new IntegerXMLSerializer(CommonFractalParameters.integerType, 1.0),
                new BooleanXMLSerializer(CommonFractalParameters.booleanType, 1.0),
                new StringXMLSerializer(CommonFractalParameters.selectionType, 1.0),
                new StringXMLSerializer(CommonFractalParameters.stringType, 1.0),
                new ColorXMLSerializer(ClientParamsEscapeTime.TYPE_COLOR, 1.0),
                new OrbittrapsXMLSerializer(ShaderSystemContext.TYPE_ORBITTRAPS, 1.0)
        );
        serializer = new ParamContainerSerializer(xmlSerializers, new ParamSupplierTypeRegistry());
        return serializer;
    }
}
