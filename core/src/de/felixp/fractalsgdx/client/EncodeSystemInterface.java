package de.felixp.fractalsgdx.client;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import de.felixperko.fractals.data.Chunk;
import de.felixperko.fractals.manager.client.ClientManagers;
import de.felixperko.fractals.system.Numbers.infra.ComplexNumber;
import de.felixperko.fractals.system.Numbers.infra.Number;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamSupplier;

public class EncodeSystemInterface extends SystemInterface {

    static double log2 = Math.log(2);
    public EncodeSystemInterface(MessageInterface messageInterface, ClientManagers managers){
        super(messageInterface, managers);
    }

    @Override
    protected Color getColor(float value) {
        return encode3f(value);
//        Color c = encode3f(value);
//        float valueAfter = decode3f((byte)(c.r*256), (byte)(c.g*256), (byte)(c.b*256));
//        return encode1f(value);
    }

    private Color encode1f(float value){
        return new Color((value+1)/256,0, 0, 1);
    }

    private Color encode3f(float value) {
//        vec3 EncodeExpV3( in float value )
//        {
//            int exponent  = int( log( abs( value ) )/log(2.0) + 1.0 );
//            value        /= exp2( float( exponent ) );
//            value         = (value + 1.0) * (256.0*256.0 - 1.0) / (2.0*256.0*256.0);
//            vec3 encode   = fract( value * vec3(1.0, 256.0, 256.0*256.0) );
//            return vec3( encode.xy - encode.yz / 256.0 + 1.0/512.0, (float(exponent) + 127.5) / 256.0 );
//        }
        if (value == 0)
            return new Color(0,0,0,1);
        float logval = (float)Math.log(value);
        float byLog2 = (float)(logval/log2);
        int exponent = (int)(byLog2+1);
        if (exponent != 0)
            value /= exponent > 0 ? 2 << exponent : (float)Math.pow(2.,exponent);
        value = (value + 1f) * (256*256 - 1f) / (2f*256*256);
        float r = (value) % 1;
        float g = (value*256) % 1;
        float b = (value*256*256) % 1;
        float r2 = (r - g/256f + 1/512f);
        float g2 = (g - b/256f + 1/512f);
        float b2 = (exponent + 127.5f) / 256f;
        return new Color(r2, g2, b2, 1);
//        return new Color(0f, 0f, value, 1f);
    }

    private float decode3f(byte x, byte y, byte z){
//        int exponent = int( pack.z * 256.0 - 127.0 );
//        float value  = dot( pack.xy, 1.0 / vec2(1.0, 256.0) );
//        value        = value * (2.0*256.0*256.0) / (256.0*256.0 - 1.0) - 1.0;
//        return value * exp2( float(exponent) );
        int exponent = (int) (z-127.0);
        float xf = x/256f;
        float yf = y/256f;
        float value = (x + y/256f);
        value *= (2f*256*256)/(256*256 - 1f);
        value -= 1;
        return value * (2^exponent);
    }
}
