uniform sampler2D gtexture;

varying vec2 texcoord;
varying vec4 glcolor;

void main() {
	if (texture2D(gtexture, texcoord).a * glcolor.a < 0.1) discard;
	gl_FragData[0] = vec4(1.0);
}
