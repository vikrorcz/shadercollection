#version 120

#ifdef GL_ES
precision mediump float;
precision mediump int;
#else
#define highp;
#endif

varying vec2 v_texCoords;

uniform sampler2D u_texture;


uniform float u_amount;
uniform float u_speed;
uniform float u_time;
/*
void main(){
    vec4 diffuse = texture2D(u_diffuse, v_texCoords);
    vec4 normal = texture2D(u_texture, v_texCoords);

    float u = normal.r * 16.0;
    float v = normal.g * 16.0;
    u += floor(normal.b * 16.0) * 16.0;
    v += mod(normal.b * 255.0, 16.0) * 16.0;
    u = u / 255.0;
    v = v / 255.0;

    vec2 p = vec2(u, v + scrollOffset);
    vec4 reflect = texture2D(u_reflection, p);
    reflect.a = normal.a;

    vec4 col = mix(diffuse, reflect, normal.a - diffuse.a);
    col.a += normal.a;

    gl_FragColor = col;
}
*/

void main() {
    vec2 uv = v_texCoords;

    uv.x += (cos((uv.y + (u_time * 0.04 * u_speed)) * 15.0) * 0.0019 * u_amount) + (cos((uv.y + (u_time * 0.1 * u_speed)) * 10.0) * 0.002 * u_amount);
    uv.y += (sin((uv.y + (u_time * 0.07 * u_speed)) * 5.0) * 0.0029 * u_amount) + (sin((uv.y + (u_time * 0.1 * u_speed)) * 5.0) * 0.002 * u_amount);


    vec4 tex_color = texture2D(u_texture, uv);

    gl_FragColor = tex_color;
}
