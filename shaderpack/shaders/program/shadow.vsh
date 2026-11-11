#include "/lib/settings.glsl"
#include "/lib/common.glsl"

varying vec2 texcoord;
varying vec4 glcolor;

void main() {
	texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	glcolor = gl_Color;
#ifdef SHADOWS
	vec4 p = ftransform();
	p.xyz = distortShadow(p.xyz);
	gl_Position = p;
#else
	gl_Position = vec4(-10.0, -10.0, -10.0, 1.0); // shadows off: draw nothing
#endif
}
