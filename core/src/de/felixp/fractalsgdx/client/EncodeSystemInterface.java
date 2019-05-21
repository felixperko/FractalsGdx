package de.felixp.fractalsgdx.client;

import com.badlogic.gdx.graphics.Color;

import java.util.UUID;

import de.felixperko.fractals.manager.client.ClientManagers;

public class EncodeSystemInterface extends SystemInterface {
//    public static void printColorBits(Color color){
//        int b1 = (int)(color.r*256);
//        int b2 = (int)(color.g*256);
//        int b3 = (int)(color.b*256);
//        System.out.println(Integer.toBinaryString(b3)+", "+Integer.toBinaryString(b1)+", "+Integer.toBinaryString(b2));
//    }

    public static void encode3fFast(Color color, float value){
        int integer = Float.floatToIntBits(value);
        int mantissa = integer & 0x007fffff;
        float exponent = ((integer >>> 23) & 0x000000ff);
        float mantissa1 = (integer >>> 15) & 0x000000ff;
        float mantissa2 = (integer >>> 7) & 0x000000ff;
        color.set(mantissa1/256f, mantissa2/256f, exponent/256f, 1f);
    }

//    public static Color encode4fFast(float value){
//        int integer = Float.floatToIntBits(value);
//        //int mantissa = integer & 0x007fffff;
//        float exponent = ((integer >>> 23) & 0x000000ff);
//        float mantissa1 = (integer >>> 15) & 0x000000ff;
//        float mantissa2 = (integer >>> 7) & 0x000000ff;
//        float mantissa3 = integer & 0x0000007f;
//        Color color = new Color(mantissa1/256, mantissa2/256, mantissa3/256, exponent/256);
//        return color;
//    }
//
//    public static float decode3fFast(Color color){
//        float mantissa1 = color.r;
//        float mantissa2 = color.g / 256;
//        float exponent = color.b*256 - 127;
//        float value = mantissa1 + mantissa2 + 1;
//        return value*(float)Math.pow(2, (exponent));
//    }
//
//    public static float decode4fFast(Color color){
//        float mantissa1 = color.r;
//        float mantissa2 = color.g / 256;
//        float mantissa3 = color.b / (256*256);
//        float exponent = color.a*256 - 127;
//        float value = mantissa1 + mantissa2 + 1;
//        return value*(float)Math.pow(2, (exponent));
//    }

    static double log2 = Math.log(2);
    public EncodeSystemInterface(UUID systemId, MessageInterface messageInterface, ClientManagers managers){
        super(systemId, messageInterface, managers);
    }

    @Override
    protected void getColor(Color color, float value) {
        encode3fFast(color, value);
//        Color c = encode3f(value);
//        float valueAfter = decode3f((byte)(c.r*256), (byte)(c.g*256), (byte)(c.b*256));
//        return encode1f(value);
    }

    private Color encode1f(float value){
        return new Color((value+1)/256,0, 0, 1);
    }

    private static Color encode3f(float value) {
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
        if (value < 0)
            value = 2000000;

        float logval = (float)Math.log(value+1);
//        float logval = log(value+1);
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

    public static double exp(double val) {
        final long tmp = (long) (1512775 * val + 1072632447);
        return Double.longBitsToDouble(tmp << 32);
    }

    /**
     * Pretty much useless for bigger numbers
     * @param x
     * @return
     */
    public static float log(float x) {
        return (float)(6 * (x - 1) / (x + 1 + 4 * (Math.sqrt(x))));
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
