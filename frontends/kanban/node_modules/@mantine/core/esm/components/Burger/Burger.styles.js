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
const sizes = {
  xs: 12,
  sm: 18,
  md: 24,
  lg: 34,
  xl: 42
};
var useStyles = createStyles((theme, { size, color }, getRef) => {
  const sizeValue = theme.fn.size({ size, sizes });
  const opened = { ref: getRef("opened") };
  return {
    opened,
    root: __spreadProps(__spreadValues({}, theme.fn.focusStyles()), {
      WebkitTapHighlightColor: "transparent",
      borderRadius: theme.radius.sm,
      width: sizeValue + theme.spacing.xs,
      height: sizeValue + theme.spacing.xs,
      padding: theme.spacing.xs / 2,
      backgroundColor: "transparent",
      border: 0,
      cursor: "pointer"
    }),
    burger: {
      position: "relative",
      userSelect: "none",
      boxSizing: "border-box",
      "&, &:before, &:after": {
        display: "block",
        width: sizeValue,
        height: Math.ceil(sizeValue / 12),
        backgroundColor: color,
        outline: "1px solid transparent",
        transitionProperty: "background-color, transform",
        transitionDuration: "300ms",
        "@media (prefers-reduced-motion)": {
          transitionDuration: "0ms"
        }
      },
      "&:before, &:after": {
        position: "absolute",
        content: '""',
        left: 0
      },
      "&:before": {
        top: sizeValue / 3 * -1
      },
      "&:after": {
        top: sizeValue / 3
      },
      [`&.${opened.ref}`]: {
        backgroundColor: "transparent",
        "&:before": {
          transform: `translateY(${sizeValue / 3}px) rotate(45deg)`
        },
        "&:after": {
          transform: `translateY(-${sizeValue / 3}px) rotate(-45deg)`
        }
      }
    }
  };
});

export default useStyles;
export { sizes };
//# sourceMappingURL=Burger.styles.js.map
