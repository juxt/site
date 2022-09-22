import { getGradientColorStops } from './get-gradient-color-stops/get-gradient-color-stops.js';

function linearGradient(deg, ...colors) {
  return `linear-gradient(${deg}deg, ${getGradientColorStops(colors)})`;
}
function radialGradient(...colors) {
  return `radial-gradient(circle, ${getGradientColorStops(colors)})`;
}

export { linearGradient, radialGradient };
//# sourceMappingURL=gradient.js.map
