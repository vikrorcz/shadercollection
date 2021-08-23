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

    uv.y += (cos((uv.y + (u_time * 0.03 * u_speed)) * 15.0) * 0.0019 * u_amount) + (cos((uv.y + (u_time * 0.1 * u_speed)) * 15.0) * 0.001 * u_amount);
    uv.x += (sin((uv.x + (u_time * 0.03 * u_speed)) * 15.0) * 0.0019 * u_amount) + (sin((uv.x + (u_time * 0.1 * u_speed)) * 15.0) * 0.001 * u_amount);
    //uv.y = uv.y * 0.7-0.15 * (sin(v_texCoords.x * 40.0 + u_time) * 0.003);
    vec4 tex_color = texture2D(u_texture, uv);

    gl_FragColor = tex_color;
}

