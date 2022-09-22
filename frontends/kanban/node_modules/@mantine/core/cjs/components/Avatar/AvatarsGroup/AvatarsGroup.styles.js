'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');

var __defProp = Object.defineProperty;
var __defProps = Object.defineProperties;
var __getOwnPropDescs = Object.getOwnPropertyDescriptors;
var __getOwnPropSymbols = Object.getOwnPropertySymbols;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __propIsEnum = Object.prototype.propertyIsEnumerable;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __spreadValues = (a, b) => {
  for (var prop in b || (b = {}))
    if (__hasOwnProp.call(b, prop))
      __defNormalProp(a, prop, b[prop]);
  if (__getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(b)) {
      if (__propIsEnum.call(b, prop))
        __defNormalProp(a, prop, b[prop]);
    }
  return a;
};
var __spreadProps = (a, b) => __defProps(a, __getOwnPropDescs(b));
var useStyles = styles.createStyles((theme, { spacing }) => ({
  root: {
    display: "flex",
    paddingLeft: theme.fn.size({ size: spacing, sizes: theme.spacing }) / 2
  },
  child: {
    marginLeft: -theme.fn.size({ size: spacing, sizes: theme.spacing }) / 2,
    background: `${theme.colorScheme === "dark" ? theme.colors.dark[7] : theme.white}`,
    border: `2px solid ${theme.colorScheme === "dark" ? theme.colors.dark[7] : theme.white}`
  },
  truncated: __spreadProps(__spreadValues({}, theme.fn.fontStyles()), {
    lineHeight: 1,
    fontSize: `${theme.fontSizes.sm}px !important`,
    color: theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.black,
    width: "100%",
    height: "100%",
    backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[5] : theme.colors.gray[0]
  })
}));

exports.default = useStyles;
//# sourceMappingURL=AvatarsGroup.styles.js.map
