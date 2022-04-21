//<VERSION>
#ifdef GL_ES //not needed? (ignored by desktop GL)
precision highp float;
precision highp int;
#endif

in vec4 v_color;
in vec2 v_texCoords;
in vec2 pos;
layout(location = 2) out vec4 colorOut;
//out vec4 colorOut;
uniform sampler2D valueTexture;
uniform sampler2D samplesTexture;
uniform sampler2D u_texture;
uniform vec2 bufferOffset;
uniform int samplesPerPixel;
uniform int depthPattern;
uniform int discardBuffer;

//const int samplesPerPixel = 50;
const float resultOffset = 10.0;

//const float enabledDepth = 1.0;
//const float disabledDepth = 0.0;

float decodeInt(in vec4 pixel){
    return (pixel.r + floor(pixel.g*256.0))*256.0;
}

vec4 encodeInt(in float value){
    float valueLocal = value;
    float g = floor(value/256.0)/256.0;
    float r = mod(value, 256.0)/256.0;
    return vec4(r, g, 0.0, 1.0);
}

float DecodeExpV3( in vec3 pack )
{
    float exponent = float(int(pack.z*256.0-128.0));
    float value  = ((ceil(pack.r*256.0)/256.0+ceil(pack.g*256.0)/(256.0*256.0)));
    return value * exp2(exponent+1.0);
}

float decode(in vec4 pixel){
    return DecodeExpV3(vec3(pixel)) - resultOffset;
}

void make_kernel(inout float n[12], sampler2D tex, vec2 coord){
    //TODO just check if r=0 g=0 b=0?
    n[0] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1, -1)+ivec2(bufferOffset.xy), 0)));
    n[1] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, -1)+ivec2(bufferOffset.xy), 0)));
    n[2] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, -1)+ivec2(bufferOffset.xy), 0)));
    n[3] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1,  0)+ivec2(bufferOffset.xy), 0)));
    n[4] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, 0)+ivec2(bufferOffset.xy), 0)));
    n[5] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1, 1)+ivec2(bufferOffset.xy), 0)));
    n[6] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, 1)+ivec2(bufferOffset.xy), 0)));
    n[7] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, 1)+ivec2(bufferOffset.xy), 0)));
    n[8] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 2, 0)+ivec2(bufferOffset.xy), 0)));
    n[9] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-2, 0)+ivec2(bufferOffset.xy), 0)));
    n[10] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, 2)+ivec2(bufferOffset.xy), 0)));
    n[11] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, -2)+ivec2(bufferOffset.xy), 0)));
}

void main()
{
//    ivec2 uv = ivec2(gl_FragCoord.xy + bufferOffset.xy);
//    ivec2 uv = ivec2(gl_FragCoord.xy);
        ivec2 uv = ivec2(floor(gl_FragCoord.x) + floor(bufferOffset.x), floor(gl_FragCoord.y) + floor(bufferOffset.y));

    vec4 samplesColor = texelFetch(u_texture, uv, 0);
    float samples = decodeInt(samplesColor);

    float remainingSamples = float(samplesPerPixel)-samples;
//    colorOut = encodeInt(remainingSamples);

//    float n[12];
//    make_kernel(n, valueTexture, v_texCoords.xy);

    int skip = 0;

//    if (discardBuffer == 1){
//        remainingSamples = float(samplesPerPixel);
//    }

//    if (samples > 1.0 && n[0] <= 0.0 && n[1] <= 0.0 && n[2] <= 0.0 && n[3] <= 0.0
//    && n[4] <= 0.0 && n[5] <= 0.0 && n[6] <= 0.0 && n[7] <= 0.0
//    && n[8] <= 0.0 && n[9] <= 0.0 && n[10] <= 0.0 && n[11] <= 0.0){
//        skip = 1;
////        remainingSamples = 0.0;
////        colorOut = encodeInt(0.0);
//    }

    if (depthPattern == 0 && int(uv.x) % 2 == 0 && int(uv.y) % 2 == 0){
        skip = 1;
    }
    else if (depthPattern == 1 && int(uv.x) % 2 == 0 && int(uv.y) % 2 == 1){
        skip = 1;
    }
    else if (depthPattern == 2 && int(uv.x) % 2 == 1 && int(uv.y) % 2 == 0){
        skip = 1;
    }
    else if (depthPattern == 3 && int(uv.x) % 2 == 1 && int(uv.y) % 2 == 1){
        skip = 1;
    }
    colorOut = skip > 0 ? encodeInt(1.0) : encodeInt(remainingSamples);
}