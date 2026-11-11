// Shared helpers.
vec3 toLinear(vec3 c) { return pow(c, vec3(2.2)); }

// Squeezes the shadow map towards the player so nearby shadows get more texels.
vec3 distortShadow(vec3 p) {
	float f = length(p.xy) * 0.85 + 0.15;
	return vec3(p.xy / f, p.z * 0.25);
}

float bayer4(vec2 p) {
	ivec2 i = ivec2(mod(p, 4.0));
	int idx = i.x + i.y * 4;
	float m[16] = float[16](0.0, 8.0, 2.0, 10.0, 12.0, 4.0, 14.0, 6.0, 3.0, 11.0, 1.0, 9.0, 15.0, 7.0, 13.0, 5.0);
	return m[idx] / 16.0;
}
