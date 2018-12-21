#ifdef GL_ES
precision highp float;
#endif
uniform sampler2D u_texture;
varying vec2 v_texCoords;
uniform vec2 resolution;
const float ambient = 0.3;
const float magnitude = 0.3;
const float upperborder = 100.0;
const float lowerborder = 0.0;
uniform float samples;
uniform float decode_factor;
uniform float colorShift;

float unpack_depth(const in vec4 rgba_depth)
{
    const vec4 bit_shift = vec4(1.0/(256.0*256.0), 1.0/(256.0), 1.0, 256.0);
    float depth = dot(rgba_depth, bit_shift);
    return depth;
}

float DecodeExpV4( in vec4 pack )
{
    int exponent = int( pack.w * 256.0 - 127.0 );
    float value  = dot( pack.xyz, 1.0 / vec3(1.0, 256.0, 256.0*256.0) );
    value        = value * (2.0*256.0*256.0*256.0) / (256.0*256.0*256.0 - 1.0) - 1.0;
    return value * exp2( float(exponent) );
}

float DecodeExpV3( in vec3 pack )
{
    int exponent = int( pack.z * 256.0 - 127.0 );
    float value  = dot( pack.xy, 1.0 / vec2(1.0, 256.0) );
    value        = value * (2.0*256.0*256.0) / (256.0*256.0 - 1.0) - 1.0;
    return value * exp2( float(exponent) );
}

float DecodeRangeV4( in vec4 pack, in float minVal, in float maxVal )
{
    float value  = dot( pack, 1.0 / vec4(1.0, 256.0, 256.0*256.0, 256.0*256.0*256.0) );
    value       *= (256.0*256.0*256.0) / (256.0*256.0*256.0 - 1.0);
    return mix( minVal, maxVal, value );
}

vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float decode(in vec4 pixel){
    //return log(DecodeRangeV4(pixel, lowerborder, upperborder));
    float value = DecodeExpV3(vec3(pixel));
    return log(value/samples)*0.5;
    //return DecodeExpV4(pixel);
}

void make_kernel(inout float n[9], sampler2D tex, vec2 coord){
    float w = 1.0/resolution.x;
    float h = 1.0/resolution.y;

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

    //gl_FragColor = texture2D(u_texture, v_texCoords);
    //gl_FragColor = vec4(0.3 + 0.3*sobel.rgb, 1.0)*texture2D(u_texture, v_texCoords);
    float s = sobel;
    s = log(s+0.75);
    if (s > 1.0)
        s = 1.0;
    //float accumulated = samples < 10.0 ? samples : 10.0;
    float d = decode(texture2D(u_texture, v_texCoords));
    if (d > 0.0){
        vec3 hsv = vec3(d+colorShift,0.6,1.0);
        vec4 rgb = vec4(hsv2rgb(hsv), 1.0);
        gl_FragColor = rgb;
        gl_FragColor = vec4(ambient + magnitude*s, ambient + magnitude*s, ambient + magnitude*s, 1) * rgb;
    }
    else {
        gl_FragColor = vec4(0.0,0.0,0.0,1.0);
    }
}