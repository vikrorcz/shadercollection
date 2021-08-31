#version 100

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif
varying LOWP vec4 vColor;
varying vec2 vTexCoord;
uniform sampler2D u_texture;
uniform sampler2D u_texture1;
uniform sampler2D u_mask;
uniform vec2 resolution;
void main() {
    vec2 p = gl_FragCoord.xy / resolution.xy;
    //sample the colour from the first texture
    vec4 texColor0 = texture2D(u_texture, vTexCoord);
    //sample the colour from the second texture
    vec4 texColor1 = texture2D(u_texture1, p);
    //get the mask; we will only use the alpha channel
    float mask = texture2D(u_mask, vTexCoord).a;
    //interpolate the colours based on the mask
    gl_FragColor = vColor * mix(texColor0, texColor1, mask);
}