#version 130

in vec4 a_position;
in vec4 a_color;
in vec2 a_texCoord0;
uniform vec2 resolution;
uniform mat4 u_projTrans;
uniform int upscaleFactor;
out vec4 v_color;
out vec2 v_texCoords;
out vec2 pos;
uniform float ratio;

void main()
{
	v_color = vec4(1, 1, 1, 1);
	v_texCoords = a_texCoord0;
	gl_Position =  u_projTrans * a_position;

	//	pos = vec2(a_position.x/resolution.x, a_position.y/resolution.y);

	//ensure midpoint at resolution/2
	vec2 mid = vec2(round((resolution.x)/(2.0/float(upscaleFactor))), round((resolution.y)/(2.0/float(upscaleFactor))));
	pos = vec2((a_position.x-mid.x)/(resolution.x), (a_position.y-mid.y)/(resolution.y));
}
