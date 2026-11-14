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
varying vec3 worldPos;
uniform float frameTimeCounter;

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
#ifdef PROGRAM_WATER
	if (blockId == 10020.0) { // ichor: a slow, drifting oil film that never repeats, on top of the game's own tint
		vec2 q = worldPos.xz * 0.35;
		float t = frameTimeCounter * 0.12;
		float swirl = sin(q.x * 1.3 + t + sin(q.y * 0.9 - t * 0.7) * 1.8) + sin(q.y * 1.7 - t * 0.8 + sin(q.x * 0.6 + t) * 2.1)
			+ sin((q.x + q.y) * 0.45 + t * 0.5) * 1.5;
		vec3 film = 0.5 + 0.5 * cos(6.2831853 * (swirl * 0.16 + vec3(0.0, 0.33, 0.67)));
		albedo.rgb = mix(albedo.rgb, albedo.rgb * (0.55 + film * 0.9) + film * 0.06, 0.6);
		albedo.a = max(albedo.a, 0.9);
	}
#endif

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
	vec3 amb = ambient;
#ifdef SKY_LIGHTING
	// hemisphere light: sky colour from above, warm bounce from below
	float upness = dot(n, normalize(upPosition)) * 0.5 + 0.5;
	vec3 sky = toLinear(skyColor);
	sky = mix(vec3(dot(sky, vec3(0.3, 0.6, 0.1))), sky, 0.35); // only a hint of the sky's hue, so colours stay true
	vec3 skyAmb = mix(ambient, sky * dot(ambient, vec3(0.6)), 0.3) * 1.3;
	vec3 groundAmb = ambient * vec3(1.0, 0.85, 0.7) * 0.6;
	amb = mix(groundAmb, skyAmb, upness);
#endif
	light += amb * (0.15 + 0.85 * skyL * skyL);
	vec3 torch = mix(vec3(1.0), vec3(1.0, 0.72, 0.45), clamp(BLOCKLIGHT_WARMTH / 1.5, 0.0, 1.0));
	light += torch * pow(blockL, 2.4) * (1.6 + BLOCKLIGHT_WARMTH * 0.3);
	light += vec3(0.025); // never pitch black
	vec3 color = albedo.rgb * light;
	if (foliage) { // sunlight glowing through leaves and grass seen against the light
		float through = pow(max(dot(normalize(viewPos), l), 0.0), 4.0);
		color += albedo.rgb * direct * through * shadow * 0.35 * smoothstep(0.3, 0.9, skyL);
	}
	if (blockId == 10010.0) { // glowing blocks shine with their own colour (and feed the bloom)
		float lum = dot(albedo.rgb, vec3(0.3, 0.6, 0.1));
		color += albedo.rgb * (0.6 + lum * 2.4) * EMISSIVE_GLOW;
	}

#ifdef WATER_SHINE
#if defined PROGRAM_WATER && !defined DIM_NETHER
	vec3 r = reflect(normalize(viewPos), n);
	float spec = pow(max(dot(r, l), 0.0), 80.0) * shadow * smoothstep(0.3, 0.8, skyL);
	color += direct * spec * 1.5;
	// the sky reflects in the surface, most at grazing angles
	float fresnel = pow(1.0 - max(dot(-normalize(viewPos), n), 0.0), 4.0);
	color = mix(color, skyGradient(r) * 0.9, fresnel * 0.7 * smoothstep(0.3, 0.8, skyL));
	albedo.a = mix(albedo.a, 1.0, max(spec, fresnel * 0.6));
#endif
#endif

	// fog, using the game's own fog colour and distances
	color = applyFog(color, viewPos);

	gl_FragData[0] = vec4(color, albedo.a);
}
