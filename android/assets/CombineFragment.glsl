//combines image sources

//<VERSION>
#ifdef GL_ES //not needed? (ignored by desktop GL)
precision highp float;
precision highp int;
#endif

in vec4 v_color;
in vec2 v_texCoords;

layout(location = 0) out vec4 colorOut0;
layout(location = 1) out vec4 colorOut1;
layout(location = 2) out vec4 colorOut2;

layout(binding = 3) uniform sampler2D textureCondition;
layout(binding = 4) uniform sampler2D textureSamples;
layout(binding = 5) uniform sampler2D textureFallback;
layout(binding = 0) uniform sampler2D textureConditionFrame;
layout(binding = 1) uniform sampler2D textureSamplesFrame;
layout(binding = 2) uniform sampler2D textureFallbackFrame;

uniform vec2 bufferOffset;
uniform int discardBuffer;

uniform int divs;
uniform float patternX;
uniform float patternY;

const float resultOffset = 10.0;

float DecodeExpV3( in vec3 pack )
{
    float exponent = float(int(pack.z*256.0-128.0));
    float value  = ((ceil(pack.r*256.0)/256.0+ceil(pack.g*256.0)/(256.0*256.0)));
    return value * exp2(exponent+1.0);
}

float decode(in vec4 pixel){
    return DecodeExpV3(vec3(pixel)) - resultOffset;
}

void main()
{

    ivec2 uv = ivec2(gl_FragCoord.xy);

    float u2  = float(uv.x)/2.0;
    float v2  = float(uv.y)/2.0;
    ivec2 uv2 = ivec2(u2, v2);

    vec4 storeColor0 = texelFetch(textureCondition, uv+ivec2(bufferOffset.xy), 0);
    vec4 storeColor1 = texelFetch(textureSamples, uv+ivec2(bufferOffset.xy), 0);
    vec4 storeColor2 = texelFetch(textureFallback, uv+ivec2(bufferOffset.xy), 0);
    vec4 storeColor3 = texelFetch(textureConditionFrame, uv2, 0);
    vec4 storeColor4 = texelFetch(textureSamplesFrame, uv2, 0);
    vec4 storeColor5 = texelFetch(textureFallbackFrame, uv2, 0);

//    vec4 outColor0 = vec4(0.0,0.0,0.0,1.0);
//    vec4 outColor1 = vec4(0.0,0.0,0.0,1.0);
//    vec4 outColor2 = vec4(0.0,0.0,0.0,1.0);

    float bufferEnabled = float(1 - discardBuffer);
//    float bufferEnabled = 0.0;

    float sampleSelected = (patternX-abs((mod(float(uv.x), 2.0))) < 0.5 && patternY-abs((mod(float(uv.y), 2.0))) < 0.5) ? 1.0 : 0.0;
//    float f_newValueCond = 1.0;
//    float f_newValueFallback = 1.0;
//    float f_newValueCond = (sampleSelected > 0.0) ? 1.0 : 0.0;
//    float f_newValueFallback = (sampleSelected > 0.0) ? 1.0 : 0.0;
    float f_newValueCond = (decode(storeColor3) > 0.0 && sampleSelected > 0.0) ? 1.0 : 0.0;
    float f_newValueFallback = (decode(storeColor5) > 0.0 && sampleSelected > 0.0) ? 1.0 : 0.0;

//    colorOut0 = f_newValueCond*storeColor3 + storeColor0*bufferEnabled*(1.0-f_newValueCond);
//    colorOut1 = f_newValueCond*storeColor4 + storeColor1*bufferEnabled;
//    colorOut2 = f_newValueFallback*storeColor5 + storeColor2*bufferEnabled*(1.0*f_newValueFallback);

//    colorOut0 = vec4(0.0, 0.0, 0.0, 1.0);
//    colorOut1 = vec4(0.0, 0.0, 0.0, 1.0);
//    colorOut2 = vec4(0.0, 0.0, 0.0, 1.0);

    if (bufferEnabled > 0.0)
        colorOut0 = storeColor0;
    if (sampleSelected > 0.0)
        colorOut0 = storeColor3;

    if (bufferEnabled > 0.0)
        colorOut1 = storeColor1;
    if (sampleSelected > 0.0)
        colorOut1 = storeColor4;

    if (bufferEnabled > 0.0)
        colorOut2 = storeColor2;
    if (sampleSelected > 0.0)
        colorOut2 = storeColor5;

    if (discardBuffer > 0){
        colorOut0 = vec4(0.0, 0.0, 0.0, 1.0);
        colorOut1 = vec4(0.0, 0.0, 0.0, 1.0);
        colorOut2 = vec4(0.0, 0.0, 0.0, 1.0);
    }
}