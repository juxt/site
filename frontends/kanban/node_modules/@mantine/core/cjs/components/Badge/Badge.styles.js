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
  xs: {
    fontSize: 9,
    height: 16
  },
  sm: {
    fontSize: 10,
    height: 18
  },
  md: {
    fontSize: 11,
    height: 20
  },
  lg: {
    fontSize: 13,
    height: 26
  },
  xl: {
    fontSize: 16,
    height: 32
  }
};
const dotSizes = {
  xs: 4,
  sm: 4,
  md: 6,
  lg: 8,
  xl: 10
};
const heights = Object.keys(sizes).reduce((acc, key) => {
  acc[key] = sizes[key].height;
  return acc;
}, {});
var useStyles = styles.createStyles((theme, { color, size, radius, gradientFrom, gradientTo, gradientDeg, fullWidth }) => {
  const dotSize = theme.fn.size({ size, sizes: dotSizes });
  const { fontSize, height } = size in sizes ? sizes[size] : sizes.md;
  const lightColors = styles.getSharedColorScheme({ color, theme, variant: "light" });
  const filledColors = styles.getSharedColorScheme({ theme, color, variant: "filled" });
  const outlineColors = styles.getSharedColorScheme({ theme, color, variant: "outline" });
  const gradientColors = styles.getSharedColorScheme({
    theme,
    variant: "gradient",
    gradient: { from: gradientFrom, to: gradientTo, deg: gradientDeg }
  });
  return {
    leftSection: {
      marginRight: theme.spacing.xs / 2
    },
    rightSection: {
      marginLeft: theme.spacing.xs / 2
    },
    inner: {
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    },
    root: __spreadProps(__spreadValues(__spreadValues({}, theme.fn.focusStyles()), theme.fn.fontStyles()), {
      fontSize,
      height,
      WebkitTapHighlightColor: "transparent",
      lineHeight: `${height - 2}px`,
      textDecoration: "none",
      padding: `0 ${theme.fn.size({ size, sizes: theme.spacing }) / 1.5}px`,
      boxSizing: "border-box",
      display: fullWidth ? "flex" : "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      width: fullWidth ? "100%" : "auto",
      textTransform: "uppercase",
      borderRadius: theme.fn.size({ size: radius, sizes: theme.radius }),
      fontWeight: 700,
      letterSpacing: 0.25,
      cursor: "default",
      textOverflow: "ellipsis",
      overflow: "hidden"
    }),
    light: {
      backgroundColor: lightColors.background,
      color: lightColors.color,
      border: "1px solid transparent"
    },
    filled: {
      backgroundColor: filledColors.background,
      color: filledColors.color,
      border: "1px solid transparent"
    },
    outline: {
      backgroundColor: outlineColors.background,
      color: outlineColors.color,
      border: `1px solid ${outlineColors.border}`
    },
    gradient: {
      backgroundImage: gradientColors.background,
      color: gradientColors.color,
      border: 0
    },
    dot: {
      backgroundColor: "transparent",
      color: theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.colors.gray[7],
      border: `1px solid ${theme.colorScheme === "dark" ? theme.colors.dark[3] : theme.colors.gray[3]}`,
      paddingLeft: theme.fn.size({ size, sizes: theme.spacing }) / 1.5 - dotSize / 2,
      "&::before": {
        content: '""',
        display: "block",
        width: dotSize,
        height: dotSize,
        borderRadius: dotSize,
        backgroundColor: theme.fn.themeColor(color, theme.colorScheme === "dark" ? 4 : 6),
        marginRight: dotSize
      }
    }
  };
});

exports.default = useStyles;
exports.heights = heights;
//# sourceMappingURL=Badge.styles.js.map
