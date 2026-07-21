#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float GameTime;

in vec2 texCoord;
out vec4 fragColor;

// cheap hash for pseudo-random streak positions around the circle
float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

void main() {
    vec2 center = vec2(0.5, 0.5);
    vec2 delta = texCoord - center;
    float dist = length(delta);
    float angle = atan(delta.y, delta.x);

    // Radial streaks: bands around the circle that scroll outward over time.
    float band = angle * 40.0 + GameTime * 6.0;
    float streak = hash(floor(band));
    float streakLine = smoothstep(0.9, 1.0, streak);
    // fade streaks in near the edge of the screen, out near the very center
    float radialFade = smoothstep(0.05, 0.6, dist);
    float streakMask = streakLine * radialFade;

    // Slight outward pull of the base image, increasing with distance from center
    // (gives a "being stretched into hyperspace" feel).
    float pull = dist * 0.12 * (0.6 + 0.4 * sin(GameTime * 1.3));
    vec2 warpedCoord = mix(texCoord, center, -pull);
    warpedCoord = clamp(warpedCoord, vec2(0.001), vec2(0.999));

    vec3 baseColor = texture(DiffuseSampler, warpedCoord).rgb;
    vec3 glowColor = vec3(1.0, 0.45, 0.7); // pink hyperspace glow

    vec3 result = mix(baseColor, glowColor, clamp(streakMask, 0.0, 0.85));
    fragColor = vec4(result, 1.0);
}
