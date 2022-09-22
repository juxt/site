'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');
var getMargins = require('./get-margins/get-margins.js');

function extractSx(sx, theme) {
  return typeof sx === "function" ? sx(theme) : sx;
}
function useSx(sx, margins, className) {
  const theme = styles.useMantineTheme();
  const { css, cx } = styles.useCss();
  if (Array.isArray(sx)) {
    return cx(className, css(getMargins.getMargins(margins, theme)), sx.map((partial) => css(extractSx(partial, theme))));
  }
  return cx(className, css(extractSx(sx, theme)), css(getMargins.getMargins(margins, theme)));
}

exports.useSx = useSx;
//# sourceMappingURL=use-sx.js.map
