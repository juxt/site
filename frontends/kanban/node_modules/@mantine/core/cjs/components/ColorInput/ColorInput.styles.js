'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');

var useStyles = styles.createStyles((theme, { disallowInput }) => ({
  dropdownBody: {
    backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.white,
    border: `1px solid ${theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.colors.gray[2]}`
  },
  input: {
    cursor: disallowInput ? "pointer" : void 0
  }
}));

exports.default = useStyles;
//# sourceMappingURL=ColorInput.styles.js.map
