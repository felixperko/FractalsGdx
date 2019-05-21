#ifdef GL_ES
precision highp float;
#endif
uniform sampler2D u_texture;
varying vec2 v_texCoords;
const float sobel_ambient = 0.2;
const float sobel_magnitude = 0.8;
uniform float colorShift;
uniform vec2 resolution;

float rand(vec2 co){
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

float DecodeExpV3( in vec3 pack )
{
    int exponent = int( pack.z * 256.0 - 127.0 );
    float value  = dot( pack.xy, 1.0 / vec2(1.0, 256.0) );
    value        = value * (2.0*256.0*256.0) / (256.0*256.0 - 1.0) - 1.0;
    return value * exp2( float(exponent) );
}

float DecodeExpV3Fast (in vec3 pack){
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
    float value = DecodeExpV3Fast(vec3(pixel));
    //float value = Decode1(pixel);
    return log(value+1.0);
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

    float sobel_edge_h = n[2] + (2.0*n[5]) + n[8] - (n[0] + (2.0*n[3]) + n[6]);
    float sobel_edge_v = n[0] + (2.0*n[1]) + n[2] - (n[6] + (2.0*n[7]) + n[8]);
    float sobel = sqrt((sobel_edge_h*sobel_edge_h) + (sobel_edge_v*sobel_edge_v));

    float s = sobel;
    s = log(s+1.0);
    float d = decode(texture2D(u_texture, v_texCoords.xy));
    if (d > 0.0){
        d = log(d+1.0);
        //s = min(1.0, s);
        s = fract(s*0.5)*2.0;
        s = 1.0 - abs(1.0 - s);
        vec3 hsv = vec3(d+colorShift,0.6,1.0);
        vec4 rgb = vec4(hsv2rgb(hsv), 1.0);
        //gl_FragColor = rgb;
        float brightness = sobel_ambient + sobel_magnitude*s;
        //float chance = fract(brightness * 256.0);
        //if (rand(v_texCoords.xy) < chance)
        //    brightness += 1.0/256.0;
        gl_FragColor = vec4(brightness, brightness, brightness, 1) * rgb;
    }
    else {
        gl_FragColor = vec4(0.0,0.0,0.0,1.0);
    }
}