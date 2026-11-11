// Sift Unbound Shaders: options. Iris reads the comments in [brackets] as the choices on the options screen.

// ---- shadows
#define SHADOWS // Sun, moon and tide shadows
const int shadowMapResolution = 2048; // [512 1024 2048 3072 4096]
const float shadowDistance = 112.0; // [48.0 64.0 80.0 112.0 160.0 224.0]
#define SHADOW_SAMPLES 2 // [1 2 3 4]
#define SHADOW_STRENGTH 0.9 // [0.4 0.5 0.6 0.75 0.9 1.0]
const float sunPathRotation = -25.0; // [-40.0 -30.0 -25.0 -15.0 0.0 15.0 25.0]

// ---- lighting
#define SIFT_TIDE_LIGHT // In the Sift, the light follows the Flow, Endure and Thrive tides
#define BLOCKLIGHT_WARMTH 1.0 // [0.0 0.5 1.0 1.5]
#define WATER_SHINE // Sun glints on water and ichor

// ---- world
#define WAVING_PLANTS // Grass, flowers and leaves sway in the wind
#define WAVING_SPEED 1.0 // [0.5 1.0 1.5 2.0]
#define CLOUD_SHADING // Blocky clouds with sunlit tops and shaded sides
#define CLOUD_OPACITY 0.85 // [0.5 0.65 0.75 0.85 1.0]

// ---- post
#define EXPOSURE 1.0 // [0.7 0.85 1.0 1.15 1.3]
#define CONTRAST 1.12 // [0.9 1.0 1.05 1.12 1.2 1.3]
#define VIBRANCE 1.3 // [0.8 1.0 1.15 1.3 1.5 1.7]
#define SHARPEN 0.4 // [0.0 0.2 0.4 0.6 0.8 1.0]
#define VIGNETTE 0.25 // [0.0 0.15 0.25 0.4]
#define FOG_DENSITY 1.0 // [0.0 0.5 1.0 1.5 2.0]
