#version 150

uniform sampler2D uDiffuseSampler;
uniform float uGameTime;
uniform float uChargeProgress; // 0.0 at charge start -> 1.0 right before the jump

in vec2 vTexCoord;
out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

const int SAMPLES = 16;

void main() {
    vec2 center = vec2(0.5, 0.5);
    vec2 dir = vTexCoord - center;
    float dist = length(dir);

    // Ramps up as the charge nears completion, plus a small pulse so it doesn't look static.
    float pulse = 0.9 + 0.1 * sin(uGameTime * 2.0);
    float blurStrength = (0.25 + 0.55 * uChargeProgress) * pulse;

    // Radial zoom blur: average samples pulled in from the pixel toward the screen center.
    // This is what actually reads as "streaming outward from the center" rather than a simple pull.
    vec3 accum = vec3(0.0);
    float totalWeight = 0.0;
    for (int i = 0; i < SAMPLES; i++) {
        float t = float(i) / float(SAMPLES - 1);
        float scale = 1.0 - blurStrength * t;
        vec2 sampleCoord = center + dir * scale;
        sampleCoord = clamp(sampleCoord, vec2(0.001), vec2(0.999));
        float weight = 1.0 - t * 0.4;
        accum += texture(uDiffuseSampler, sampleCoord).rgb * weight;
        totalWeight += weight;
    }
    vec3 baseColor = accum / totalWeight;

    // Streak lines along the same radial direction, more prominent as charge nears completion.
    float angle = atan(dir.y, dir.x);
    float band = angle * 50.0 + uGameTime * 8.0;
    float streak = hash(floor(band));
    float streakLine = smoothstep(0.88, 1.0, streak);
    float radialFade = smoothstep(0.05, 0.55, dist) * (0.35 + 0.65 * uChargeProgress);
    float streakMask = streakLine * radialFade;

    vec3 glowColor = vec3(0.75, 0.95, 1.0); // white-leaning light cyan/blue

    vec3 result = mix(baseColor, glowColor, clamp(streakMask, 0.0, 0.9));
    fragColor = vec4(result, 1.0);
}
