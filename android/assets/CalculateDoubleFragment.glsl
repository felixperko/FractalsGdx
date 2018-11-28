#ifdef GL_ES
precision highp double;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 pos;
uniform sampler2D u_texture;
uniform float iterations;
uniform sampler1D palette;
uniform vec2 center;
uniform float scale;
uniform float ratio;

vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main()
{
	gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
	//vec2 p = pos*8 - vec2(2,2);
	double resX = 0;
	double resY = 0;
	double cx = (pos.x - 0.5*ratio) * scale + center.x;
	double cy = (pos.y - 0.5) * scale + center.y;
	double resYSq = resY*resY;
	double resXSq = resX*resX;
	int i = 0;
	for (i ; i < iterations ; i++){
		resY = resX*resY*2 + cy;
		resX = resX*resX - resYSq + cx;
		resXSq = resX*resX;
		resYSq = resY*resY;
		if (resXSq + resYSq > 4.0)
			break;
	}
	if (resXSq + resYSq > 4.0){
		float l = log(i);
		vec3 hsv = vec3(mod(l, 1),1,1);
		vec3 rgb = hsv2rgb(hsv);
		gl_FragColor = vec4(rgb,1);
		//gl_FragColor = texture1D(palette, float(i) / 100.0);
	} else {
		gl_FragColor = vec4(0, 0, 0, 1);
	}
	//} else if (l < 0){
	//	gl_FragColor = vec4(1, 0, 0, 1);
	//} else {
	//	gl_FragColor = vec4(0, 0, 0, 1);
	//}
}
