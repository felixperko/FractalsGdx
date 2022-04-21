//<VERSION>
#ifdef GL_ES
precision mediump float;
#endif
in vec4 v_color;
in vec2 v_texCoords;
layout(location = 0) out vec4 colour1;
uniform sampler2D u_texture;
void main()
{
	colour1 = v_color * texture(u_texture, v_texCoords);
}
