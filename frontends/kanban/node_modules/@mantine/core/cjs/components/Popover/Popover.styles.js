'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');

var useStyles = styles.createStyles((theme) => ({
  root: {
    position: "relative",
    display: "inline-block"
  },
  arrow: {
    borderColor: theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.colors.gray[2],
    background: theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.white
  },
  target: {
    zIndex: 1
  }
}));

exports.default = useStyles;
//# sourceMappingURL=Popover.styles.js.map
