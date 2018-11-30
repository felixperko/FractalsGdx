#ifdef GL_ES
precision highp float;
#endif
uniform sampler2D u_texture;
varying vec2 v_texCoords;
uniform vec2 resolution;
const float ambient = 0.3;
const float magnitude = 0.3;

void make_kernel(inout vec4 n[9], sampler2D tex, vec2 coord){
    float w = 1.0/resolution.x;
    float h = 1.0/resolution.y;

    n[0] = texture2D(tex, coord + vec2(-w, -h));
    n[1] = texture2D(tex, coord + vec2(0.0, -h));
    n[2] = texture2D(tex, coord + vec2(w, -h));
    n[3] = texture2D(tex, coord + vec2(-w, 0.0));
    n[4] = texture2D(tex, coord);
    n[5] = texture2D(tex, coord + vec2(w, 0.0));
    n[6] = texture2D(tex, coord + vec2(-w, h));
    n[7] = texture2D(tex, coord + vec2(0.0, h));
    n[8] = texture2D(tex, coord + vec2(w, h));
}

void main(void){

    vec4 n[9];

    make_kernel(n, u_texture, v_texCoords.xy);

    vec4 sobel_edge_h = n[2] + (2.0*n[5]) + n[8] - (n[0] + (2.0*n[3]) + n[6]);
    vec4 sobel_edge_v = n[0] + (2.0*n[1]) + n[2] - (n[6] + (2.0*n[7]) + n[8]);
    vec4 sobel = sqrt((sobel_edge_h*sobel_edge_h) + (sobel_edge_v*sobel_edge_v));

    //gl_FragColor = texture2D(u_texture, v_texCoords);
    //gl_FragColor = vec4(0.3 + 0.3*sobel.rgb, 1.0)*texture2D(u_texture, v_texCoords);
    float s = sobel.x + sobel.y + sobel.z;
    s = s;
    if (s > 1)
        s = 1;
    gl_FragColor = vec4(ambient + magnitude*s, ambient + magnitude*s, ambient + magnitude*s, 1) * texture2D(u_texture, v_texCoords);
    //gl_FragColor = vec4(0.0,0.0,1.0,1.0);
}