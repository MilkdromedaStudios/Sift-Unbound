#include "/lib/settings.glsl"
#include "/lib/common.glsl"

uniform sampler2D gtexture;
uniform vec3 sunPosition;
uniform vec3 upPosition;
uniform vec3 fogColor;
uniform vec3 skyColor;
uniform float fogStart;
uniform float fogEnd;
uniform float rainStrength;
uniform int worldTime;

varying vec2 texcoord;
varying vec2 lmcoord;
varying vec4 glcolor;
varying vec3 viewPos;
varying vec3 worldNormal;

#include "/lib/light.glsl"

/* DRAWBUFFERS:0 */
void main() {
#if defined PROGRAM_BASIC || defined PROGRAM_SKYBASIC
	vec4 c = glcolor;
#else
	vec4 c = texture2D(gtexture, texcoord) * glcolor;
#endif

#if defined PROGRAM_SKYBASIC
	// the sky as the game colours it, a little brighter so it survives tonemapping
	c.rgb = toLinear(c.rgb) * 1.35;
#elif defined PROGRAM_SKYTEXTURED || defined PROGRAM_EMISSIVE
	c.rgb = toLinear(c.rgb) * 1.6; // sun, moon, glowing eyes, beacon beams
#elif defined PROGRAM_CLOUDS
	vec3 direct, ambient;
	lightColors(direct, ambient);
	vec3 base = toLinear(c.rgb);
	#ifdef CLOUD_SHADING
	// blocky clouds: bright tops, softer sides, dark undersides
	float up = worldNormal.y;
	float face = up > 0.5 ? 1.0 : up < -0.5 ? 0.45 : 0.72;
	c.rgb = base * (ambient * 1.4 + direct * 0.45) * face;
	#else
	c.rgb = base * (ambient * 1.4 + direct * 0.35);
	#endif
	c.a *= CLOUD_OPACITY;
#elif defined PROGRAM_WEATHER
	c.rgb = toLinear(c.rgb);
	c.a *= 0.55;
#else
	if (c.a < 0.1) discard;
	vec3 direct, ambient;
	lightColors(direct, ambient);
	c.rgb = toLinear(c.rgb) * (ambient + direct * 0.4 * lmcoord.y + vec3(1.0, 0.8, 0.6) * lmcoord.x * 1.4 + 0.05);
#endif

#if !defined PROGRAM_SKYBASIC && !defined PROGRAM_SKYTEXTURED
	float dist = length(viewPos);
	float fog = clamp((dist - fogStart) / max(fogEnd - fogStart, 1.0), 0.0, 1.0);
	#ifdef PROGRAM_CLOUDS
	fog *= 0.6;
	#endif
	c.rgb = mix(c.rgb, toLinear(fogColor), fog * fog);
#endif
	gl_FragData[0] = c;
}
