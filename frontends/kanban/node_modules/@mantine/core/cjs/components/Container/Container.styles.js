'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');

const sizes = {
  xs: 540,
  sm: 720,
  md: 960,
  lg: 1140,
  xl: 1320
};
var useStyles = styles.createStyles((theme, { fluid, size, padding }) => ({
  root: {
    maxWidth: fluid ? "100%" : theme.fn.size({ size, sizes }),
    marginLeft: "auto",
    marginRight: "auto",
    paddingLeft: theme.fn.size({ size: padding, sizes: theme.spacing }),
    paddingRight: theme.fn.size({ size: padding, sizes: theme.spacing })
  }
}));

exports.default = useStyles;
exports.sizes = sizes;
//# sourceMappingURL=Container.styles.js.map
