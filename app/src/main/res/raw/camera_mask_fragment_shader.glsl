#version 100
precision mediump float;
uniform sampler2D uTexSampler;
uniform int uShape;
varying vec2 vTexSamplingCoord;

void main() {
  vec4 source = texture2D(uTexSampler, vTexSamplingCoord);
  vec2 centered = abs(vTexSamplingCoord - vec2(0.5));
  float alpha = 1.0;
  if (uShape == 0) {
    float distanceFromCenter = length((vTexSamplingCoord - vec2(0.5)) * 2.0);
    alpha = 1.0 - smoothstep(0.985, 1.0, distanceFromCenter);
  } else if (uShape == 2) {
    float radius = 0.12;
    vec2 cornerDistance = max(centered - vec2(0.5 - radius), vec2(0.0));
    float signedDistance = length(cornerDistance) - radius;
    alpha = 1.0 - smoothstep(-0.005, 0.005, signedDistance);
  }
  gl_FragColor = vec4(source.rgb, source.a * alpha);
}
