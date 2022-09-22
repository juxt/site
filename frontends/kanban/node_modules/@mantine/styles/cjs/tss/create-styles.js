'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var fromEntries = require('./utils/from-entries/from-entries.js');
var useCss = require('./use-css.js');
var MantineProvider = require('../theme/MantineProvider.js');
var mergeClassNames = require('./utils/merge-class-names/merge-class-names.js');

function createStyles(getCssObjectOrCssObject) {
  const getCssObject = typeof getCssObjectOrCssObject === "function" ? getCssObjectOrCssObject : () => getCssObjectOrCssObject;
  function useStyles(params, options) {
    const theme = MantineProvider.useMantineTheme();
    const themeStyles = MantineProvider.useMantineThemeStyles()[options == null ? void 0 : options.name];
    const { css, cx } = useCss.useCss();
    let count = 0;
    function createRef(refName) {
      count += 1;
      return `mantine-ref_${refName || ""}_${count}`;
    }
    const cssObject = getCssObject(theme, params, createRef);
    const _styles = typeof (options == null ? void 0 : options.styles) === "function" ? options == null ? void 0 : options.styles(theme) : (options == null ? void 0 : options.styles) || {};
    const _themeStyles = typeof themeStyles === "function" ? themeStyles(theme) : themeStyles || {};
    const classes = fromEntries.fromEntries(Object.keys(cssObject).map((key) => {
      const mergedStyles = cx(css(cssObject[key]), css(_themeStyles[key]), css(_styles[key]));
      return [key, mergedStyles];
    }));
    return { classes: mergeClassNames.mergeClassNames(cx, classes, options == null ? void 0 : options.classNames, options == null ? void 0 : options.name), cx, theme };
  }
  return useStyles;
}

exports.createStyles = createStyles;
//# sourceMappingURL=create-styles.js.map
