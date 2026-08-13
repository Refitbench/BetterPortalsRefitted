#version 120

uniform sampler2D sampler;
uniform vec2 screenSize;
uniform float fogDensity;
uniform vec3 fogColor;
uniform float opacity;

void main() {
    vec2 uv = gl_FragCoord.xy / screenSize;
    vec3 color = texture2D(sampler, uv).rgb;
    color = mix(color, gl_Fog.color.rgb, clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0));
    color = mix(color, fogColor, fogDensity);
    // Fade the remote view out (e.g. one-way portals fading after use) by fading its alpha instead of
    // darkening the color: the background (sky) shows through as the portal fades instead of turning black.
    gl_FragColor = vec4(color, opacity);
    gl_FragDepth = gl_FragCoord.z;
}
