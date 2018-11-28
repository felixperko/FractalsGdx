attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
uniform vec2 resolution;
uniform mat4 u_projTrans;
varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 pos;
uniform float ratio;

void main()
{
	v_color = vec4(1, 1, 1, 1);
	v_texCoords = a_texCoord0;
	gl_Position =  u_projTrans * a_position;
	pos = vec2(a_position.x*ratio/resolution.x, a_position.y/resolution.y);
}
