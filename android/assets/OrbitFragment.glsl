//<VERSION>

#ifdef GL_ES
precision mediump float;
#endif

in vec4 v_color;
in vec2 v_texCoords;
in vec2 pos;

layout(location = 0) out vec4 colour1;

layout(binding = 0) uniform sampler2D u_texture;
layout(binding = 1) uniform sampler2D orbitTexture;
uniform vec2 point0;
uniform vec2 midpoint;
uniform vec2 res;
uniform float zoom;
uniform int iterations;

float DecodeExpV3( in vec3 pack )
{
	float exponent = float(int(ceil(pack.z*256.0-128.0)));
	float g = ceil(pack.g*256.0);
	float sign = g / 128.0;
	if (sign >= 1.0)
		g -= 128.0;
	float value = ((ceil(pack.r*256.0)/256.0+(g*2.0)/(256.0*256.0))+1.0) * exp2(exponent+1.0);
	if (sign >= 1.0)
		value = -value;
	return (value) ;
}

void main()
{
	float ratio = res.x/res.y;
	float coordX = v_texCoords.x;
	float coordY = 1.0 - v_texCoords.y;

	colour1 = v_color * texture(u_texture, v_texCoords);

	vec2 ratioScaleVec = vec2(1.0/ratio, 1.0);
	vec2 shiftVec = vec2(0.5, 0.5);

	int x = 0;
	int y = 0;
	int hitColor = 0;
	for (int i = 0 ; i < iterations ; i++){
		float real = DecodeExpV3(texelFetch(orbitTexture, ivec2(x, y), 0).rgb);
		float imag = DecodeExpV3(texelFetch(orbitTexture, ivec2(x, y+1), 0).rgb);
		vec2 pointD = vec2(real, imag);
		vec2 pointScreenPos = ((pointD-midpoint)/zoom)*ratioScaleVec+shiftVec;
		float distX = (coordX-pointScreenPos.x)*ratio;
		float distY = (coordY-pointScreenPos.y);
		if (abs(distX) < 0.005 && abs(distY) < 0.005){
			hitColor++;
		}
		x++;
		if (x > 1023){
			x = 0;
			y += 2;
		}
	}
	if (hitColor > 0){
		colour1 = vec4(1.0, 0.0, 0.0, 1.0);
	}
}
