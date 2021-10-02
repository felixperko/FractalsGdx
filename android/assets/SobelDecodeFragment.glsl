#version 320 es
//#version 130
#define FLT_MIN 1.175494351e-38
#ifdef GL_ES
precision highp float;
#endif
uniform sampler2D u_texture;
uniform sampler2D palette;
uniform int usePalette;
in vec2 v_texCoords;
layout(location = 0) out vec4 colour1;
uniform float sobel_ambient;
uniform float sobel_magnitude;
uniform float colorAdd;
uniform float colorMult;
uniform float colorSaturation;
uniform int extractChannel;
uniform float mappingColorR;
uniform float mappingColorG;
uniform float mappingColorB;
float sobelLuminance = 1.0;
uniform float sobelPeriod;
uniform vec2 resolution;

const float resultOffset = 10.0;
const int kernelRadius = 1;
const int kernelDim = (kernelRadius*2+1);
const int kernelLength = kernelDim*kernelDim;

float DecodeExpV3( in vec3 pack )
{
    float exponent = float(int( pack.z * 256.0-128.0 ));
    float value  = (pack.x + (pack.y)/256.0);
    return (value) * exp2(exponent+1.0);
}

//float DecodeExpV3( in vec3 pack )
//{
//    int exponent = int( pack.z * 256.0 ) ;
//    float value  = dot( pack.xy, (1.0) / vec2(1.0, 256.0) ) + 1.0;
////    value        = ((value) * (256.0*256.0) / (256.0*256.0) );
//    return value * exp2( float(exponent) );
//}

vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float decode(in vec4 pixel){
    return DecodeExpV3(vec3(pixel)) - resultOffset;
}

void make_kernel(inout float n[kernelLength], sampler2D tex, vec2 coord){

    for (int y = -kernelRadius ; y <= kernelRadius ; y++){
        for (int x = -kernelRadius ; x <= kernelRadius ; x++){
            float val = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2(x, y), 0)));
            if (val == 0.0)
                val = 10.0;
            else if (val > 0.0)
                val = log(val);
            n[(y+kernelRadius)*kernelDim+(x+kernelRadius)] = val;
        }
    }

//    n[0] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2(-1, -1), 0));
//    n[1] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2( 0, -1), 0));
//    n[2] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2( 1, -1), 0));
//    n[3] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2(-1,  0), 0));
//    n[4] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0));
//    n[5] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2( 1, 0), 0));
//    n[6] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2(-1, 1), 0));
//    n[7] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2( 0, 1), 0));
//    n[8] = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2( 1, 1), 0));

//    float w = 1.0/resolution.x;
//    float h = 1.0/resolution.y;

//    n[0] = decode(texture2D(tex, coord + vec2(-w, -h)));
//    n[1] = decode(texture2D(tex, coord + vec2(0.0, -h)));
//    n[2] = decode(texture2D(tex, coord + vec2(w, -h)));
//    n[3] = decode(texture2D(tex, coord + vec2(-w, 0.0)));
//    n[4] = decode(texture2D(tex, coord));
//    n[5] = decode(texture2D(tex, coord + vec2(w, 0.0)));
//    n[6] = decode(texture2D(tex, coord + vec2(-w, h)));
//    n[7] = decode(texture2D(tex, coord + vec2(0.0, h)));
//    n[8] = decode(texture2D(tex, coord + vec2(w, h)));

//    n[0] = max(0.0, n[0]);
//    n[1] = max(0.0, n[1]);
//    n[2] = max(0.0, n[2]);
//    n[3] = max(0.0, n[3]);
//    n[4] = max(0.0, n[4]);
//    n[5] = max(0.0, n[5]);
//    n[6] = max(0.0, n[6]);
//    n[7] = max(0.0, n[7]);
//    n[8] = max(0.0, n[8]);
}

void main(void){


    float n[kernelLength];

    make_kernel(n, u_texture, v_texCoords.xy);

//    float d = decode(texture2D(u_texture, v_texCoords.xy));
    float d = decode(texelFetch(u_texture, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0));

    if (d > 0.0){

        d = log(d);

        //sobel edge detection
        float sobel_edge_h = n[2] + (2.0*n[5]) + n[8] - (n[0] + (2.0*n[3]) + n[6]);
        float sobel_edge_v = n[0] + (2.0*n[1]) + n[2] - (n[6] + (2.0*n[7]) + n[8]);
        float sobel = sqrt((sobel_edge_h*sobel_edge_h) + (sobel_edge_v*sobel_edge_v));

        //https://stackoverflow.com/a/10032882

        //https://stackoverflow.com/a/41065243
        //Gx_ij = i / (i*i + j*j)
        //Gy_ij = j / (i*i + j*j)

        float s = sobel;
        s = log(s+1.0);

        s = fract(s*0.5/sobelPeriod)*2.0;
        s = 1.0 - abs(1.0 - s);
        s *= sobelLuminance;

        //select color based on iteration count
        vec3 hsv = vec3(d/colorMult+colorAdd,colorSaturation,1.0);
        vec4 rgb = vec4(hsv2rgb(hsv), 1.0);

        float brightness = sobel_ambient;
        if (gl_FragCoord.x >= 1.0 && gl_FragCoord.x < resolution.x-1.0 && gl_FragCoord.y >= 1.0 && gl_FragCoord.y < resolution.y-1.0)
            brightness += sobel_magnitude*s;

//        float chance = fract(brightness * 256.0);
//        if (rand(v_texCoords.xy) < chance)
//            brightness += 1.0/256.0;



        //gl_FragColor = rgb;
        int brightness256 = int(brightness*256.0);

        float brightnessShift = float(brightness256)/128.0;
        float shiftG = 0.0;
        float shiftB = 0.0;
        if (brightnessShift >= 1.0)
            shiftG = 1.0/256.0;
        if (brightnessShift == 2.0)
            shiftB = 1.0/256.0;

        if (brightness > 1.0)
            brightness = 1.0;

        //palette
        if (usePalette > 0)
            rgb = texture(palette, vec2(mod(d/colorMult+colorAdd, 1.0), 0.0));
//            rgb = texture2D(palette, vec2(mod(d/colorMult+colorAdd, 1.0), 0.0));

        //test: reduce brightness banding at low brightness, might not work
        colour1 = vec4(brightness, brightness+shiftG, brightness+shiftB, 1.0) * rgb;
//        gl_FragColor = vec4(brightness, brightness, brightness, 1.0) * rgb;

        //extract channel
        if (extractChannel == 1)
            colour1 = vec4(colour1.r*mappingColorR, colour1.r*mappingColorG, colour1.r*mappingColorB, 1.0);
        else if (extractChannel == 2)
            colour1 = vec4(colour1.g*mappingColorR, colour1.g*mappingColorG, colour1.g*mappingColorB, 1.0);
        else if (extractChannel == 3)
            colour1 = vec4(colour1.b*mappingColorR, colour1.b*mappingColorG, colour1.b*mappingColorB, 1.0);
        //extract channel end

//            gl_FragColor = texture2D(palette, vec2(mod(d/colorMult+colorAdd, 256.0), 0.0));
//        if (v_texCoords.y > 0.5)
//        else
//            gl_FragColor = texture2D(u_texture, vec2(0.0, mod(v_texCoords.x, 1.0)));
    }
    else {
        colour1 = vec4(0.0,0.0,0.0,1.0);
    }
}