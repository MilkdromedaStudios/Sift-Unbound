// Unlit geometry: lines, unlit particles, weather, sun and moon, glowing eyes, beacon beams, the sky and clouds.
#include "/lib/settings.glsl"

uniform mat4 gbufferModelViewInverse;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 viewPos;
varying vec3 worldNormal;

void main() {
	texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	lmcoord = clamp(((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy * 33.05 / 32.0) - (1.05 / 32.0), 0.0, 1.0);
	glcolor = gl_Color;
	worldNormal = mat3(gbufferModelViewInverse) * normalize(gl_NormalMatrix * gl_Normal);
	vec4 vpos = gl_ModelViewMatrix * gl_Vertex;
	viewPos = vpos.xyz;
	gl_Position = gl_ProjectionMatrix * vpos;
}
