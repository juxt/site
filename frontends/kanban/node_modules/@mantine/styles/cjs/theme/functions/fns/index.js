'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var fontStyles = require('./font-styles/font-styles.js');
var focusStyles = require('./focus-styles/focus-styles.js');
var themeColor = require('./theme-color/theme-color.js');
var gradient = require('./gradient/gradient.js');
var breakpoints = require('./breakpoints/breakpoints.js');
var rgba = require('./rgba/rgba.js');
var size = require('./size/size.js');
var cover = require('./cover/cover.js');
var darken = require('./darken/darken.js');
var lighten = require('./lighten/lighten.js');

const fns = {
  fontStyles: fontStyles.fontStyles,
  themeColor: themeColor.themeColor,
  focusStyles: focusStyles.focusStyles,
  linearGradient: gradient.linearGradient,
  radialGradient: gradient.radialGradient,
  smallerThan: breakpoints.smallerThan,
  largerThan: breakpoints.largerThan,
  rgba: rgba.rgba,
  size: size.size,
  cover: cover.cover,
  darken: darken.darken,
  lighten: lighten.lighten
};

exports.fns = fns;
//# sourceMappingURL=index.js.map
