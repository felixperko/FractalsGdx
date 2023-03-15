#version 130
#ifdef GL_ES
precision highp float;
#endif

in vec4 v_color;
in vec2 v_texCoords;

out vec4 colour0;
out vec4 colour1;

uniform sampler2D textureR;
uniform sampler2D textureG;

uniform vec2 resolution;
uniform float ratio;

uniform float timeStep;
uniform float diffRate1;
uniform float diffRate2;
uniform float reactionRate;

uniform float feedRateConst;
uniform float feedRateVarFactorX;
uniform float feedRateVarFactorY;

uniform float killRateConst;
uniform float killRateVarFactorX;
uniform float killRateVarFactorY;

in vec2 pos;

void make_kernel(inout float n[18], vec2 coord, float weightCenter, float weightAdj, float weightCorner){
//    vec4 col0 = texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1, -1), 0); //c
//    vec4 col1 = texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, -1), 0); //a
//    vec4 col2 = texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, -1), 0); //c
//    vec4 col3 = texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1,  0), 0); //a
//    vec4 col4 = texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y), 0);
//    vec4 col5 = texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1,  0), 0); //a
//    vec4 col6 = texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1,  1), 0); //c
//    vec4 col7 = texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0,  1), 0); //a
//    vec4 col8 = texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1,  1), 0); //c
//
//    vec4 col9 = texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1, -1), 0);
//    vec4 col10 = texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, -1), 0);
//    vec4 col11 = texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, -1), 0);
//    vec4 col12 = texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1,  0), 0);
//    vec4 col13 = texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y), 0);
//    vec4 col14 = texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1,  0), 0);
//    vec4 col15 = texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1,  1), 0);
//    vec4 col16 = texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0,  1), 0);
//    vec4 col17 = texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1,  1), 0);
//    n[0] = col0.r*weightCorner;
//    n[1] = col1.r*weightAdj;
//    n[2] = col2.r*weightCorner;
//    n[3] = col3.r*weightAdj;
//    n[4] = col4.r*weightCenter;
//    n[5] = col5.r*weightAdj;
//    n[6] = col6.r*weightCorner;
//    n[7] = col7.r*weightAdj;
//    n[8] = col8.r*weightCorner;
//    n[0+9] = col9.r*weightCorner;
//    n[1+9] = col10.r*weightAdj;
//    n[2+9] = col11.r*weightCorner;
//    n[3+9] = col12.r*weightAdj;
//    n[4+9] = col13.r*weightCenter;
//    n[5+9] = col14.r*weightAdj;
//    n[6+9] = col15.r*weightCorner;
//    n[7+9] = col16.r*weightAdj;
//    n[8+9] = col17.r*weightCorner;

        n[0] =   clamp(float(texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1, -1), 0)), 0.0, 1.0)*weightCorner;
        n[1] =   clamp(float(texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, -1), 0)), 0.0, 1.0)*weightAdj;
        n[2] =   clamp(float(texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, -1), 0)), 0.0, 1.0)*weightCorner;
        n[3] =   clamp(float(texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1,  0), 0)), 0.0, 1.0)*weightAdj;
        n[4] =   clamp(float(texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0,  0), 0)), 0.0, 1.0)*weightCenter;
        n[5] =   clamp(float(texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1,  0), 0)), 0.0, 1.0)*weightAdj;
        n[6] =   clamp(float(texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1,  1), 0)), 0.0, 1.0)*weightCorner;
        n[7] =   clamp(float(texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0,  1), 0)), 0.0, 1.0)*weightAdj;
        n[8] =   clamp(float(texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1,  1), 0)), 0.0, 1.0)*weightCorner;
        n[0+9] = clamp(float(texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1, -1), 0)), 0.0, 1.0)*weightCorner;
        n[1+9] = clamp(float(texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0, -1), 0)), 0.0, 1.0)*weightAdj;
        n[2+9] = clamp(float(texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1, -1), 0)), 0.0, 1.0)*weightCorner;
        n[3+9] = clamp(float(texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1,  0), 0)), 0.0, 1.0)*weightAdj;
        n[4+9] = clamp(float(texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0,  0), 0)), 0.0, 1.0)*weightCenter;
        n[5+9] = clamp(float(texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1,  0), 0)), 0.0, 1.0)*weightAdj;
        n[6+9] = clamp(float(texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2(-1,  1), 0)), 0.0, 1.0)*weightCorner;
        n[7+9] = clamp(float(texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 0,  1), 0)), 0.0, 1.0)*weightAdj;
        n[8+9] = clamp(float(texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y)+ivec2( 1,  1), 0)), 0.0, 1.0)*weightCorner;
}

void main()
{

    float n[18];
//    make_kernel(n, v_texCoords.xy, -1.0, 0.2, 0.05);
    make_kernel(n, v_texCoords.xy, -1.0, 1.0/8.0, 1.0/8.0);
    float currR = clamp(float(texelFetch(textureR, ivec2(gl_FragCoord.x, gl_FragCoord.y), 0)), 0.0, 1.0);
    float currG = clamp(float(texelFetch(textureG, ivec2(gl_FragCoord.x, gl_FragCoord.y), 0)), 0.0, 1.0);
    float neighbourAvgR = (n[0]+n[1]+n[2]+n[3]+n[4]+n[5]+n[6]+n[7]+n[8]);
    float neighbourAvgG = (n[9]+n[10]+n[11]+n[12]+n[13]+n[14]+n[15]+n[16]+n[17]);

//    float deltaX = (pos.x)*ratio;
//    float deltaY = (pos.y);
    float deltaX = float(gl_FragCoord.x)/resolution.x - 0.5;
    float deltaY = float(gl_FragCoord.y)/resolution.y - 0.5;
//    float deltaX = 0.0;
//    float deltaY = 0.0;
//    float feedRate = feedRateConst;
//    float killRate = killRateConst;
    float feedRate = feedRateConst + feedRateVarFactorX*deltaX + feedRateVarFactorY*deltaY;
    float killRate = killRateConst + killRateVarFactorX*deltaX + killRateVarFactorY*deltaY;

    float deltaT = timeStep;
    float reactionDelta = reactionRate*currR*currG*currG;
    float newR = currR + (diffRate1*(neighbourAvgR) - reactionDelta + feedRate*(1.0-currR))*deltaT;
    float newG = currG + (diffRate2*(neighbourAvgG) + reactionDelta - (killRate+feedRate)*currG)*deltaT;

//    colour0 = vec4(newR, 0.0, 0.0, 1.0);
//    colour1 = vec4(newG, 0.0, 0.0, 1.0);
    colour0 = vec4(min(1.0, max(0.0, newR)));
    colour1 = vec4(min(1.0, max(0.0, newG)));
}
