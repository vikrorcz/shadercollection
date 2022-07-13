#version 100

#ifdef GL_ES
#define PRECISION mediump
precision PRECISION float;
precision PRECISION int;
#else
#define PRECISION
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_amount;
uniform float u_speed;
uniform float u_time;

void main() {
    vec2 uv = v_texCoords;
    float sway = 20.0;
    uv.x += (cos(u_time/2.0 + uv.y / 100.0) / (100.0 - sway)) + sin(u_time + uv.y / 100.0) / (100.0 - sway);
    vec4 tex_color = texture2D(u_texture, uv);

    gl_FragColor = tex_color;
}