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
var useStyles = styles.createStyles((theme, { radius, color }) => ({
  item: __spreadProps(__spreadValues({}, theme.fn.fontStyles()), {
    WebkitTapHighlightColor: "transparent",
    fontSize: theme.fontSizes.sm,
    border: 0,
    backgroundColor: "transparent",
    outline: 0,
    width: "100%",
    textAlign: "left",
    display: "inline-block",
    textDecoration: "none",
    boxSizing: "border-box",
    padding: `${theme.spacing.xs}px ${theme.spacing.sm}px`,
    cursor: "pointer",
    borderRadius: theme.fn.size({ size: radius, sizes: theme.radius }),
    color: color ? theme.fn.themeColor(color, theme.colorScheme === "dark" ? 5 : 7) : theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.black,
    "&:disabled": {
      color: theme.colorScheme === "dark" ? theme.colors.dark[3] : theme.colors.gray[5],
      pointerEvents: "none"
    }
  }),
  itemHovered: {
    backgroundColor: color ? theme.fn.rgba(theme.fn.themeColor(color, theme.colorScheme === "dark" ? 9 : 0), theme.colorScheme === "dark" ? 0.2 : 1) : theme.colorScheme === "dark" ? theme.fn.rgba(theme.colors.dark[3], 0.35) : theme.colors.gray[0]
  },
  itemInner: {
    display: "flex",
    alignItems: "center",
    height: "100%"
  },
  itemBody: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    flex: 1
  },
  itemIcon: {
    marginRight: theme.spacing.xs,
    "& *": {
      display: "block"
    }
  },
  itemLabel: {
    lineHeight: 1
  }
}));

exports.default = useStyles;
//# sourceMappingURL=MenuItem.styles.js.map
