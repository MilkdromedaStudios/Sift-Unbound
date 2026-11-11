// Lit geometry (terrain, entities, hand, block entities, water, lit particles).
// Wrappers define DIM_* and PROGRAM_* before including this file.
#include "/lib/settings.glsl"
#include "/lib/common.glsl"

attribute vec4 mc_Entity;
attribute vec4 mc_midTexCoord;

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;
uniform float rainStrength;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 normal;
varying vec3 viewPos;
varying vec4 shadowPos;
varying float blockId;

void main() {
	texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
	lmcoord = clamp((lmcoord * 33.05 / 32.0) - (1.05 / 32.0), 0.0, 1.0);
	glcolor = gl_Color;
	normal = normalize(gl_NormalMatrix * gl_Normal);
	blockId = mc_Entity.x;

	vec4 vpos = gl_ModelViewMatrix * gl_Vertex;
	vec3 playerPos = (gbufferModelViewInverse * vpos).xyz;

#if defined WAVING_PLANTS && defined PROGRAM_TERRAIN
	// 10000: short plants (top edge sways), 10001: leaves (gentle), 10002: upper half of tall plants (whole quad sways)
	bool top = gl_MultiTexCoord0.t < mc_midTexCoord.t;
	float amount = (blockId == 10000.0 && top) ? 1.0 : blockId == 10002.0 ? 1.0 : blockId == 10001.0 ? 0.35 : 0.0;
	if (amount > 0.0) {
		vec3 world = playerPos + cameraPosition;
		float t = frameTimeCounter * WAVING_SPEED * (1.0 + rainStrength);
		vec2 wave = vec2(sin(t * 1.7 + world.x * 0.6 + world.z * 0.3), cos(t * 1.3 + world.z * 0.5 + world.x * 0.2));
		playerPos.xz += wave * 0.07 * amount * lmcoord.y;
		vpos = gbufferModelView * vec4(playerPos, 1.0);
	}
#endif

	viewPos = vpos.xyz;
	gl_Position = gl_ProjectionMatrix * vpos;

	// Shadow-space position, pushed out along the normal (more with distance) so surfaces don't shadow themselves.
	vec3 playerNormal = mat3(gbufferModelViewInverse) * normal;
	float bias = 0.03 + length(playerPos) * 0.0015;
	shadowPos = shadowProjection * (shadowModelView * vec4(playerPos + playerNormal * bias, 1.0));
}
