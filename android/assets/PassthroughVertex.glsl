#version 320 es

in vec4 a_position;
in vec4 a_color;
in vec2 a_texCoord0;
uniform vec2 resolution;
uniform mat4 u_projTrans;
out vec4 v_color;
out vec2 v_texCoords;
out vec2 pos;
uniform float ratio;

void main()
{
	v_color = vec4(1, 1, 1, 1);
	v_texCoords = a_texCoord0;
	gl_Position =  u_projTrans * a_position;
	pos = vec2(a_position.x/resolution.x, a_position.y/resolution.y);
}
