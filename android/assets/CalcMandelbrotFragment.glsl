#ifdef GL_ES
precision highp float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 pos;
uniform sampler2D u_texture;
uniform int iterations;
//uniform sampler1D palette;
uniform vec2 center;
uniform float scale;
uniform float ratio;
uniform float biasReal;
uniform float biasImag;
uniform float samples;
uniform float flip;
uniform vec2 resolution;

const float log2 = log(2.0);
const float upperborder = 100.0;
const float lowerborder = 0.0;

uniform float burningship;
uniform float juliaset;


vec3 EncodeExpV3( in float value )
{
    int exponent  = int( log( abs( value ) )/log(2.0) + 1.0 );
    value        /= exp2( float( exponent ) );
    value         = (value + 1.0) * (256.0*256.0 - 1.0) / (2.0*256.0*256.0);
    vec3 encode   = fract( value * vec3(1.0, 256.0, 256.0*256.0) );
    return vec3( encode.xy - encode.yz / 256.0 + 1.0/512.0, (float(exponent) + 127.5) / 256.0 );
}

float DecodeExpV3( in vec3 pack )
{
    int exponent = int( pack.z * 256.0 - 127.0 );
    float value  = dot( pack.xy, 1.0 / vec2(1.0, 256.0) );
    value        = value * (2.0*256.0*256.0) / (256.0*256.0 - 1.0) - 1.0;
    return value * exp2( float(exponent) );
}

vec4 encode(in float value){
    //return EncodeRangeV4(value, lowerborder, upperborder);
    return vec4(EncodeExpV3(value), 1.0);
    //return EncodeExpV4(value);
}

float decode(in vec4 pixel){
    //return log(DecodeRangeV4(pixel, lowerborder, upperborder));
    return DecodeExpV3(vec3(pixel));
    //return DecodeExpV4(pixel);
}

void addValue(inout vec4 pixel, in float addValue){
    float oldValue = decode(pixel);
    //if (addValue < 0) //do nothing
    //    pixel = encode(oldValue);
    if (oldValue >= 0.0)
        pixel = encode(oldValue+addValue);
}

void main()
{
	//gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
	//vec2 p = pos*8 - vec2(2,2);

	float resX = biasReal;
	float resY = biasImag;
	float cx = (pos.x - 0.5*ratio) * scale + center.x;
	float cy = (((pos.y - 0.5) * scale) + center.y);

    if (juliaset > 0.0){
        resX = cx;
        resY = cy;
        cx = biasReal;
        cy = biasImag;
    }

	//float cx = biasReal;
	//float cy = biasImag;
	//float resX = (pos.x - 0.5*ratio) * scale + center.x;
	//float resY = (pos.y - 0.5) * scale + center.y;

	float resYSq = resY*resY;
	float resXSq = resX*resX;
	float resIterations = 0.0;
	for (int i = 0 ; i < iterations ; i++){
	    if (burningship > 0.0){
	        resX = abs(resX);
	        resY = abs(resY);
        }
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
		float smoothIterations = (resIterations + 1.0 - log(log(lSq)*0.5/log2)/log2);
        //gl_FragColor = encode(l);
        //addValue(gl_FragColor, l);
        //vec2 coords = v_texCoords;
        //if (flip == -1.0)
        //    coords.y = resolution.y - coords.y;
        vec4 lastColor = texture2D(u_texture, v_texCoords);
        float last = decode(lastColor);
        //addValue(gl_FragColor, l);
        //gl_FragColor = encode((smoothIterations)*(1.0/(samples)) + last*((samples-1.0)/(samples)));
        //addValue(gl_FragColor, smoothIterations);
        //if (last == 0.0)
        //if (lastColor.a > 0.0)
        gl_FragColor = encode(smoothIterations + last);
        //else
        //    gl_FragColor = encode(smoothIterations*samples);
        //if (samples == 1){
        //    gl_FragColor = encode(smoothIterations);
        //}
        //else {
        //    gl_FragColor = encode(smoothIterations*0.01 + last);
        //}
        //gl_FragColor = encode((l/100.0 + last*(99.0/100.0)));
        //gl_FragColor = encode(last*0.9 + smoothIterations*0.1);
	} else {
		gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
		//addValue(gl_FragColor, 0.0);
		//gl_FragColor = decode(texture2D(u_texture, v_texCoords)) + encode(0.0);
	}
}
