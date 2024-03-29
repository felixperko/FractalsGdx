#ifdef GL_ES
precision highp float;
#endif
uniform sampler2D u_texture;
varying vec2 v_texCoords;
uniform float sobel_ambient;
uniform float sobel_magnitude;
uniform float colorAdd;
uniform float colorMult;
uniform float colorSaturation;
uniform int extractChannel;
float sobelLuminance = 1.0;
uniform float sobelPeriod;
uniform vec2 resolution;

uniform float axisWidth;
uniform vec4 axisColor;
uniform vec2 axisTexCoords;

float rand(vec2 co){
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

float DecodeExpV3( in vec3 pack )
{
    int exponent = int( pack.z * 256.0 - 127.0 );
    float value  = dot( pack.xy, 1.0 / vec2(1.0, 256.0) );
    //value        = value * (2.0*256.0*256.0) / (256.0*256.0 - 1.0) - 1.0;
    return value * exp2( float(exponent) );
}

float DecodeExpV3_2 (in vec3 pack){
    float mantissa1 = pack.r;
    float mantissa2 = pack.g / 256.0;
    //float exponent = color.b*256.0 - 127.0;
    float value = mantissa1 + mantissa2 + 1.0;
    //return value*(float)Math.pow(2, (exponent));
    //float value = 1.0 + dot(pack.rgb, 1.0 / vec3(1.0, 256.0, 256.0*256.0));
    float exponent = pack.b * 256.0 - 127.0;
    return value * exp2(exponent);
}

float Decode1( in vec4 pack){
    return pack.x*256.0;
}

vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float decode(in vec4 pixel){
    float value = DecodeExpV3_2(vec3(pixel));
    return log(value+1.0);
    //return value;
}

void make_kernel(inout float n[9], sampler2D tex, vec2 coord){
    float w = 1.0/resolution.x;
    float h = 1.0/resolution.y;

    //float w = 1.0/1920.0;
    //float h = 1.0/1080.0;

    n[0] = decode(texture2D(tex, coord + vec2(-w, -h)));
    n[1] = decode(texture2D(tex, coord + vec2(0.0, -h)));
    n[2] = decode(texture2D(tex, coord + vec2(w, -h)));
    n[3] = decode(texture2D(tex, coord + vec2(-w, 0.0)));
    n[4] = decode(texture2D(tex, coord));
    n[5] = decode(texture2D(tex, coord + vec2(w, 0.0)));
    n[6] = decode(texture2D(tex, coord + vec2(-w, h)));
    n[7] = decode(texture2D(tex, coord + vec2(0.0, h)));
    n[8] = decode(texture2D(tex, coord + vec2(w, h)));
}

void main(void){

    float n[9];

    make_kernel(n, u_texture, v_texCoords.xy);

    float d = decode(texture2D(u_texture, v_texCoords.xy));
    if (d > 0.0){

        //sobel edge detection
        float sobel_edge_h = n[2] + (2.0*n[5]) + n[8] - (n[0] + (2.0*n[3]) + n[6]);
        float sobel_edge_v = n[0] + (2.0*n[1]) + n[2] - (n[6] + (2.0*n[7]) + n[8]);
        float sobel = sqrt((sobel_edge_h*sobel_edge_h) + (sobel_edge_v*sobel_edge_v));

        float s = sobel;
        s = log(s+1.0);

        s = fract(s*0.5/sobelPeriod)*2.0;
        s = 1.0 - abs(1.0 - s);
        s *= sobelLuminance;

        //select color based on iteration count
        vec3 hsv = vec3(d/colorMult+colorAdd,colorSaturation,1.0);
        vec4 rgb = vec4(hsv2rgb(hsv), 1.0);
        float brightness = sobel_ambient + sobel_magnitude*s;
        float chance = fract(brightness * 256.0);
        if (rand(v_texCoords.xy) < chance)
            brightness += 1.0/256.0;



        //gl_FragColor = rgb;
        int brightness256 = int(brightness*256.0);

        //test: reduce brightness banding at low brightness, might not work
        float brightnessShift = float(brightness256)/128.0;
        float shiftG = 0.0;
        float shiftB = 0.0;
        if (brightnessShift >= 1.0)
            shiftG = 1.0/256.0;
        if (brightnessShift == 2.0)
            shiftB = 1.0/256.0;
        gl_FragColor = vec4(brightness, brightness+shiftG, brightness+shiftB, 1.0) * rgb;

        if (brightness > 1.0)
            brightness = 1.0;
        gl_FragColor = vec4(brightness, brightness, brightness, 1.0) * rgb;

        //extract channel
        if (extractChannel == 1)
            gl_FragColor = vec4(gl_FragColor.r, gl_FragColor.r, gl_FragColor.r, 1.0);
        else if (extractChannel == 2)
            gl_FragColor = vec4(gl_FragColor.g, gl_FragColor.g, gl_FragColor.g, 1.0);
        else if (extractChannel == 3)
            gl_FragColor = vec4(gl_FragColor.b, gl_FragColor.b, gl_FragColor.b, 1.0);
        //extract channel end
    }
    else {
        gl_FragColor = vec4(0.0,0.0,0.0,1.0);
    }

    //draw axis
    if (axisColor.a > 0.0){
        vec2 axisDistances = v_texCoords.xy - axisTexCoords;
        vec2 overlap = (axisWidth/resolution.xy - abs(axisDistances.xy))*resolution.xy;
        float maxOverlap = min(1.0, max(overlap.x, overlap.y));
        //if ( < width/resolution.x || abs(axisDistances.y) < width/resolution.y)
        float axisAlpha = axisColor.a * maxOverlap;
        vec3 axisAlphaVec = vec3(axisAlpha, axisAlpha, axisAlpha);
        if (maxOverlap > 0.0)
            gl_FragColor.xyz = (1.0 - axisAlphaVec) * gl_FragColor.xyz + axisAlphaVec * axisColor.xyz;
            //gl_FragColor = vec4(1.0,1.0,1.0,0.1);
    }
}