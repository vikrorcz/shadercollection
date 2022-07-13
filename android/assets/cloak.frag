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
    //float sway = 20.0;
    //uv.x += (cos(u_time/2.0 + uv.y / 100.0) / (100.0 - sway)) + sin(u_time + uv.y / 100.0) / (100.0 - sway);
    uv.y += (cos((uv.y + (u_time * 0.03 * u_speed)) * 15.0) * 0.0019 * u_amount) + (cos((uv.y + (u_time * 0.1 * u_speed)) * 15.0) * 0.001 * u_amount);
    uv.x += (sin((uv.x + (u_time * 0.03 * u_speed)) * 15.0) * 0.0019 * u_amount) + (sin((uv.x + (u_time * 0.1 * u_speed)) * 15.0) * 0.001 * u_amount);
    vec4 tex_color = texture2D(u_texture, uv);

    gl_FragColor = tex_color;
}

