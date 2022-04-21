//aggregrates requested data in output pixels
//[planning] output modes: average result, average deviation, min samples, average iterations,...
//[planning] input modes: raw data, previous reduce iteration
//[planning] scaling modes:
//- 4x4 -> 2x2
//- 2x2 -> 1
//- 8x8 -> 4x4 ?

//<VERSION>
#ifdef GL_ES //not needed? (ignored by desktop GL)
precision highp float;
precision highp int;
#endif

in vec4 v_color;
in vec2 v_texCoords;
in vec2 pos;
layout(location = 0) out vec4 colorOut0;
layout(location = 2) out vec4 colorOut1;
//layout(location = 2) out vec4 colorOut2;
uniform sampler2D texture0;
uniform sampler2D texture1;
//0: int, 1: float
uniform int inputDataType;
//0: 2x2 -> 1
uniform int scalingMode;
//0: sum, 1: average, 2: min, 3: max
uniform int outputMode;
//1: substract the blue channel value (8 bit)
uniform int maxB;
uniform float minBorder;

const float resultOffset = 10.0;
const float log2 = log(2.0);

float decodeInt(in vec4 pixel){
    return (pixel.r + floor(pixel.g*256.0))*256.0;
}

vec4 encodeInt(in float value){
    float valueLocal = value;
    float g = floor(value/256.0)/256.0;
    float r = mod(value, 256.0)/256.0;
    return vec4(r, g, 0.0, 1.0);
}

vec3 EncodeExpV3( in float value )
{
    int exponent  = int( ceil(log( abs( value ) )/log2) );
    //normalize to 0 ... 1
    float normalizedValue = value / exp2( float( exponent ) );
    //8 upper bits in r
    float r = floor(normalizedValue*256.0)/256.0;
    //8 lower bits in g
    float g = floor((normalizedValue-r)*256.0*256.0)/256.0;
    //offset exponent in b
    float b = (float(exponent)+128.0) / 256.0;
    return vec3(r-0.5/256.0, g-0.5/256.0, b);
}

vec4 encode(in float value){
    return vec4(EncodeExpV3(value + resultOffset), 1.0);
}

float DecodeExpV3( in vec3 pack )
{
    float exponent = float(int(pack.z*256.0-128.0));
    float value  = ((ceil(pack.r*256.0)/256.0+ceil(pack.g*256.0)/(256.0*256.0)));
    return value * exp2(exponent+1.0);
}

float decode(in vec4 pixel){
    return DecodeExpV3(vec3(pixel)) - resultOffset;
}

vec4 encodeIntB(in vec4 pixel, in float value){
    pixel.b = value/256.0;
    return pixel;
}

float decodeIntB(in vec4 pixel){
    return (pixel.b)*256.0;
}

void main()
{

    ivec2 uv = ivec2(gl_FragCoord.xy);
//        vec4 currentColor = texelFetch(texture0, uv, 0);
//    ivec2 uvLastTex = ivec2(uv.x*2, uv.y*2);
//    vec4 currentColor = texelFetch(texture0, uvLastTex, 0);
    vec4 currentColor = encodeInt(200.0);
    vec4 outputColor = currentColor;

//    vec4 extraColor = vec4(0.0, 0.0, 0.0, 1.0);

    int maxValue = 10000;
    float outputVal = outputMode == 2 ? float(maxValue) : 0.0;
    float outputVal2 = 0.0;
//    float extraVal = 0.0;

    ivec2 texture0Size = textureSize(texture0, 0);

//    float filteredProportion = 0.0;

    if (inputDataType == 0 || inputDataType == 1){ //int
        if (scalingMode == 0){ //2x2 -> 1
//            if (outputMode == 2) //min
//                outputVal = float(max);
            for (int xs = 0 ; xs < 2 ; xs++){
                for (int ys = 0 ; ys < 2 ; ys++){
                    ivec2 sampleCoords = ivec2(uv.x*2+xs, uv.y*2+ys);
////                    ivec2 sampleCoords = ivec2(uv.x, uv.y);
                    if (sampleCoords.x/2 <= uv.x && sampleCoords.y/2 <= uv.y){
                        vec4 color = texelFetch(texture0, sampleCoords, 0);
                        vec4 color2 = texelFetch(texture1, sampleCoords, 0);
                        float val = inputDataType == 0 ? decodeInt(color) : decode(color);
                        float val2 = inputDataType == 0 ? decodeInt(color2) : decode(color2);
                        outputVal2 += val2;
//                        if (inputDataType == 1){
//                            filteredProportion += val <= 1.0 ? 0.25 : 0.25*min(decodeIntB(color)/val, 0.0);
//                        }
                        val = maxB == 1 ? max(val, decodeIntB(color)) : val;
                        if (outputMode == 0 || outputMode == 1){ //sum, average
                            outputVal += val;
                        }
                        else if (outputMode == 2){ //min
                            if (val < outputVal && val > minBorder)
                                outputVal = val;
                            else
                                outputVal = outputVal;
                        }
                        else if (outputMode == 3){ //max
                            if (val > outputVal)
                                outputVal = val;
                            else
                                outputVal = outputVal;
                        }
                    }
//                    else {
//                        outputVal = 200.0;
//                    }
//                    outputVal = 100.0*float(xs)+50.0*float(ys);
                }
            }
            if (outputMode == 1){ //average
                outputVal /= 4.0;
//                if (inputDataType == 1){
//                    extraColor = vec4(0.0, 0.0, min(1.0,filteredProportion), 1.0);
//                }
            }
            outputVal2 /= 4.0;
//            outputColor = encodeInt(outputVal);
        }
    }

    colorOut0 = inputDataType == 0 ? encodeInt(outputVal) : encode(outputVal);
    colorOut1 = inputDataType == 0 ? encodeInt(outputVal2) : encode(outputVal2);
//    colorOut2 = extraColor;
//    colorOut0 = encodeInt(200.0);
}