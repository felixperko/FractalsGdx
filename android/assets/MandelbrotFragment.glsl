#ifdef GL_ES
precision highp float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 pos;
uniform sampler2D u_texture;
uniform int iterations;
uniform float colorShift;
//uniform sampler1D palette;
uniform vec2 center;
uniform float scale;
uniform float ratio;
uniform float biasReal;
uniform float biasImag;
const float log2 = log(2.0);

vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main()
{
	gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
	//vec2 p = pos*8 - vec2(2,2);
	float resX = biasReal;
	float resY = biasImag;
	float cx = (pos.x - 0.5*ratio) * scale + center.x;
	float cy = (pos.y - 0.5) * scale + center.y;
	float resYSq = resY*resY;
	float resXSq = resX*resX;
	float resIterations = 0.0;
	for (int i = 0 ; i < iterations ; i++){
	    //resX = abs(resX);
	    //resY = abs(resY);
        resY = resX*resY*2.0 + cy;
        resX = resXSq - resYSq + cx;
        resXSq = resX*resX;
        resYSq = resY*resY;
		if (resXSq + resYSq > 65536.0){
		    resIterations = float(i);
		    break;
        }
	}
	float lSq = resXSq + resYSq;
	if (lSq > 65536.0){
		float l = log(resIterations + 1.0 - log(log(lSq)*0.5/log2)/log2)*0.5+colorShift;
		vec3 hsv = vec3(log(l)*0.5,0.6,1);
		vec3 rgb = hsv2rgb(hsv);
	    gl_FragColor = vec4(rgb,1);
		//gl_FragColor = texture1D(palette, float(i) / 10.0);
		vec2 coords = vec2(fract(l),0);
        gl_FragColor = texture2D(u_texture, coords);
	} else {
		gl_FragColor = vec4(0, 0, 0, 1);
	}
}
