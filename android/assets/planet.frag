#version 100

#ifdef GL_ES
#define PRECISION highp
precision PRECISION float;
precision PRECISION int;
#else
#define PRECISION
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_time;
uniform vec2 resolution;

void main() {
    //vec2 uv = (2.0 * gl_FragCoord.xy - resolution.xy) / resolution.y;
    //float t = (4.0*u_time) / 80.0;
    //vec3 Normal = vec3(uv.x, -uv.y, 1.0-dot(uv,uv) * 3.0);//škálování
    //float U = 1.0-atan(Normal.z, Normal.x) /(1.35 * 3.14159265359);//2.0
    //float V = 1.0-(atan(length(Normal.xz), Normal.y)) / (1.0 * 3.14159265359);
    //vec3 tex_color = texture2D(u_texture, vec2(U-t, V)).xyz;
    //gl_FragColor = vec4(tex_color, 1.0);

    vec2 p = (2.0 * gl_FragCoord.xy - resolution.xy) / resolution.y;
    //vec2 tc = (2.0 * gl_FragCoord.xy - resolution.xy) / resolution.y;
   // vec2 p = -1.0 + 2.0 * tc;//-1.0 + 2.0 * tc;
    float r = dot(p,p) * 3.5;//škálování
    if (r > 1.0) discard;
    float f = (1.0-sqrt(1.0-r))/(r);
    vec2 uv;
    uv.x = p.x * f + (u_time * 0.3);//uv.x = p.x * f + u_time;
    uv.y = 0.5 + -p.y * f;//uv.y = p.y * f + u_time; škálování textury
    vec3 tex_color = texture2D(u_texture, uv).xyz;
    gl_FragColor = vec4(tex_color, 1.0);

}