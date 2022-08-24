//<VERSION>
#ifdef GL_ES //not needed? (ignored by desktop GL)
precision highp float;
precision highp int;
#endif

#define PI 3.1415926535897932384626433832795

in vec4 v_color;
in vec2 v_texCoords;

layout(location = 0) out vec4 colour1; //escape time
layout(location = 1) out vec4 colour2; //samples, failedSamples
layout(location = 2) out vec4 colour3; //movedDistance/avgAngle

layout(binding = 0) uniform sampler2D currTexture;
layout(binding = 1) uniform sampler2D escapeTimeTexture;
layout(binding = 2) uniform sampler2D samplesTexture;
layout(binding = 3) uniform sampler2D missingSamplesTexture;
layout(binding = 4) uniform sampler2D referenceTexture;

uniform vec2 bufferOffset;
uniform int discardBuffer;
uniform int extractRemainingSamples;
uniform float iterations;
uniform float firstIterations;

//0: movedDistance, 1: avgAngle
uniform int colour3Output;

uniform vec2 center;
//uniform vec2 centerFp64Low;
in vec2 pos;

uniform vec2 resolution;
uniform float ratio;
uniform float samples;
uniform float limit;
uniform float smoothstepScaling;
uniform float smoothstepShift;
uniform int sampleCount;
uniform int maxSampleCount;
uniform float maxBorderSamples;
uniform float maxSamplesPerFrame;
uniform float gridFrequency;
uniform float moduloFrequency;

uniform vec2 cRef;

uniform int upscaleFactor;
uniform vec2 upscaleShift;

//patternX patternY divs
uniform vec3 selectParams;

//uniform float[1] params;
//<FIELDS>

const float resultOffset = 10.0;
const float maxSamplesNotEscaped = 1.0;
//const float firstItSkipDist = 20.0; //distance of "probe" pixels for which firstIterations is ignored

//const float notEscapedValue = 1.175494351e-38;
const float notEscapedValue = -10.0;
const float log2 = log(2.0);
const float movedLimit = 16.0;

const float splitFactor = pow(2.0, 16.0)+1.0;

const float REF_TEX_WIDTH = 1024.0;

uniform float burningship;
uniform float juliaset;

float DecodeFloatSignedV3( in vec3 pack )
{
    float exponent = float(int(ceil(pack.z*256.0-128.0)));
    float g = ceil(pack.g*256.0);
    float sign = g / 128.0;
    float value = ((ceil(pack.r*256.0)/256.0+(mod(g, 128.0)*2.0)/(256.0*256.0))+1.0) * exp2(exponent+1.0);
    if (sign >= 1.0)
        return -value;
    return value;
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

vec4 EncodeExpV4( in float value )
{
    int exponent  = int( ceil(log( abs( value ) )/log2) );
    //normalize to 0 ... 1
    float normalizedValue = value / exp2( float( exponent ) );
    //8 upper bits in r
    float r = floor(normalizedValue*256.0)/256.0;
    //8 lower bits in g
    float g = floor((normalizedValue-r)*256.0*256.0)/256.0;
    float a = floor(((normalizedValue-r)*256.0-g)*256.0*256.0)/256.0;
//    float a = 1.0;
    //offset exponent in b
    float b = (float(exponent)+128.0) / 256.0;
    return vec4(r-0.5/256.0, g-0.5/256.0, b, a-0.5/256.0);
}

vec4 encode(in float value){
    return vec4(EncodeExpV3(value + resultOffset), 1.0);
//    return EncodeExpV4(value + resultOffset);
}

float DecodeExpV3( in vec3 pack )
{
    float exponent = float(int(pack.z*256.0-128.0));
    float value  = ((ceil(pack.r*256.0)/256.0+ceil(pack.g*256.0)/(256.0*256.0)));
    return value * exp2(exponent+1.0);
}

float DecodeExpV4( in vec4 pack )
{
    float exponent = float(int(pack.b*256.0-128.0));
    float value  = ((ceil(pack.r*256.0)/256.0+ceil(pack.g*256.0)/(256.0*256.0)));
    return value * exp2(exponent+1.0);
}

float decode(in vec4 pixel){
    return DecodeExpV3(vec3(pixel)) - resultOffset;
//    return DecodeExpV4(pixel)-resultOffset;
}

void make_kernel(inout float n[12], sampler2D tex, vec2 coord){
      n[0] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1, -1)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));
      n[1] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, -1)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));
      n[2] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, -1)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));
      n[3] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1,  0)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));
      n[4] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, 0)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));
      n[5] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1, 1)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));
      n[6] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, 1)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));
      n[7] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, 1)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));

      n[8] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 2, 0)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));
      n[9] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-2, 0)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));
      n[10] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, 2)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));
      n[11] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, -2)+ivec2(bufferOffset.xy)*upscaleFactor, 0)));

      //n[0] = -1.0;
      //n[2] = -1.0;
      //n[5] = -1.0;
      //n[7] = -1.0;
  }

vec4 encodeInt(in float value){
    float valueLocal = value;

    float g = floor(value/256.0)/256.0;
    float r = mod(value, 256.0)/256.0;

//    float r = float(int(mod(value, 128.0)))/256.0;
//    float g = float(int(mod(value/128.0, 128.0)))/256.0;

//    float r = mod((value-mod(value, 256.0))/256.0, 256.0)/256.0;
//    float g = mod(value, 256.0)/256.0;
    return vec4(r, g, 0.0, 1.0);
}

float decodeInt(in vec4 pixel){
    return (pixel.r + floor(pixel.g*256.0))*256.0;
}

vec4 encodeInt8b(in vec4 pixel, in float value){
    pixel.b = value/256.0;
    return pixel;
}

float decodeInt8b(in vec4 pixel){
    return (pixel.b)*256.0;
}

void addValue(inout vec4 pixel, in float addValue){
    float oldValue = decode(pixel);
    //if (addValue < 0) //do nothing
    //    pixel = encode(oldValue);
    if (oldValue >= 0.0)
    pixel = encode(oldValue+addValue);
}

void splitF(out float hi, out float lo, in float val) {

//    hi = val;

    float temp = splitFactor*val;
    float temp2 = temp-val;
    hi = temp-temp2;
    lo = val-hi;
}

void addFloatFloat(inout float ah, inout float al, in float bh, in float bl) {

    float r = ah + bh;
    float s = 0.0;
    if (abs(ah) >= abs(bh)) {
        s = (((ah-r)+bh)+bl)+al;
    } else {
        s = (((bh-r)+ah)+al)+bl;
    }
    float s2 = r+s;
    float v = s2-r;
    float r2 = (r-(s2-v))+(s-v);
    ah = s2;
    al = r2;
}

//https://blog.cyclemap.link/2011-06-09-glsl-part2-emu/
vec2 ds_set(float a)
{
    vec2 z;
    z.x = a;
    z.y = 0.0;
    return z;
}

vec2 ds_add (vec2 dsa, vec2 dsb)
{
    vec2 dsc;
    float t1, t2, e;

    t1 = dsa.x + dsb.x;
    e = t1 - dsa.x;
    t2 = ((dsb.x - e) + (dsa.x - (t1 - e))) + dsa.y + dsb.y;

    dsc.x = t1 + t2;
    dsc.y = t2 - (dsc.x - t1);
    return dsc;
}

vec2 ds_mul (vec2 dsa, vec2 dsb)
{
    vec2 dsc;
    float c11, c21, c2, e, t1, t2;
    float a1, a2, b1, b2, cona, conb, split = 8193.;

    cona = dsa.x * split;
    conb = dsb.x * split;
    a1 = cona - (cona - dsa.x);
    b1 = conb - (conb - dsb.x);
    a2 = dsa.x - a1;
    b2 = dsb.x - b1;

    c11 = dsa.x * dsb.x;
    c21 = a2 * b2 + (a2 * b1 + (a1 * b2 + (a1 * b1 - c11)));

    c2 = dsa.x * dsb.y + dsa.y * dsb.x;

    t1 = c11 + c2;
    e = t1 - c11;
    t2 = dsa.y * dsb.y + ((c2 - e) + (c11 - (t1 - e))) + c21;

    dsc.x = t1 + t2;
    dsc.y = t2 - (dsc.x - t1);

    return dsc;
}


void multFloatFloat(inout float ah, inout float al, in float bh, in float bl) {

//    ah = ah * bh;

//    float temp = ah*bh;
//
//    float temp2 = splitFactor*ah;
//    float temp3 = temp2-ah;
//    float temp4 = temp2-temp3;
//    float temp5 = ah-temp4;
//
//    temp2 = splitFactor*bh;
//    temp3 = temp2-bh;
//    float temp6 = temp2-temp3;
//    float temp7 = bh-temp6;
//
//    temp2 = temp - temp4*temp6;
//    temp3 = temp2 - temp5*temp6;
//    temp2 = temp3 - temp4*temp7;
//
//    temp3 = ah*bl + al*bh + temp5*temp7 - temp2;
//    ah = temp+temp3;
//    temp2 = ah-temp;
//    al = (temp-(ah-temp2))+(temp3-temp2);

    float x = ah*bh;
    float av0 = 0.0;
    float av1 = 0.0;
    float bv0 = 0.0;
    float bv1 = 0.0;
    splitF(av0, av1, ah);
    splitF(bv0, bv1, bh);
    float err1 = x - (av0*bv0);
    float err2 = err1 - (av1*bv0);
    float err3 = err2 - (av0*bv1);
    float y = av1*bv1 - err3;

    float t2 = ((ah*bl)+(al*bh)) + y;

    float s = x+t2;
    float v = s-x;
    float r = (x-(s-v))+(t2-v);

    ah = s;
    al = r;
}

void main()
{

    bool first = true;

//    ivec2 uv = ivec2(gl_FragCoord.xy+bufferOffset.xy);
//    ivec2 uv = ivec2(floor(gl_FragCoord.x+selectParams.x) + floor(bufferOffset.x), floor(gl_FragCoord.y+selectParams.y) + floor(bufferOffset.y));
    ivec2 uv = ivec2(floor((gl_FragCoord.x*float(upscaleFactor)+floor(bufferOffset.x) - float(upscaleFactor)*0.5)) + upscaleShift.x,
                     floor((gl_FragCoord.y*float(upscaleFactor)+floor(bufferOffset.y) - float(upscaleFactor)*0.5)) + upscaleShift.y);
    vec4 currentColor = texelFetch(escapeTimeTexture, uv, 0);
    float currentValue = decode(currentColor);

    vec4 samplesColor = texelFetch(samplesTexture, uv, 0);
    float samples = decodeInt(samplesColor);
    float failedSamples = decodeInt8b(samplesColor);

//    vec4 remainingSamplesColor = texelFetch(missingSamplesTexture, uv, 0);
//    float remainingSamples = decodeInt(remainingSamplesColor);
    float remainingSamples = float(maxSampleCount)-samples;

    vec4 extraColor = texelFetch(missingSamplesTexture, uv, 0);
    float extraValue = decode(extraColor);

    if (discardBuffer == 1){
        samples = 0.0;
        failedSamples = 0.0;
        currentValue = 0.0;
        extraValue = 0.0;
    }

    float n[12];
    make_kernel(n, escapeTimeTexture, v_texCoords.xy);
    float frameSampleCount = float(sampleCount)-samples;
    if (samples > 0.0)
        frameSampleCount = min(frameSampleCount, remainingSamples);
    frameSampleCount = min(frameSampleCount, maxSamplesPerFrame);
    int frameSampleCountInt = int(frameSampleCount);
//    if (extractRemainingSamples > 0)
//    colour3 = colour3;
//    colour3 = encodeInt(remainingSamples);

    //calculate a new sample or use previous value if sample is culled/not requested
    float neighbourSum = n[0]+n[1]+n[2]+n[3]+n[4]+n[5]+n[6]+n[7]+n[8]+n[9]+n[10]+n[11];
    int culled = (samples >= maxBorderSamples && neighbourSum <= 0.0) ? 1 : 0;
//    int culled = 0;
    if (discardBuffer == 0 && (frameSampleCountInt == 0 || culled == 1)){
        colour1 = currentColor;
        colour2 = encodeInt8b(encodeInt(samples), culled == 1 ? float(maxSampleCount) : failedSamples);
        colour3 = extraColor;
//        float br = mod(movedValue, 1.0);
//        colour3 = vec4(br,br,br,1.0);
//        discard;
    }
    else {
        if (failedSamples > samples)
            failedSamples = samples;
        float limitSq = limit*limit;

        float moduloFrequency2 = moduloFrequency+moduloFrequency;

         //Padovan sequence for 2 dimensions
         //			g = 1.32471795724474602596
         //			a1 = 1.0/g
         //			a2 = 1.0/(g*g)
         //			x[n] = (0.5+a1*n) %1
         //			y[n] = (0.5+a2*n) %1
        float g = 1.32471795724474602596;
        float a1 = 1.0/g;
        float a2 = 1.0/(g*g);

        float f = float(upscaleFactor);

        for (int s = 0 ; s < frameSampleCountInt ; s++){

            float sampleNo = samples;

            float sampleDeltaX = (mod(0.5+a1*sampleNo*0.5, 1.0))/(resolution.x*f);
            float sampleDeltaY = (mod(0.5+a2*sampleNo*0.5, 1.0))/(resolution.y*f);

            float deltaX = (pos.x/f + sampleDeltaX)*ratio;
            float deltaY = (pos.y/f + sampleDeltaY);

            float deltaXh = 0.0;
            float deltaXl = 0.0;
            float deltaYh = 0.0;
            float deltaYl = 0.0;
            splitF(deltaXh, deltaXl, deltaX);
            splitF(deltaYh, deltaYl, deltaY);

//<INIT>

            float requestedIterations = maxSampleCount > 1 && sampleNo == 0.0
                                        ? firstIterations*iterations : iterations;
            float loopIterations = notEscapedValue;
            float lastValueScale = 0.0;

            float moved = 0.0;
            float avgR = 0.0;
            float avgI = 0.0;

            for (float i = 0.0 ; i < requestedIterations ; i++){

//<ITERATE>

                float resXSq = float(local_0*local_0);
                float resYSq = float(local_1*local_1);

//                if (colour3Output == 0)
//                    moved += min(resXSq+resYSq, movedLimit);
//                if (colour3Output == 1){
//                    avgR += float(local_0)*100.0;
//                    avgI += float(local_1)*100.0;
//                }

                if (
true//<CONDITION>
                ){
                    lastValueScale = log(sqrt(resXSq+resYSq))*0.5;
                    loopIterations = float(i) + 5.0 - log(lastValueScale/log2+smoothstepShift)/smoothstepScaling;
                    break;
                }
            }

            samples += 1.0;

//            avgR /= requestedIterations;
//            avgI /= requestedIterations;

            float contrib = 1.0/(1.0+sampleNo);
            if (colour3Output == 0){
                moved = moved*1000.0/iterations;
//                moved = moved*moved;
            }
            if (colour3Output == 1)
                moved = (1.0+(atan(avgI, avgR))/(2.0*PI))*1000.0;
            extraValue = extraValue*(1.0-contrib) + (moved)*contrib;
//            extraValue = moved;

            if (loopIterations == notEscapedValue){
                failedSamples += 1.0;
                if (currentValue == 0.0 && sampleNo < maxSamplesNotEscaped-1.0)
                    continue;
                if (currentValue == 0.0 && sampleNo >= maxBorderSamples-1.0)
                    break;
//                else {
//                if (currentValue != 0.0)
//                    loopIterations = currentValue;
//                }
            }
            if (loopIterations >= 0.0){
//                loopIterations += 5.0 - log(lastValueScale/log2+smoothstepShift)/smoothstepScaling;
                currentValue = currentValue*(1.0-contrib) + loopIterations*contrib;
            }
        }

        colour1 = currentValue > 0.0 ? encode(currentValue) : encode(notEscapedValue);
        colour2 = encodeInt8b(encodeInt(samples), min(failedSamples, 255.0));
        colour3 = encode(extraValue);
    }
}
