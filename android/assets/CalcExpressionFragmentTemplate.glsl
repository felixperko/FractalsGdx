#version 130
//#version 400 //double support
#ifdef GL_ES //not needed? (ignored by desktop GL)
precision highp float;
#endif
in vec4 v_color;
in vec2 v_texCoords;
in vec2 pos;
out vec4 colour1;
out vec4 colour2;
uniform sampler2D samplesTexture;
uniform sampler2D u_texture;
uniform vec2 bufferOffset;
uniform int discardBuffer;
uniform float iterations;
//uniform sampler1D palette;
uniform vec2 center;
//uniform vec2 centerFp64Low;
//uniform float scale;
uniform float ratio;
//uniform float biasReal;
//uniform float biasImag;
uniform float samples;
//uniform float flip;
uniform float limit;
uniform float logPow;
//uniform vec4 renderBorders;
uniform float[] params;
uniform vec2 resolution;
uniform int sampleCountRoot;
uniform float maxBorderSamples;

const float log2 = log(2.0);
//const float upperborder = 100.0;
//const float lowerborder = 0.0;
//const float angle = radians(45.0);
const float notEscapedValue = 1.175494351e-38;
const float resultOffset = 10.0;
const float maxSamplesPerFrame = 1.0;
const float maxSamplesNotEscaped = 1.0;

uniform float burningship;
uniform float juliaset;

/*
    //changes for converting Java -> GLSL
    //byte representation: range not -128 - 127 (Java byte), but 0-255 (vec4 scalar components)
    //Math.log() -> log(); Math.abs() -> abs(); ...
    //Math.pow(2, var) -> exp2(var)
    //(int)var -> int(var)

    byte[] encodeV3(float value){
        int exponent = (int) (Math.log(Math.abs(value))/Math.log(2));
        double exponentScaling = Math.pow(2, exponent);
        value /= exponentScaling;
        byte[] encoded = new byte[3];
        value = value % 1f;
        encoded[0] = (byte)(value*256f-128f);
        encoded[1] = (byte)((value-(encoded[0]+128f)/256f)*256f-128f);
        encoded[2] = (byte) (exponent);
        return encoded;
    }
*/

vec3 EncodeExpV3( in float value )
{
    int exponent  = int(( log( abs( value ) )/log2 )) ;
//    if (exponent > -1)
    value        /= exp2( float( exponent ) );
//    value         = (value);
//    vec3 encode   = fract( value * vec3(1.0, 256.0, 256.0*256.0) );
//    return vec3( encode.xy - encode.yz / 256.0, (float(exponent) + 127.0) / 256.0 );
//    vec2 encode   = fract( value * vec2(1.0, 256.0));
//    value = mod(value, 1.0);
//    value -= 1.0;
value *= 0.5;
//    value *= 257.0/256.0;
    float e0 = float(int(value*256.0))/256.0;
    float e1 = (value-e0)*256.0;
//    float e1 = 0.0;
    return vec3( e0, e1, (float(exponent+128.0)) / 256.0 );
}

//vec3 EncodeExpV3( in float value )
//{
//    int exponent  = int( log( abs( value ) )/log2 );
//    value        /= exp2( float( exponent ) );
////    value         = (value);
////    vec3 encode   = fract( value * vec3(1.0, 256.0, 256.0*256.0) );
////    return vec3( encode.xy - encode.yz / 256.0, (float(exponent) + 127.0) / 256.0 );
//    vec2 encode   = fract( value * vec2(1.0, 256.0));
//    return vec3( encode.x, encode.y, (float(exponent) + 127.0) / 256.0 );
//}

/*
    float decodeV3(byte[] encoded){
        int exponent = (int) (encoded[2]);
        float value = 1f;
        value += (encoded[0]+128f)/256f;
        value += (encoded[1]+128f)/(256f*256f);
        float exponentScaling = (float) Math.pow(2, exponent);
        return value * exponentScaling;
    }
*/

//float DecodeExpV3( in vec3 pack )
//{
//    float scale = 256.0/257.0;
//    int exponent = int( pack.z * 256.0 - 128.0 );
//    float value  = pack.x + pack.y/256.0 + 1.0;
//    return value * exp2( float(exponent) );
//}

float DecodeExpV3( in vec3 pack )
{
//    float scale = 256.0/257.0;
    int exponent = int(( pack.z * 256.0 - 127.0 ));
    float value  = (pack.x + (pack.y+1.0)/256.0)*2.0;
    return value * exp2( float(exponent) );
}

//float DecodeExpV3( in vec3 pack )
//{
////    float scale = 256.0/257.0;
//    int exponent = int( pack.z * 256.0 - 127.0 );
//    float value  = dot( pack.xy, (257.0/256.0)/vec2(1.0, 256.0) ) + 1.0;
//    return value * exp2( float(exponent) );
//}

vec4 encode(in float value){
    //return EncodeRangeV4(value, lowerborder, upperborder);
    return vec4(EncodeExpV3(value), 1.0);
    //return EncodeExpV4(value);
}

float decode(in vec4 pixel){
    //return log(DecodeRangeV4(pixel, lowerborder, upperborder));
    return DecodeExpV3(vec3(pixel));
    //return DecodeExpV4(pixel);
}

void make_kernel(inout float n[9], sampler2D tex, vec2 coord){

      n[0] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2(-1, -1), 0));
      n[1] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2( 0, -1), 0));
      n[2] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2( 1, -1), 0));
      n[3] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2(-1,  0), 0));
      n[4] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2( 1, 0), 0));
      n[5] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2(-1, 1), 0));
      n[6] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2( 0, 1), 0));
      n[7] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2( 1, 1), 0));

      n[0] = max(0.0, n[0]);
      n[1] = max(0.0, n[1]);
      n[2] = max(0.0, n[2]);
      n[3] = max(0.0, n[3]);
      n[4] = max(0.0, n[4]);
      n[5] = max(0.0, n[5]);
      n[6] = max(0.0, n[6]);
      n[7] = max(0.0, n[7]);
  }

vec4 encodeInt(in float value){
    float valueLocal = value;
    float r = float(int(mod(valueLocal, 256.0)))/256.0;
    valueLocal /= 256.0;
    float g = float(int(valueLocal))/256.0;
//    float r = mod((value-mod(value, 256.0))/256.0, 256.0)/256.0;
//    float g = mod(value, 256.0)/256.0;
    return vec4(r, g, 0.0, 1.0);
}

float decodeInt(in vec4 pixel){
    return (pixel.r + pixel.g*256.0)*256.0;
//    return pixel.r*256.0;
//    return (pixel.r*256.0 + pixel.g)*256.0;
}

void addValue(inout vec4 pixel, in float addValue){
    float oldValue = decode(pixel);
    //if (addValue < 0) //do nothing
    //    pixel = encode(oldValue);
    if (oldValue >= 0.0)
        pixel = encode(oldValue+addValue);
}

void main()
{

    float outputXSq = 0.0;
    float outputYSq = 0.0;
    float outputX = 0.0;
    float outputY = 0.0;
    float resIterations = 0.0;

    int maxSampleCount = sampleCountRoot*sampleCountRoot;

//    float samplesCalculated = samples+maxSamplesPerFrame;
    bool first = true;

    float moved = 0.0;

    vec4 currentColor = texelFetch(u_texture, ivec2(gl_FragCoord.xy)+ivec2(bufferOffset.xy), 0);
    float currentValue = decode(currentColor);
    currentValue -= resultOffset;
    if (currentValue < 0.0)
        currentValue = 0.0;

    vec4 samplesColor = texelFetch(samplesTexture, ivec2(gl_FragCoord.xy)+ivec2(bufferOffset.xy), 0);
    float samples = decodeInt(samplesColor);

//    float samples = 1.0;
    if (discardBuffer == 1){
        samples = 0.0;
        currentValue = 0.0;
    }
    float frameSampleCount = min(maxSampleCount-samples, maxSamplesPerFrame);
    if (frameSampleCount <= 0 || (samples >= maxBorderSamples && currentValue <= 0.0)){
        colour1 = currentColor;
//        samples = samples+1.0;
//        colour2 = encodeInt(samples);
    }
    else {

        if (samples == 0.0)
            currentValue = 0.0;
//        samples = 1.0;
    //    if (v_texCoords.x < 0.9)
    //        discard;
    //    if (currentValue == 0.0){
    //        gl_FragData[0] = vec4(0.0, 0.0, 0.0, 0.0);
    //    }
    //    else {

        for (int s = 0 ; s < frameSampleCount ; s++){

            float sampleNo = samples;

            float deltaX = (pos.x - 0.5 + mod(sampleNo, sampleCountRoot)/(resolution.y*sampleCountRoot))*ratio;
            float deltaY = (pos.y - 0.5 + (sampleNo / sampleCountRoot)/(resolution.x*sampleCountRoot));
            <INIT>

            float resYSq = 0.0;
            float resXSq = 0.0;
            float lastR = 0.0;
            float lastI = 0.0;

            float loopIterations = notEscapedValue;

            float trapX = 0.1;
            float trapY = 0.2;
            float trapRadius = 0.1;

            for (float i = 0.0 ; i < iterations ; i++){

                <ITERATE>

    //             if (lastR == local_0 && lastI == local_1){
    //                resIterations = float(i);
    //                outputXSq = resXSq;
    //                outputYSq = resYSq;
    //                break;
    //             }
    //             lastR = local_0;
    //             lastI = local_1;

                resXSq = float(local_0*local_0);
                resYSq = float(local_1*local_1);

                float movedNow = sqrt(resXSq+resYSq)/maxSampleCount;
                moved += movedNow;

//                float xDistanceTrap = local_0 - trapX;
//                float yDistanceTrap = local_1 - trapY;
//                float distanceTrap = sqrt(xDistanceTrap*xDistanceTrap+yDistanceTrap*yDistanceTrap);
//                if (distanceTrap < trapRadius){
//                    loopIterations = float(i + 1.0);
//                    break;
//                }


                if (sqrt(resXSq + resYSq) > limit || movedNow == 0.0){
//                    loopIterations = float(i+1);
                    loopIterations = float(i + 1.0 + resultOffset - log(log(resXSq+resYSq)*0.5/log2)/(logPow));
    //                outputX += local_0;
    //                outputY += local_1;
//                    outputXSq += resXSq;
//                    outputYSq += resYSq;
    //                samplesCalculated += 1;
                    break;
                }
            }
            if (loopIterations == notEscapedValue){
                if (currentValue <= 0.0){
//                    samplesCalculated = 0.0;
                    samples += 1.0;
                    if (sampleNo < maxSamplesNotEscaped-1.0)
                        continue;
                    if (sampleNo >= maxBorderSamples-1.0)
                        break;
                    float n[9];
                    make_kernel(n, u_texture, v_texCoords.xy);
                    if (n[0] <= resultOffset+1.0 && n[1] <= resultOffset+1.0 && n[2] <= resultOffset+1.0 && n[3] <= resultOffset+1.0
                            && n[4] <= resultOffset+1.0 && n[5] <= resultOffset+1.0 && n[6] <= resultOffset+1.0 && n[7] <= resultOffset+1.0)
                        break;
                }
//                else {
//                    samplesCalculated--;
//                }
            } else {
//                resIterations += loopIterations;
//                float contrib = 1.0;
                float contrib = 1.0/(1.0+sampleNo);
                if (sampleNo <= 1.0)
                    currentValue = loopIterations;
                else
                    currentValue = currentValue*(1.0-contrib) + loopIterations*contrib;
            }
            samples += 1.0;
        }

    //    samplesCalculated = samplesCalculated + 1;
    //    if (samplesCalculated == -1)
    //        samplesCalculated = maxSampleCount;
//        if (samplesCalculated > 0){
//            resIterations = resIterations/samplesCalculated + resultOffset;
//        }

        if (currentValue > 0.0){
            currentValue += resultOffset;
            colour1 = encode(currentValue);
//            float decoded = decode(colour1);
//            colour1 = encode(decoded);
        } else {
            colour1 = encode(notEscapedValue);
        }
    }
    colour2 = encodeInt(samples);
}
