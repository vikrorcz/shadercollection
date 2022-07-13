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
uniform vec2 resolution;
uniform vec2 coords;

float rayStrength(vec2 raySource, vec2 rayRefDirection, vec2 coords, float seedA, float seedB, float speed)
{
    vec2 sourceToCoord = coords - raySource;
    float cosAngle = dot(normalize(sourceToCoord), rayRefDirection);

    //return clamp(
    //(1.0 + 0.15 * sin(cosAngle * seedA + u_time * u_speed)) +
    //(1.0 + 0.2 * cos(-cosAngle * seedB + u_time * u_speed)),
    //1.0, 1.0) *
    //clamp((resolution.x - length(sourceToCoord)) / resolution.x, 0.5, 1.0);

    return clamp(
    (0.45 + 0.15 * sin(cosAngle * seedA + u_time * u_speed)) +
    (0.3 + 0.2 * cos(-cosAngle * seedB + u_time * u_speed)),
    0.0, 1.0) *
    clamp((resolution.x - length(sourceToCoord)) / resolution.x, 0.5, 1.0);
}

void main()
{
    vec2 uv = gl_FragCoord.xy / resolution.xy;
    uv.y = 1.0 - uv.y;
    vec2 coords = vec2(gl_FragCoord.x, resolution.y - gl_FragCoord.y);


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

    // Calculate the colour of the sun rays on the current fragment
    vec4 rays1 =
    vec4(1.0, 1.0, 1.0, 1.0) *
    rayStrength(rayPos1, rayRefDir1, coords, raySeedA1, raySeedB1, raySpeed1);

    vec4 rays2 =
    vec4(1.0, 1.0, 1.0, 1.0) *
    rayStrength(rayPos2, rayRefDir2, coords, raySeedA2, raySeedB2, raySpeed2);

    vec4 tex_color = texture2D(u_texture, uv);

    float brightness = 1.0 - (coords.y / resolution.y);
    rays1.x *= 0.1 + (brightness * 0.7);
    rays1.y *= 0.3 + (brightness * 0.6);
    rays1.z *= 0.5 + (brightness * 0.5);

    rays2.x *= 0.1 + (brightness * 0.7);
    rays2.y *= 0.3 + (brightness * 0.6);
    rays2.z *= 0.5 + (brightness * 0.5);

    gl_FragColor = tex_color * 1.0 + rays1 * 0.5 + rays2 * 0.4;

}
