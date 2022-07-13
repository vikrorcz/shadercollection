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
uniform sampler2D u_texture2;
uniform float u_time;
uniform vec2 resolution;
uniform vec2 coords;
uniform float u_speed;

float wavedx(vec2 position, vec2 direction, float time, float freq){
    float x = dot(direction, position) * freq + time;
    return exp(sin(x) - 1.0);
}

float getwaves(vec2 position){
    float iter = 0.0,phase = 6.0,speed = 4.0;
    float weight = 1.0,w = 0.0,ws = 0.0;
    for(int i=0;i<5;i++){
        vec2 p = vec2(sin(iter), cos(iter));
        float res = wavedx(position,p,speed*u_time,phase);
        w += res * weight; ws += weight;
        iter += 12.0; weight *=0.75; phase *= 1.58; speed *= 1.08;//iter += 12.0; weight *=0.75; phase *= 1.18; speed *= 1.08;
    }
    return w / ws;
}
float sea_octave(vec2 uv,float choppy){
    return getwaves(uv*choppy)+getwaves(uv); }

float noise3D(vec3 p){
    vec3 s = vec3(7, 157, 113);
    vec3 ip = floor(p); // Unique unit cell ID.
    vec4 h = vec4(0.0, s.yz, s.y + s.z) + dot(ip, s);
    p -= ip; // Cell's fractional component.
    p = p*p*(3.0 - 2.0*p);
    h = mix(fract(sin(h)*43758.5453),fract(sin(h + s.x)*43758.5453),p.x);
    h.xy = mix(h.xz, h.yw, p.y);
    return mix(h.x, h.y, p.z);
}

float rayStrength(vec2 raySource, vec2 rayRefDirection, vec2 coords, float seedA, float seedB, float speed)
{
    vec2 sourceToCoord = coords - raySource;
    float cosAngle = dot(normalize(sourceToCoord), rayRefDirection);

    return clamp(
    (0.45 + 0.15 * sin(cosAngle * seedA + u_time * u_speed)) +
    (0.3 + 0.2 * cos(-cosAngle * seedB + u_time * u_speed)),
    0.0, 1.0) *
    clamp((resolution.x - length(sourceToCoord)) / resolution.x, 0.5, 1.0);
}

void main() {
    // adjust uv
    //vec2 uv = (2.0 * gl_FragCoord.xy - resolution.xy) / resolution.y;
    //uv -= 0.4;//posun efektu vlnění

    vec2 uv = gl_FragCoord.xy/resolution.xy-0.5;
    uv.x*=resolution.x/resolution.y;
   // uv.y -= 0.15;//uv.y -= 0.15;


    vec3 pos = (vec3(1.0, 1.0, 1.0));
    vec3 dir = normalize(vec3(uv, -0.6));//vec3 dir = normalize(vec3(uv, -0.6));

    vec3 sun = vec3(0.0, 0.5,-0.5);//vec3 sun = vec3(-0.6, 0.5,-0.3);
    //sun.x *= cos(u_time * 0.6);
    //sun.y *= cos(u_time * 0.6);

    float i = max(0.0, 1.2/(length(sun-dir)+1.0));
    vec3 color = vec3(pow(i, 1.9), pow(i, 1.0), pow(i, 0.8)) * 1.25;
    color = mix(color, vec3(0.0,0.39,0.62),(1.0-dir.y)*0.9);//color = mix(color, vec3(0.0,0.39,0.62),(1.0-dir.y)*0.9);

    float d = (pos.y - 5.0) / dir.y;
    vec2 wat = (dir * d).xz - pos.xz;
    d += sin(wat.x + u_time*0.6);
    wat = (dir * d).xz - pos.xz;
    wat = wat * 0.1 + 0.2 * texture2D(u_texture, wat * 0.01).xz;
    color += sea_octave(wat,0.5)*0.6 * max(2.0/-d, 0.0);


    //rays
    vec2 coords = vec2(gl_FragCoord.x, resolution.y - gl_FragCoord.y);
    //coords.y -= 0.5;
    //vec2 coords = vec2(1.0, 1.0);
    //coords.xyz = sun.xyz;
    //coords.x *= cos(u_time * 0.6);
    // Set the parameters of the sun rays
    vec2 rayPos1 = vec2(resolution.x * 0.5, resolution.y * -0.2);
    vec2 rayRefDir1 = normalize(vec2(1.0, -0.116));
    float raySeedA1 = 36.2214;
    float raySeedB1 = 21.11349;
    float raySpeed1 = 1.5;

    vec2 rayPos2 = vec2(resolution.x * 0.5, resolution.y * -0.2);
    vec2 rayRefDir2 = normalize(vec2(1.0, 0.241));
    const float raySeedA2 = 22.39910;
    const float raySeedB2 = 18.0234;
    const float raySpeed2 = 1.1;

    vec3 rays1 =
    vec3(1.0, 1.0, 1.0) *
    rayStrength(rayPos1, rayRefDir1, coords, raySeedA1, raySeedB1, raySpeed1);

    vec3 rays2 =
    vec3(1.0, 1.0, 1.0) *
    rayStrength(rayPos2, rayRefDir2, coords, raySeedA2, raySeedB2, raySpeed2);

    float brightness = 1.0 - (coords.y / resolution.y);
    rays1.x *= 0.1 + (brightness * 0.7);
    rays1.y *= 0.3 + (brightness * 0.6);
    rays1.z *= 0.5 + (brightness * 0.5);

    rays2.x *= 0.1 + (brightness * 0.7);
    rays2.y *= 0.3 + (brightness * 0.6);
    rays2.z *= 0.5 + (brightness * 0.5);
    //end rays

    //vec2 p = gl_FragCoord.xy / resolution.xy;
    //p.y = 1.0 - p.y;
    //vec3 tex_color = texture2D(u_texture1, p).xyz;

    color += rays1 * 0.1 + rays2 * 0.09;// + rays2 * 0.09;

    //tex_color = tex_color * 1.0 + color * 0.7 + rays1 * 0.2 + rays2 * 0.1;
    //tex_color += color;
    //gl_FragColor = tex_color;;
    gl_FragColor = vec4(color, 1.0);

}
