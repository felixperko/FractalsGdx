//<VERSION>
#define FLT_MIN 1.175494351e-38
#ifdef GL_ES
precision highp float;
#endif
uniform sampler2D u_texture;
//uniform sampler2D extraTexture;
uniform sampler2D altColorTexture;
uniform sampler2D paletteEscaped;
uniform sampler2D paletteFallback;
uniform int usePalette;
uniform int usePalette2;
in vec2 v_texCoords;
layout(location = 0) out vec4 colour1;
uniform float light_ambient;
uniform float light_ambient2;
uniform float light_sobel_magnitude;
uniform float light_sobel_magnitude2;
uniform float light_sobel_period;
uniform float light_sobel_period2;
uniform float colorAdd;
uniform float colorAdd2;
uniform float colorMult;
uniform float colorMult2;
uniform float colorSaturation;
uniform float colorSaturation2;
uniform int extractChannel;
uniform float mappingColorR;
uniform float mappingColorG;
uniform float mappingColorB;
uniform vec2 resolution;
uniform int sobelSpan;

const float resultOffset = 10.0;
const int kernelRadius = 2;
const int kernelDim = (kernelRadius*2+1);
const int kernelLength = kernelDim*kernelDim;
const int kernelMidIndex = kernelLength/2;

float DecodeExpV3( in vec3 pack )
{
    float exponent = float(int(pack.z*256.0-128.0));
    float value  = ((ceil(pack.r*256.0)/256.0+ceil(pack.g*256.0)/(256.0*256.0)));
    return value * exp2(exponent+1.0);
}

//float DecodeExpV3( in vec3 pack )
//{
//    int exponent = int( pack.z * 256.0 ) ;
//    float value  = dot( pack.xy, (1.0) / vec2(1.0, 256.0) ) + 1.0;
////    value        = ((value) * (256.0*256.0) / (256.0*256.0) );
//    return value * exp2( float(exponent) );
//}

vec3 hsv2rgb(vec3 c) {
  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float decode(in vec4 pixel){
    return DecodeExpV3(vec3(pixel)) - resultOffset;
}

void make_kernel(inout float n[kernelLength], sampler2D tex, vec2 coord, int logScalingEnabled){
//    float mid = decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0));
    //TODO generate at compile time (ShaderBuilder)
//    for (int y = -sobelSpan ; y <= sobelSpan ; y++){
//        for (int x = -sobelSpan ; x <= sobelSpan ; x++){
//            float val = max(0.0, decode(texelFetch(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y)+ivec2(x, y), 0)));
//            if (val == 0.0)
//                val = mid;
//            else if (logScalingEnabled == 1)
//                val = log(val);
//            n[(y+kernelRadius)*kernelDim+(x+kernelRadius)] = val;
//        }
//    }

    //texelFetchOffset(gsampler sampler, ivec texCoord, int lod, ivec offset);
//    n[0] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-1, -1))));
//    n[1] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 0, -1))));
//    n[2] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 1, -1))));
//    n[3] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-1,  0))));
//    n[4] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 0,  0))));
//    n[5] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 1,  0))));
//    n[6] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-1,  1))));
//    n[7] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 0,  1))));
//    n[8] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 1,  1))));

    n[0] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-2, -2))));
    n[1] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-1, -2))));
    n[2] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 0, -2))));
    n[3] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 1, -2))));
    n[4] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 2, -2))));
    n[5] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-2, -1))));
    n[6] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-1, -1))));
    n[7] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 0, -1))));
    n[8] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 1, -1))));
    n[9] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 2, -1))));
    n[10] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-2,  0))));
    n[11] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-1,  0))));
    n[12] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 0,  0))));
    n[13] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 1,  0))));
    n[14] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 2,  0))));
    n[15] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-2,  1))));
    n[16] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-1,  1))));
    n[17] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 0,  1))));
    n[18] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 1,  1))));
    n[19] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 2,  1))));
    n[20] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-2,  2))));
    n[21] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2(-1,  2))));
    n[22] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 0,  2))));
    n[23] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 1,  2))));
    n[24] = max(0.0, decode(texelFetchOffset(tex, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0, ivec2( 2,  2))));
}

void main(void){


    float n[kernelLength];
    float n2[kernelLength];

//    float d = decode(texture2D(u_texture, v_texCoords.xy));
    float d = decode(texelFetch(u_texture, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0));

//    vec4 extraColor = texelFetch(extraTexture, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0);
//    float dim = useExtraData * extraColor.b;

    float value2 = decode(texelFetch(altColorTexture, ivec2(gl_FragCoord.x, resolution.y-gl_FragCoord.y), 0));

    vec4 rgb = vec4(0.0, 0.0, 0.0, 1.0);
//    colour1 = rgb;
    float sobel = 0.0;
    if (light_sobel_magnitude != 0.0){
        make_kernel(n, u_texture, v_texCoords.xy, 1);
        float sobel_edge_h = n[4] + (4.0*n[9]) + (6.0*n[14]) + (4.0*n[19]) + n[24] - (n[0] + (4.0*n[5]) + (6.0*n[10]) + (4.0*n[15]) + n[20]);
        float sobel_edge_v = n[0] + (4.0*n[1]) + (6.0*n[2]) + (4.0*n[3]) + n[4] - (n[20] + (4.0*n[21]) + (6.0*n[22]) + (4.0*n[23]) + n[24]);
        float sobel_edge_h2 = (2.0*n[3]) + (8.0*n[8]) + (12.0*n[13]) + (8.0*n[18]) + (2.0*n[23]) - ((2.0*n[1]) + (8.0*n[6]) + (12.0*n[11]) + (8.0*n[16]) + (2.0*n[21]));
        float sobel_edge_v2 = (2.0*n[5]) + (8.0*n[6]) + (12.0*n[7]) + (8.0*n[8]) + (2.0*n[9]) - ((2.0*n[15]) + (8.0*n[16]) + (12.0*n[17]) + (8.0*n[18]) + (2.0*n[19]));
        sobel = sqrt(((sobel_edge_h*sobel_edge_h)+(sobel_edge_h2*sobel_edge_h2) + (sobel_edge_v*sobel_edge_v)+(sobel_edge_v2*sobel_edge_v2))/(12.0*12.0));
    }
    float sobel2 = 0.0;
    if (light_sobel_magnitude2 != 0.0 && d <= 0.0){
        make_kernel(n2, altColorTexture, v_texCoords.xy, 1);
        //        float sobel_edge_h = n[2] + (2.0*n[5]) + n[8] - (n[0] + (2.0*n[3]) + n[6]);
        //        float sobel_edge_v = n[0] + (2.0*n[1]) + n[2] - (n[6] + (2.0*n[7]) + n[8]);
        //        float sobel = sqrt((sobel_edge_h*sobel_edge_h) + (sobel_edge_v*sobel_edge_v));
        float sobel_edge_h = n2[4] + (4.0*n2[9]) + (6.0*n2[14]) + (4.0*n2[19])+ n2[24] - (n2[0] + (4.0*n2[5]) + (6.0*n2[10]) + (4.0*n2[15]) + n2[20]);
        float sobel_edge_v = n2[0] + (4.0*n2[1]) + (6.0*n2[2]) + (4.0*n2[3]) + n2[4] - (n2[20] + (4.0*n2[21]) + (6.0*n2[22]) + (4.0*n2[23]) + n2[24]);
        float sobel_edge_h2 = (2.0*n2[3]) + (8.0*n2[8]) + (12.0*n2[13]) + (8.0*n2[18]) + (2.0*n2[23]) - ((2.0*n2[1]) + (8.0*n2[6]) + (12.0*n2[11]) + (8.0*n2[16]) + (2.0*n2[21]));
        float sobel_edge_v2 = (2.0*n2[5]) + (8.0*n2[6]) + (12.0*n2[7]) + (8.0*n2[8]) + (2.0*n2[9]) - ((2.0*n2[15]) + (8.0*n2[16]) + (12.0*n2[17]) + (8.0*n2[18]) + (2.0*n2[19]));
        sobel2 = sqrt(((sobel_edge_h*sobel_edge_h)+(sobel_edge_h2*sobel_edge_h2) + (sobel_edge_v*sobel_edge_v)+(sobel_edge_v2*sobel_edge_v2)))*0.001/12.0;
    }

    vec3 add = vec3(0.0, 0.0, 0.0);
    float brightness = light_ambient;

    if (d > 0.0){

        d = log(d);

        float s = 0.0;

        //sobel edge detection
        if (light_sobel_magnitude != 0.0){
//            float sobel_edge_h = n[2] + (2.0*n[5]) + n[8] - (n[0] + (2.0*n[3]) + n[6]);
//            float sobel_edge_v = n[0] + (2.0*n[1]) + n[2] - (n[6] + (2.0*n[7]) + n[8]);
//            float sobel = sqrt((sobel_edge_h*sobel_edge_h) + (sobel_edge_v*sobel_edge_v));


            //https://stackoverflow.com/a/10032882

            //https://stackoverflow.com/a/41065243
            //Gx_ij = i / (i*i + j*j)
            //Gy_ij = j / (i*i + j*j)

            //if all neighbours have the same value (e.g. not escaped) but middle is different, compare them as just two values
            //to avoid strangely dim single pixels in constrast to isles a few bright pixels in otherwise stable/black regions.
            //        float s = sobel > 0.0 || (n[0] == n[4] && n[1] == n[4] && n[2] == n[4] && n[3] == n[4]
            //            && n[5] == n[4] && n[6] == n[4] && n[7] == n[4] && n[8] == n[4]) ? sobel : min(abs(n[4]-n[0]), 1.0);
            s = sobel;
            s = log(s+1.0);

            if (light_sobel_period > 0.0){
                s = fract(s*0.5/light_sobel_period)*2.0;
                s = 1.0 - abs(1.0 - s);
            }
        }

        //select color based on iteration count
        vec3 hsv = vec3(d/colorMult+colorAdd,colorSaturation, 1.0);
        rgb = vec4(hsv2rgb(hsv), 1.0);

        if (gl_FragCoord.x >= 2.0 && gl_FragCoord.x < resolution.x-2.0 && gl_FragCoord.y >= 2.0 && gl_FragCoord.y < resolution.y-2.0)
            brightness += light_sobel_magnitude*s;

//        float chance = fract(brightness * 256.0);
//        if (rand(v_texCoords.xy) < chance)
//            brightness += 1.0/256.0;

        //gl_FragColor = rgb;
        brightness = tanh(brightness);
        int brightness256 = int(brightness*256.0);


//        brightness = brightness-min(dim, brightness/2.0);

        //palette

        //test: reduce banding due to brightness at low brightness levels
        float colorBr = (rgb.r + rgb.g + rgb.b)/3.0;
        ivec3 scRgb = ivec3(rgb*256.0*brightness);
        int deviation = int(round((float(scRgb.r + scRgb.g + scRgb.b)/(colorBr*256.0)) / brightness));
        if (deviation == 2){
            add.r = add.r > add.g || add.r > add.b ? 1.0/255.0 : 0.0;
            add.g = add.g > add.r || add.g > add.b ? 1.0/255.0 : 0.0;
            add.b = add.b > add.g || add.b > add.r ? 1.0/255.0 : 0.0;
        }
        else if (deviation == 1){
            add.r = add.r > add.g && add.r > add.b ? 1.0/255.0 : 0.0;
            add.g = add.g > add.r && add.g > add.b ? 1.0/255.0 : 0.0;
            add.b = add.b > add.g && add.b > add.r ? 1.0/255.0 : 0.0;
        }
    }
//    else {
//        float br = mod(decode(altColoringColor), 1.0);
//        colour1 = vec4(br,br,br,1.0);
//        for (int i = 0 ; i < int(n2.length) ; i++){
//            n2[i] = n2[i]*0.001;
//            n[i] = mod(n[i], 1.0);
//            if (n[i] > 0.5)
//                n[i] = 1.0-n[i];
//        }

//        if (n[4] > 0.5){
//            for (int i = 0 ; i < n.length ; i++){
//                if (n[i] > 1.5)
//                    n[i] = 2.0-n[i];
//            }
//        }
//        float sobel_edge_h2 = n[2] + (2.0*n[5]) + n[8] - (n[0] + (2.0*n[3]) + n[6]);
//        float sobel_edge_v2 = n[0] + (2.0*n[1]) + n[2] - (n[6] + (2.0*n[7]) + n[8]);
//        float sobel2 = sqrt((sobel_edge_h2*sobel_edge_h2) + (sobel_edge_v2*sobel_edge_v2));
//        sobel = min(sobel, sobel2);
//        sobel_edge_h = min(sobel_edge_h, sobel_edge_h2);
//        sobel_edge_v = min(sobel_edge_v, sobel_edge_v2);
//        float sobel = sqrt(((sobel_edge_h*sobel_edge_h) + (sobel_edge_v*sobel_edge_v))/256.0);


//    }

    if (light_sobel_period2 != 0.0){
        sobel2 = fract(sobel2*0.5/light_sobel_period2)*2.0;
        //        s = 1.0 - abs(1.0 - s);
        if (sobel2 > 1.0)
            sobel2 = 2.0-sobel2;
    }

    float brightness2 = light_ambient2;
    if (gl_FragCoord.x >= 2.0 && gl_FragCoord.x < resolution.x-2.0 && gl_FragCoord.y >= 2.0 && gl_FragCoord.y < resolution.y-2.0)
        brightness2 = tanh(sqrt(sobel2)*light_sobel_magnitude2+light_ambient2);
    //            brightness = min(sqrt(sobel)*light_sobel_magnitude2/light_sobel_period2+light_ambient2, 1.0);
    //        float brightness = 1.0;
    //        float brightness = min(log(sobel+1.0)*light_sobel_magnitude2+light_ambient2, 1.0);

    float val1 = d/colorMult + colorAdd;
    float val2 = value2/(1000.0*colorMult2) + colorAdd2;
    rgb = rgb*brightness;
    vec3 c = vec3(rgb.r+add.r, rgb.g+add.g, rgb.b+add.b);
    vec3 c2 = hsv2rgb(vec3(val2, colorSaturation2, brightness2));
    float f = d > 0.0 ? 1.0 : 0.0;
    float f2 = d > 0.0 ? 0.0 : 1.0;
    vec4 rgb1 = texture(paletteEscaped, vec2(mod(val1, 1.0), 0.0));
    vec4 rgb2 = texture(paletteFallback, vec2(mod(val2, 1.0), 0.0));
    if (usePalette > 0){
        c = rgb1.rgb*brightness;
    }
    if (usePalette2 > 0){
        c2 = rgb2.rgb*brightness2;
    }
//    if (usePalette > 0 || usePalette2 > 0){
//        vec3 v2 = vec3(texture(palette, vec2(mod(val2, 1.0), 0.0)));
//        v2 = v2*brightness2;
//        vec4 rgb2 = vec4(v2.r, v2.g, v2.b, 1.0);
//        vec4 rgbTotal = rgb1*f + rgb2*f2;
//        float totalBrightness = (f*brightness+f2*brightness2);
//        colour1 = vec4(rgbTotal.r*totalBrightness, rgbTotal.g*totalBrightness, rgbTotal.b*totalBrightness, 1.0);
//    }
//    else {
        colour1 = vec4(f*c.r+f2*c2.r, f*c.g+f2*c2.g, f*c.b+f2*c2.b, 1.0);
//    }

    //extract channel
    if (extractChannel == 1)
        colour1 = vec4(colour1.r*mappingColorR, colour1.r*mappingColorG, colour1.r*mappingColorB, 1.0);
    else if (extractChannel == 2)
        colour1 = vec4(colour1.g*mappingColorR, colour1.g*mappingColorG, colour1.g*mappingColorB, 1.0);
    else if (extractChannel == 3)
        colour1 = vec4(colour1.b*mappingColorR, colour1.b*mappingColorG, colour1.b*mappingColorB, 1.0);
}