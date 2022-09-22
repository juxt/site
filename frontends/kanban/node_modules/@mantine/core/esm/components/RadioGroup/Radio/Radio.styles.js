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
  sm: 16,
  md: 20,
  lg: 24,
  xl: 36
};
var useStyles = createStyles((theme, { size, color }, getRef) => {
  const labelDisabled = { ref: getRef("labelDisabled") };
  return {
    labelDisabled,
    radioWrapper: {
      display: "flex",
      alignItems: "center",
      WebkitTapHighlightColor: "transparent"
    },
    radio: __spreadProps(__spreadValues({}, theme.fn.focusStyles()), {
      backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[4] : theme.white,
      border: `1px solid ${theme.colorScheme === "dark" ? theme.colors.dark[4] : theme.colors.gray[4]}`,
      position: "relative",
      appearance: "none",
      width: theme.fn.size({ sizes, size }),
      height: theme.fn.size({ sizes, size }),
      borderRadius: theme.fn.size({ sizes, size }),
      margin: 0,
      marginRight: theme.spacing.sm,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      "&:checked": {
        background: theme.fn.themeColor(color, 6),
        borderColor: theme.fn.themeColor(color, 6),
        "&::before": {
          content: '""',
          display: "block",
          backgroundColor: theme.white,
          width: theme.fn.size({ sizes, size }) / 2,
          height: theme.fn.size({ sizes, size }) / 2,
          borderRadius: theme.fn.size({ sizes, size }) / 2
        }
      },
      "&:disabled": {
        borderColor: theme.colorScheme === "dark" ? theme.colors.dark[5] : theme.colors.gray[4],
        backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[5] : theme.colors.gray[1],
        "&::before": {
          backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.colors.gray[4]
        }
      }
    }),
    label: __spreadProps(__spreadValues({}, theme.fn.fontStyles()), {
      display: "flex",
      alignItems: "center",
      fontSize: theme.fontSizes[size] || theme.fontSizes.md,
      lineHeight: `${theme.fn.size({ sizes, size })}px`,
      color: theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.black,
      [`&.${labelDisabled.ref}`]: {
        color: theme.colorScheme === "dark" ? theme.colors.dark[3] : theme.colors.gray[5]
      }
    })
  };
});

export default useStyles;
export { sizes };
//# sourceMappingURL=Radio.styles.js.map
