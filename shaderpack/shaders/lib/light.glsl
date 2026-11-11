// Light colours for each dimension. Expects: sunPosition, upPosition, worldTime, rainStrength, fogColor.

// The Sift's three tides (8000 ticks each, as in the mod): Flow (teal-white), Endure (deep blue), Thrive (salmon-gold).
void siftTide(out vec3 direct, out vec3 ambient) {
	float t = mod(float(worldTime), 24000.0);
	vec3 flowD = vec3(0.95, 1.05, 1.0) * 1.9, flowA = vec3(0.32, 0.55, 0.58);
	vec3 endureD = vec3(0.28, 0.36, 0.7) * 0.55, endureA = vec3(0.07, 0.10, 0.22);
	vec3 thriveD = vec3(1.45, 0.98, 0.78) * 1.9, thriveA = vec3(0.58, 0.42, 0.44);
	float toEndure = smoothstep(7400.0, 8600.0, t);
	float toThrive = smoothstep(15400.0, 16600.0, t);
	float toFlow = smoothstep(23400.0, 24000.0, t) + (1.0 - smoothstep(0.0, 600.0, t)) * 0.0;
	direct = mix(mix(mix(flowD, endureD, toEndure), thriveD, toThrive), flowD, toFlow);
	ambient = mix(mix(mix(flowA, endureA, toEndure), thriveA, toThrive), flowA, toFlow);
}

void lightColors(out vec3 direct, out vec3 ambient) {
#if defined DIM_NETHER
	direct = vec3(0.0);
	ambient = toLinear(fogColor) * 0.6 + vec3(0.16, 0.09, 0.07);
#elif defined DIM_END
	direct = vec3(0.55, 0.45, 0.75) * 0.9;
	ambient = vec3(0.18, 0.14, 0.26);
#elif defined DIM_SIFT && defined SIFT_TIDE_LIGHT
	siftTide(direct, ambient);
#else
	float sunH = dot(normalize(sunPosition), normalize(upPosition));
	float day = smoothstep(-0.08, 0.22, sunH);
	float golden = 1.0 - smoothstep(0.05, 0.35, sunH);
	vec3 sun = mix(vec3(1.0, 0.94, 0.84), vec3(1.25, 0.62, 0.32), golden) * 2.1;
	vec3 moon = vec3(0.16, 0.2, 0.34) * 0.7;
	direct = mix(moon, sun, day);
	ambient = mix(vec3(0.045, 0.06, 0.11), vec3(0.42, 0.52, 0.72), day);
#endif
	direct *= 1.0 - rainStrength * 0.75;
	ambient *= 1.0 - rainStrength * 0.3;
}
