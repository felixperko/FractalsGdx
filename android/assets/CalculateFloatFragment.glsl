//#ifdef GL_ES
precision highp float;
//#endif
varying highp vec4 v_color;
varying highp vec2 v_texCoords;
varying highp vec2 pos;
uniform sampler2D u_texture;
uniform highp float iterations;
//uniform sampler1D palette;
uniform highp vec2 center;
uniform highp float scale;
uniform highp float ratio;
const highp float log2 = log(2.0);

highp vec3 hsv2rgb(vec3 c) {
  highp vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  highp vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main()
{
	gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
	//vec2 p = pos*8 - vec2(2,2);
	highp float resX = 0.0;
	highp float resY = 0.0;
	highp float cx = (pos.x - 0.5*ratio) * scale + center.x;
	highp float cy = (pos.y - 0.5) * scale + center.y;
	highp float resYSq = resY*resY;
	highp float resXSq = resX*resX;
	highp float i = 0.0;
	highp float resIterations = 0.0;
	for (i ; i < iterations ; i++){
	    //resX = abs(resX);
	    //resY = -abs(resY);
        resY = resX*resY*2.0 + cy;
        resX = resXSq - resYSq + cx;
        resXSq = resX*resX;
        resYSq = resY*resY;
		if (resXSq + resYSq > 65536.0)
		    break;
	}
	highp float lSq = resXSq + resYSq;
	if (lSq > 65536.0){
		highp float l = i + 1.0 - log(log(lSq)*0.5/log2)/log2;
		highp vec3 hsv = vec3(log(l)*0.5,0.6,1);
		highp vec3 rgb = hsv2rgb(hsv);
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
