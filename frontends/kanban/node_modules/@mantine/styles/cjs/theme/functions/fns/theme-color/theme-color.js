'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

function themeColor(theme) {
  return (color, shade, primaryFallback = true) => {
    const primaryShades = theme.colors[theme.primaryColor];
    return color in theme.colors ? theme.colors[color][shade] : primaryFallback ? primaryShades[shade] : color;
  };
}

exports.themeColor = themeColor;
//# sourceMappingURL=theme-color.js.map
