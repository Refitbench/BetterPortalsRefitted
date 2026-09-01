// Keep the same source usable by native ESSL and the compatibility wrappers.
#if defined(GL_ES) && !defined(MG_MOBILEGLUES)
precision highp float;
#endif

attribute vec3 pos;

varying vec2 vpos;

void main() {
    vpos = pos.xy;
    gl_Position = vec4(vpos, 0.0, 1.0);
}
