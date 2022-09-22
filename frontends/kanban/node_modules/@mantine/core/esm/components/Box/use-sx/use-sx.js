import { useMantineTheme, useCss } from '@mantine/styles';
import { getMargins } from './get-margins/get-margins.js';

function extractSx(sx, theme) {
  return typeof sx === "function" ? sx(theme) : sx;
}
function useSx(sx, margins, className) {
  const theme = useMantineTheme();
  const { css, cx } = useCss();
  if (Array.isArray(sx)) {
    return cx(className, css(getMargins(margins, theme)), sx.map((partial) => css(extractSx(partial, theme))));
  }
  return cx(className, css(extractSx(sx, theme)), css(getMargins(margins, theme)));
}

export { useSx };
//# sourceMappingURL=use-sx.js.map
