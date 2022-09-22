import { createStyles, getSharedColorScheme } from '@mantine/styles';

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
  xs: 16,
  sm: 26,
  md: 38,
  lg: 56,
  xl: 84
};
var useStyles = createStyles((theme, { size, radius, color }) => ({
  root: __spreadProps(__spreadValues({}, theme.fn.focusStyles()), {
    WebkitTapHighlightColor: "transparent",
    boxSizing: "border-box",
    position: "relative",
    userSelect: "none",
    overflow: "hidden",
    width: theme.fn.size({ size, sizes }),
    minWidth: theme.fn.size({ size, sizes }),
    height: theme.fn.size({ size, sizes }),
    borderRadius: radius ? theme.fn.size({ size: radius, sizes: theme.radius }) : size
  }),
  image: {
    objectFit: "cover",
    width: "100%",
    height: "100%",
    display: "block"
  },
  placeholder: __spreadProps(__spreadValues({}, theme.fn.fontStyles()), {
    fontSize: theme.fn.size({ size, sizes }) / 2.5,
    color: getSharedColorScheme({ theme, color, variant: "light" }).color,
    fontWeight: 700,
    backgroundColor: getSharedColorScheme({ theme, color, variant: "light" }).background,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    width: "100%",
    height: "100%",
    userSelect: "none"
  }),
  placeholderIcon: {
    width: "70%",
    height: "70%",
    color: getSharedColorScheme({ theme, color, variant: "light" }).color
  }
}));

export default useStyles;
export { sizes };
//# sourceMappingURL=Avatar.styles.js.map
