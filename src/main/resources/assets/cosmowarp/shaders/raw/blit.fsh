#version 150

uniform sampler2D uDiffuseSampler;

in vec2 vTexCoord;
out vec4 fragColor;

void main() {
    fragColor = texture(uDiffuseSampler, vTexCoord);
}
