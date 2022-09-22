import { createStyles } from '@mantine/styles';

var __defProp = Object.defineProperty;
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
var useStyles = createStyles((theme, { radius, shadow, spacing, width }) => ({
  title: {},
  wrapper: __spreadValues({}, theme.fn.focusStyles()),
  popover: {
    background: theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.white,
    pointerEvents: "all",
    borderRadius: theme.fn.size({ size: radius, sizes: theme.radius }),
    width
  },
  body: {
    border: `1px solid ${theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.colors.gray[2]}`,
    boxShadow: shadow in theme.shadows ? theme.shadows[shadow] : shadow,
    borderRadius: theme.fn.size({ size: radius, sizes: theme.radius })
  },
  inner: {
    padding: theme.fn.size({ size: spacing, sizes: theme.spacing })
  },
  header: {
    borderBottom: `1px solid ${theme.colorScheme === "dark" ? theme.colors.dark[7] : theme.colors.gray[2]}`,
    padding: `${theme.spacing.xs / 1.5}px ${theme.fn.size({
      size: spacing,
      sizes: theme.spacing
    })}px`,
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between"
  },
  close: {
    position: "absolute",
    top: 7,
    zIndex: 2,
    right: 10
  }
}));

export default useStyles;
//# sourceMappingURL=PopoverBody.styles.js.map
