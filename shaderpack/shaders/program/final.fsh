// Post: sharpening, exposure, filmic tonemap, contrast, vibrance and a vignette, so the details pop.
#include "/lib/settings.glsl"
#include "/lib/common.glsl"

/*
const int colortex0Format = RGBA16F;
*/

uniform sampler2D colortex0;
uniform float viewWidth;
uniform float viewHeight;

varying vec2 texcoord;

vec3 aces(vec3 x) {
	return clamp(x * (2.51 * x + 0.03) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
	vec2 px = 1.0 / vec2(viewWidth, viewHeight);
	vec3 c = texture2D(colortex0, texcoord).rgb;
	if (SHARPEN > 0.0) {
	vec3 n = texture2D(colortex0, texcoord + vec2(0.0, px.y)).rgb;
	vec3 s = texture2D(colortex0, texcoord - vec2(0.0, px.y)).rgb;
	vec3 e = texture2D(colortex0, texcoord + vec2(px.x, 0.0)).rgb;
	vec3 w = texture2D(colortex0, texcoord - vec2(px.x, 0.0)).rgb;
	float k = float(SHARPEN) * 0.25;
	c = max(c * (1.0 + 4.0 * k) - k * (n + s + e + w), vec3(0.0));
	}
	c = aces(c * EXPOSURE * 1.1);
	c = pow(c, vec3(1.0 / 2.2));
	c = clamp((c - 0.5) * CONTRAST + 0.5, 0.0, 1.0);
	float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
	float sat = max(c.r, max(c.g, c.b)) - min(c.r, min(c.g, c.b));
	c = mix(vec3(luma), c, 1.0 + (VIBRANCE - 1.0) * (1.0 - sat));
	vec2 v = texcoord - 0.5;
	c *= 1.0 - dot(v, v) * VIGNETTE * 1.6;
	c += (bayer4(gl_FragCoord.xy) - 0.5) / 255.0; // dither away banding
	gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
