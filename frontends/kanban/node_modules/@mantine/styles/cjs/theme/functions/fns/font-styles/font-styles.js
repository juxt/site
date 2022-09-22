'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

function fontStyles(theme) {
  return () => ({
    WebkitFontSmoothing: "antialiased",
    MozOsxFontSmoothing: "grayscale",
    fontFamily: theme.fontFamily || "sans-serif"
  });
}

exports.fontStyles = fontStyles;
//# sourceMappingURL=font-styles.js.map
