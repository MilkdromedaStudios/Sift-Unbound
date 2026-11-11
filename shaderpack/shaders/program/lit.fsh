#include "/lib/settings.glsl"
#include "/lib/common.glsl"

uniform sampler2D gtexture;
uniform sampler2D shadowtex0;
uniform vec3 shadowLightPosition;
uniform vec3 sunPosition;
uniform vec3 upPosition;
uniform vec3 fogColor;
uniform float fogStart;
uniform float fogEnd;
uniform float rainStrength;
uniform int worldTime;
uniform int isEyeInWater;
uniform vec4 entityColor;
uniform vec3 skyColor;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 normal;
varying vec3 viewPos;
varying vec4 shadowPos;
varying float blockId;

#include "/lib/light.glsl"
#include "/lib/sky.glsl"

#if defined SHADOWS && defined HAS_SHADOWS
float shadowAt(vec4 sp) {
	vec3 p = distortShadow(sp.xyz) * 0.5 + 0.5;
	if (any(lessThan(p, vec3(0.0))) || any(greaterThan(p, vec3(1.0)))) {
		return 1.0;
	}
	float texel = 1.0 / float(shadowMapResolution);
	float angle = bayer4(gl_FragCoord.xy) * 6.2831853;
	mat2 rot = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
	float lit = 0.0;
	float count = 0.0;
	for (int x = -SHADOW_SAMPLES + 1; x < SHADOW_SAMPLES; x++) {
		for (int y = -SHADOW_SAMPLES + 1; y < SHADOW_SAMPLES; y++) {
			vec2 off = rot * vec2(float(x), float(y)) * texel * 0.9;
			lit += step(p.z - 0.00008, texture2D(shadowtex0, p.xy + off).r);
			count += 1.0;
		}
	}
	// fade out towards the edge of the shadow distance
	float edge = smoothstep(0.8, 1.0, length(sp.xy));
	return mix(lit / count, 1.0, edge);
}
#endif

/* DRAWBUFFERS:0 */
void main() {
	vec4 albedo = texture2D(gtexture, texcoord) * glcolor;
#ifdef PROGRAM_WATER
	if (albedo.a < 0.01) discard;
#else
	if (albedo.a < 0.1) discard;
#endif
#ifdef PROGRAM_ENTITIES
	albedo.rgb = mix(albedo.rgb, entityColor.rgb, entityColor.a);
#endif
	albedo.rgb = toLinear(albedo.rgb);

	vec3 direct, ambient;
	lightColors(direct, ambient);
	float skyL = lmcoord.y;
	float blockL = lmcoord.x;

	vec3 n = normalize(normal);
	vec3 l = normalize(shadowLightPosition);
	float ndl = max(dot(n, l), 0.0);
	bool foliage = blockId >= 10000.0 && blockId <= 10002.0;
	if (foliage) {
		ndl = 0.45 + ndl * 0.4; // light passes through leaves and grass
	}

	float shadow = 1.0;
#if defined SHADOWS && defined HAS_SHADOWS
	if (ndl > 0.0) {
		shadow = shadowAt(shadowPos);
	}
	shadow = mix(1.0, shadow, SHADOW_STRENGTH);
#endif

	vec3 light = direct * ndl * shadow * smoothstep(0.02, 0.45, skyL);
	light += ambient * (0.15 + 0.85 * skyL * skyL);
	vec3 torch = mix(vec3(1.0), vec3(1.0, 0.72, 0.45), clamp(BLOCKLIGHT_WARMTH / 1.5, 0.0, 1.0));
	light += torch * pow(blockL, 2.4) * (1.6 + BLOCKLIGHT_WARMTH * 0.3);
	light += vec3(0.025); // never pitch black
	vec3 color = albedo.rgb * light;

#ifdef WATER_SHINE
#if defined PROGRAM_WATER && !defined DIM_NETHER
	vec3 r = reflect(normalize(viewPos), n);
	float spec = pow(max(dot(r, l), 0.0), 80.0) * shadow * smoothstep(0.3, 0.8, skyL);
	color += direct * spec * 1.5;
	albedo.a = mix(albedo.a, 1.0, spec);
#endif
#endif

	// fog, using the game's own fog colour and distances
	color = applyFog(color, viewPos);

	gl_FragData[0] = vec4(color, albedo.a);
}
