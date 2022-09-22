'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');

var useStyles = styles.createStyles((theme, { padding, first, last }) => ({
  cardSection: {
    display: "block",
    marginLeft: -1 * theme.fn.size({ size: padding, sizes: theme.spacing }),
    marginRight: -1 * theme.fn.size({ size: padding, sizes: theme.spacing }),
    marginTop: first ? -1 * theme.fn.size({ size: padding, sizes: theme.spacing }) : void 0,
    marginBottom: last ? -1 * theme.fn.size({ size: padding, sizes: theme.spacing }) : void 0
  }
}));

exports.default = useStyles;
//# sourceMappingURL=CardSection.styles.js.map
