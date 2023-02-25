#version 130
#ifdef GL_ES
precision highp float;
#endif

in vec4 v_color;
in vec2 v_texCoords;
in vec2 pos;

out vec4 colour0;
out vec4 colour1;

uniform sampler2D texture0;
uniform sampler2D texture1;

uniform vec4 color;
uniform int channel;

void main()
{
    if (channel == 0){
//        colour0 = color;
        colour0 = vec4(1.0, 1.0, 1.0, 1.0/8.0);
        colour1 = (texelFetch(texture1, ivec2(gl_FragCoord.x, gl_FragCoord.y), 0));
    }
    if (channel == 1){
        colour0 = (texelFetch(texture0, ivec2(gl_FragCoord.x, gl_FragCoord.y), 0));
        colour1 = vec4(1.0, 1.0, 1.0, 1.0/8.0);
//        colour1 = color;
    }
}
