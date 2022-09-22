import { createStyles } from '@mantine/styles';

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
var useStyles = createStyles((theme, { color, radius }) => ({
  root: {
    position: "relative",
    display: "inline-block"
  },
  wrapper: {
    background: "transparent",
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    pointerEvents: "none"
  },
  body: __spreadProps(__spreadValues({}, theme.fn.fontStyles()), {
    backgroundColor: theme.fn.themeColor(color, theme.colorScheme === "dark" ? 3 : 9),
    lineHeight: theme.lineHeight,
    fontSize: theme.fontSizes.sm,
    borderRadius: theme.fn.size({ size: radius, sizes: theme.radius }),
    padding: `${theme.spacing.xs / 2}px ${theme.spacing.xs}px`,
    color: theme.colorScheme === "dark" ? theme.colors.dark[9] : theme.white,
    position: "relative",
    overflow: "hidden",
    textOverflow: "ellipsis"
  }),
  tooltip: {
    display: "inline-block",
    position: "absolute"
  },
  arrow: {
    border: 0,
    background: theme.fn.themeColor(color, theme.colorScheme === "dark" ? 3 : 9),
    zIndex: 1
  }
}));

export default useStyles;
//# sourceMappingURL=Tooltip.styles.js.map
