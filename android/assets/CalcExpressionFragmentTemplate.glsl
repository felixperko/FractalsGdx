#version 130
//#version 400 //double support
#ifdef GL_ES //not needed? (ignored by desktop GL)
precision highp float;
#endif
in vec4 v_color;
in vec2 v_texCoords;
in vec2 pos;
out vec4 colour1;
out vec4 colour2;
uniform sampler2D samplesTexture;
uniform sampler2D u_texture;
uniform vec2 bufferOffset;
uniform int discardBuffer;
uniform float iterations;
uniform float firstIterations;
uniform vec2 center;
//uniform vec2 centerFp64Low;
uniform float ratio;
uniform float samples;
uniform float limit;
uniform float logPow;
uniform float[] params;
uniform vec2 resolution;
uniform int sampleCountRoot;
uniform float maxBorderSamples;
uniform float maxSamplesPerFrame;

<FIELDS>

const float resultOffset = 10.0;
const float maxSamplesNotEscaped = 1.0;
const float firstItSkipDist = 20.0; //distance of "probe" pixels for which firstIterations is ignored

//const float notEscapedValue = 1.175494351e-38;
const float notEscapedValue = -10.0;
const float log2 = log(2.0);

uniform float burningship;
uniform float juliaset;

vec3 EncodeExpV3( in float value )
{
    int exponent  = int( ceil(log( abs( value ) )/log2) );
    //normalize to 0 ... 1
    float normalizedValue = value / exp2( float( exponent ) );
    //8 upper bits in r
    float r = floor(normalizedValue*256.0)/256.0;
    //8 lower bits in g
    float g = (normalizedValue-r)*256.0;
    //offset exponent in b
    float b = (float(exponent)+128.0) / 256.0;
    return vec3(r + 0.5/256.0, g + 0.5/256.0, b);
}

float DecodeExpV3( in vec3 pack )
{
    float exponent = float(int(pack.z*256.0-128.0));
    float value  = ((pack.x - 0.5/256.0)+(pack.y - 0.5/256.0)/256.0);
    return (value) * exp2(exponent+1.0) ;
}

vec4 encode(in float value){
    return vec4(EncodeExpV3(value + resultOffset), 1.0);
}

float decode(in vec4 pixel){
    return DecodeExpV3(vec3(pixel)) - resultOffset;
}

void make_kernel(inout float n[12], sampler2D tex, vec2 coord){
      n[0] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1, -1)+ivec2(bufferOffset.xy), 0)));
      n[1] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, -1)+ivec2(bufferOffset.xy), 0)));
      n[2] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, -1)+ivec2(bufferOffset.xy), 0)));
      n[3] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1,  0)+ivec2(bufferOffset.xy), 0)));
      n[4] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, 0)+ivec2(bufferOffset.xy), 0)));
      n[5] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1, 1)+ivec2(bufferOffset.xy), 0)));
      n[6] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, 1)+ivec2(bufferOffset.xy), 0)));
      n[7] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, 1)+ivec2(bufferOffset.xy), 0)));

      n[8] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 2, 0)+ivec2(bufferOffset.xy), 0)));
      n[9] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-2, 0)+ivec2(bufferOffset.xy), 0)));
      n[10] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, 2)+ivec2(bufferOffset.xy), 0)));
      n[11] = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, -2)+ivec2(bufferOffset.xy), 0)));

      //n[0] = -1.0;
      //n[2] = -1.0;
      //n[5] = -1.0;
      //n[7] = -1.0;
  }

vec4 encodeInt(in float value){
    float valueLocal = value;
    float r = float(int(mod(value, 128.0)))/256.0;
    float g = float(int(mod(value/128.0, 128.0)))/256.0;
//    float r = mod((value-mod(value, 256.0))/256.0, 256.0)/256.0;
//    float g = mod(value, 256.0)/256.0;
    return vec4(r, g, 0.0, 1.0);
}

float decodeInt(in vec4 pixel){
    return (pixel.r + pixel.g*128.0)*256.0;
//    return pixel.r*256.0;
//    return (pixel.r*256.0 + pixel.g)*256.0;
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

    float outputXSq = 0.0;
    float outputYSq = 0.0;
    float outputX = 0.0;
    float outputY = 0.0;
    float resIterations = 0.0;

    int maxSampleCount = sampleCountRoot;

    bool first = true;

    float moved = 0.0;

    ivec2 uv = ivec2(gl_FragCoord.xy)+ivec2(bufferOffset.xy);
    vec4 currentColor = texelFetch(u_texture, uv, 0);
    float currentValue = decode(currentColor);

    vec4 samplesColor = texelFetch(samplesTexture, uv, 0);
    float samples = decodeInt(samplesColor);

    if (discardBuffer == 1){
        samples = 0.0;
        currentValue = 0.0;
    }
    float n[12];
    make_kernel(n, u_texture, v_texCoords.xy);
    float frameSampleCount = min(maxSampleCount-samples, maxSamplesPerFrame);
    if (frameSampleCount <= 0 || (samples > 0.0 && currentValue < 0.0
             && n[0] <= resultOffset && n[1] <= resultOffset && n[2] <= resultOffset && n[3] <= resultOffset
             && n[4] <= resultOffset && n[5] <= resultOffset && n[6] <= resultOffset && n[7] <= resultOffset
             && n[8] <= resultOffset && n[9] <= resultOffset && n[10] <= resultOffset && n[11] <= resultOffset
             )){
        colour1 = currentColor;
    }
    else {

        float limitSq = limit*limit;

         //Padovan sequence for 2 dimensions
         //			g = 1.32471795724474602596
         //			a1 = 1.0/g
         //			a2 = 1.0/(g*g)
         //			x[n] = (0.5+a1*n) %1
         //			y[n] = (0.5+a2*n) %1
        float g = 1.32471795724474602596;
        float a1 = 1.0/g;
        float a2 = 1.0/(g*g);

        for (int s = 0 ; s < frameSampleCount ; s++){

            float sampleNo = samples;

            float sampleDeltaX = mod(0.5+a1*sampleNo*0.5, 1.0)/resolution.x;
            float sampleDeltaY = mod(0.5+a2*sampleNo*0.5, 1.0)/resolution.y;

            float deltaX = (pos.x - 0.5 + sampleDeltaX)*ratio;
            float deltaY = (pos.y - 0.5 + sampleDeltaY);

            //float deltaX = (pos.x - 0.5 + mod(sampleNo, sampleCountRoot)/(resolution.x*sampleCountRoot))*ratio;
            //float deltaY = (pos.y - 0.5 + (sampleNo / sampleCountRoot)/(resolution.y*sampleCountRoot));

            <INIT>

 //           float resYSq = 0.0;
 //           float resXSq = 0.0;
            float lastR = 0.0;
            float lastI = 0.0;

            float loopIterations = notEscapedValue;

            float trapX = 0.1;
            float trapY = 0.25;
            float trapRadius = 0.01;

            float requestedIterations = maxSampleCount > 1 && sampleNo == 0.0
             //&& (!(mod(gl_FragCoord.x, firstItSkipDist) <= 1.0 && mod(gl_FragCoord.y, firstItSkipDist) <= 1.0)
             //&& gl_FragCoord.x > 1
             //&& !(gl_FragCoord.x == resolution.x-1 && mod(gl_FragCoord.y, firstItSkipDist) >= 1.0)
             //&& gl_FragCoord.y > 1
             //&& !(gl_FragCoord.y == resolution.y-1 && mod(gl_FragCoord.x, firstItSkipDist) >= 1.0)
             //)
             ? firstIterations*iterations : iterations;

            for (float i = 0.0 ; i < requestedIterations ; i++){

                <ITERATE>

 //               resXSq = float(local_0*local_0);
 //               resYSq = float(local_1*local_1);

                //float movedNow = sqrt(resXSq+resYSq)/maxSampleCount;
                //moved += movedNow;

                if (<CONDITION>
                ){
                    loopIterations = float(i + 5.0 - log(log(local_0*local_0+local_1*local_1)*0.5/log2)/(logPow));
                    break;
                }
            }

            if (loopIterations == notEscapedValue){
                samples += 1.0;
                if (currentValue <= 0.0){
                    if (sampleNo < maxSamplesNotEscaped-1.0)
                        continue;
                    if (sampleNo >= maxBorderSamples-1.0)
                        break;
                }
            } else {
                samples += 1.0;
                float contrib = 1.0/(1.0+sampleNo);
                if (sampleNo <= 1.0 || currentValue < 0.0)
                    currentValue = loopIterations;
                else
                    currentValue = currentValue*(1.0-contrib) + loopIterations*contrib;
            }
        }

        if (currentValue > 0.0){
            colour1 = encode(currentValue);
        } else {
            colour1 = encode(notEscapedValue);
        }
    }
    colour2 = encodeInt(samples);
}
