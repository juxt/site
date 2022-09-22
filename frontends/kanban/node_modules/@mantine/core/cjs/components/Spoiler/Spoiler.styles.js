'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');

var useStyles = styles.createStyles((_theme, { transitionDuration }) => ({
  control: {},
  root: {
    position: "relative"
  },
  content: {
    overflow: "hidden",
    transitionProperty: "max-height",
    transitionTimingFunction: "ease",
    transitionDuration: `${transitionDuration}ms`,
    "@media (prefers-reduced-motion)": {
      transitionDuration: "0ms"
    }
  }
}));

exports.default = useStyles;
//# sourceMappingURL=Spoiler.styles.js.map
