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
const sizes = {
  xs: 18,
  sm: 22,
  md: 28,
  lg: 34,
  xl: 44
};
function getVariantStyles({ variant, theme, color }) {
  if (variant === "hover" || variant === "transparent") {
    return {
      border: "1px solid transparent",
      color: theme.fn.themeColor(color, theme.colorScheme === "dark" ? 4 : 7),
      backgroundColor: "transparent",
      "&:hover": variant === "transparent" ? {} : {
        backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[8] : theme.fn.themeColor(color, 0)
      }
    };
  }
  const colors = styles.getSharedColorScheme({ theme, color, variant });
  return {
    backgroundColor: colors.background,
    color: colors.color,
    border: `1px solid ${colors.border}`,
    "&:hover": {
      backgroundColor: colors.hover
    }
  };
}
var useStyles = styles.createStyles((theme, { color, size, radius }) => ({
  root: __spreadProps(__spreadValues(__spreadValues({}, theme.fn.focusStyles()), theme.fn.fontStyles()), {
    position: "relative",
    appearance: "none",
    WebkitAppearance: "none",
    WebkitTapHighlightColor: "transparent",
    boxSizing: "border-box",
    height: theme.fn.size({ size, sizes }),
    minHeight: theme.fn.size({ size, sizes }),
    width: theme.fn.size({ size, sizes }),
    minWidth: theme.fn.size({ size, sizes }),
    borderRadius: theme.fn.size({ size: radius, sizes: theme.radius }),
    padding: 0,
    lineHeight: 1,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    cursor: "pointer",
    "&:disabled": {
      color: theme.colors.gray[theme.colorScheme === "dark" ? 6 : 4],
      cursor: "not-allowed",
      backgroundColor: theme.fn.themeColor("gray", theme.colorScheme === "dark" ? 8 : 1),
      borderColor: theme.fn.themeColor("gray", theme.colorScheme === "dark" ? 8 : 1)
    },
    "&:not(:disabled):active": {
      transform: "translateY(1px)"
    }
  }),
  outline: getVariantStyles({ theme, color, variant: "outline" }),
  filled: getVariantStyles({ theme, color, variant: "filled" }),
  default: getVariantStyles({ theme, color, variant: "default" }),
  light: getVariantStyles({ theme, color, variant: "light" }),
  hover: getVariantStyles({ theme, color, variant: "hover" }),
  transparent: getVariantStyles({ theme, color, variant: "transparent" }),
  loading: {
    "&::before": {
      content: '""',
      position: "absolute",
      top: -1,
      left: -1,
      right: -1,
      bottom: -1,
      backgroundColor: theme.colorScheme === "dark" ? theme.fn.rgba(theme.colors.dark[7], 0.5) : "rgba(255, 255, 255, .5)",
      borderRadius: theme.fn.size({ size: radius, sizes: theme.radius }) - 1,
      cursor: "not-allowed"
    }
  }
}));

exports.default = useStyles;
exports.sizes = sizes;
//# sourceMappingURL=ActionIcon.styles.js.map
