// Light colours for each dimension. Expects: sunPosition, upPosition, worldTime, rainStrength, fogColor.

// The Sift's three tides (8000 ticks each, as in the mod): Flow (teal-white), Endure (deep blue), Thrive (salmon-gold).
void siftTide(out vec3 direct, out vec3 ambient) {
	float t = mod(float(worldTime), 24000.0);
	vec3 flowD = vec3(1.0, 1.05, 0.95) * 2.7, flowA = vec3(0.16, 0.32, 0.36);
	vec3 endureD = vec3(0.3, 0.38, 0.75) * 0.7, endureA = vec3(0.05, 0.07, 0.17);
	vec3 thriveD = vec3(1.5, 0.95, 0.7) * 2.7, thriveA = vec3(0.32, 0.2, 0.24);
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
#elif defined DIM_SIFT
	#ifdef SIFT_TIDE_LIGHT
	siftTide(direct, ambient);
	#else
	direct = vec3(1.0, 1.0, 0.95) * 1.8;
	ambient = vec3(0.35, 0.5, 0.55);
	#endif
#else
	float sunH = dot(normalize(sunPosition), normalize(upPosition));
	float day = smoothstep(-0.08, 0.22, sunH);
	float golden = 1.0 - smoothstep(0.05, 0.35, sunH);
	vec3 sun = mix(vec3(1.0, 0.9, 0.76), vec3(1.3, 0.58, 0.28), golden) * 3.0;
	vec3 moon = vec3(0.16, 0.2, 0.34) * 0.8;
	direct = mix(moon, sun, day);
	ambient = mix(vec3(0.035, 0.05, 0.1), vec3(0.2, 0.27, 0.42), day);
#endif
	direct *= 1.0 - rainStrength * 0.75;
	ambient *= 1.0 - rainStrength * 0.3;
}
