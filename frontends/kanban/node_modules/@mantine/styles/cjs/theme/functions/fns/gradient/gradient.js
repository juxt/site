'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var getGradientColorStops = require('./get-gradient-color-stops/get-gradient-color-stops.js');

function linearGradient(deg, ...colors) {
  return `linear-gradient(${deg}deg, ${getGradientColorStops.getGradientColorStops(colors)})`;
}
function radialGradient(...colors) {
  return `radial-gradient(circle, ${getGradientColorStops.getGradientColorStops(colors)})`;
}

exports.linearGradient = linearGradient;
exports.radialGradient = radialGradient;
//# sourceMappingURL=gradient.js.map
