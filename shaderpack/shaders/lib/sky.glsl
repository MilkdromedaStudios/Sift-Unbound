// Sky gradient and atmospheric haze. Expects: skyColor, fogColor, sunPosition, upPosition, fogStart, fogEnd,
// isEyeInWater, rainStrength.

vec3 skyGradient(vec3 dir) {
	float up = dot(dir, normalize(upPosition));
	vec3 zenith = toLinear(skyColor) * 0.85;
	vec3 horizon = toLinear(fogColor) * 1.05;
	vec3 sky = mix(horizon, zenith, smoothstep(-0.05, 0.55, up));
	float sunGlow = max(dot(dir, normalize(sunPosition)), 0.0);
	sky += vec3(1.0, 0.75, 0.45) * (pow(sunGlow, 8.0) * 0.35 + pow(sunGlow, 64.0) * 0.8) * (1.0 - rainStrength);
	return sky * 1.25;
}

vec3 applyFog(vec3 color, vec3 viewPos) {
	float dist = length(viewPos);
	if (isEyeInWater != 0) { // under water or in lava: the game's own thick fog
		float f = clamp((dist - fogStart) / max(fogEnd - fogStart, 1.0), 0.0, 1.0);
		return mix(color, toLinear(fogColor), f);
	}
	// haze that thickens with distance and fills in completely at the edge of the render distance
	float haze = 1.0 - exp(-dist * 0.0022 * FOG_DENSITY * (1.0 + rainStrength * 3.0));
	float edge = smoothstep(fogEnd * 0.75, fogEnd, dist);
	float f = clamp(max(haze, edge), 0.0, 1.0);
	return mix(color, skyGradient(normalize(viewPos)) * 0.9, f);
}
