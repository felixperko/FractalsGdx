#version 130
#ifdef GL_ES
precision highp float;
#endif
in vec4 v_color;
in vec2 v_texCoords;
in vec2 pos;
out vec4 frag_color;
uniform sampler2D u_texture;
uniform int iterations;
//uniform sampler1D palette;
uniform vec2 center;
uniform float scale;
uniform float ratio;
uniform float biasReal;
uniform float biasImag;
uniform float samples;
uniform float flip;
uniform float limit;
uniform float logPow;
uniform float[] params;
uniform vec2 resolution;

const float log2 = log(2.0);
const float upperborder = 100.0;
const float lowerborder = 0.0;

uniform float burningship;
uniform float juliaset;

vec3 EncodeExpV3( in float value )
{
    int exponent  = int( log( abs( value ) )/log(2.0) + 1.0 );
    value        /= exp2( float( exponent ) );
    value         = (value + 1.0) * (256.0*256.0 - 1.0) / (2.0*256.0*256.0);
    vec3 encode   = fract( value * vec3(1.0, 256.0, 256.0*256.0) );
    return vec3( encode.xy - encode.yz / 256.0 + 1.0/512.0, (float(exponent) + 127.5) / 256.0 );
}

float DecodeExpV3( in vec3 pack )
{
    int exponent = int( pack.z * 256.0 - 127.0 );
    float value  = dot( pack.xy, 1.0 / vec2(1.0, 256.0) );
    value        = value * (2.0*256.0*256.0) / (256.0*256.0 - 1.0) - 1.0;
    return value * exp2( float(exponent) );
}

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

void addValue(inout vec4 pixel, in float addValue){
    float oldValue = decode(pixel);
    //if (addValue < 0) //do nothing
    //    pixel = encode(oldValue);
    if (oldValue >= 0.0)
        pixel = encode(oldValue+addValue);
}

void main()
{
    float deltaX = (pos.x - 0.5)*ratio;
    float deltaY = (pos.y - 0.5);

    <INIT>

    float resYSq = local_1*local_1;
    float resXSq = local_0*local_0;
    float resIterations = -1.0;
    float outputXSq = 0.0;
    float outputYSq = 0.0;
    bool first = true;
    for (int i = 0 ; i < iterations ; i++){

        <ITERATE>

        resXSq = local_0*local_0;
        resYSq = local_1*local_1;

        if (resXSq + resYSq > limit){
            resIterations = float(i);
            outputXSq = resXSq;
            outputYSq = resYSq;
            break;
        }
    }

    float resX = local_0;
    float resY = local_1;

    float lSq = outputXSq + outputYSq;
    if (resIterations >= 0){
        float smoothIterations = (resIterations + 1.0 - log(log(lSq)*0.5/log2)/(logPow));
        vec4 lastColor = texture2D(u_texture, v_texCoords);
        float last = decode(lastColor);
        frag_color = encode(smoothIterations + last);
    } else {
        frag_color = vec4(0.0, 0.0, 0.0, 0.0);
    }
}
