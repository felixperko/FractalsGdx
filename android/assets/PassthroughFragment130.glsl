#version 130
#ifdef GL_ES
precision mediump float;
#endif

in vec4 v_color;
in vec2 v_texCoords;
out vec4 colour1;
uniform sampler2D u_texture;
void main()
{
	colour1 = v_color * texture(u_texture, v_texCoords);
}
