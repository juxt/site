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
var useStyles = createStyles((theme, { color, orientation }, getRef) => {
  const tabActive = { ref: getRef("tabActive") };
  return {
    tabActive,
    tabLabel: {},
    tabControl: __spreadProps(__spreadValues(__spreadValues({}, theme.fn.fontStyles()), theme.fn.focusStyles()), {
      WebkitTapHighlightColor: "transparent",
      boxSizing: "border-box",
      display: "block",
      height: 40,
      backgroundColor: "transparent",
      border: 0,
      padding: `0 ${theme.spacing.md}px`,
      fontSize: theme.fontSizes.sm,
      cursor: "pointer",
      width: orientation === "vertical" ? "100%" : "auto",
      "&:disabled": {
        cursor: "not-allowed",
        color: theme.colorScheme === "dark" ? theme.colors.dark[3] : theme.colors.gray[5]
      }
    }),
    default: {
      transition: "border-color 150ms ease, color 150ms ease",
      color: theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.black,
      [orientation === "horizontal" ? "borderBottom" : "borderRight"]: "2px solid transparent",
      [`&.${tabActive.ref}`]: {
        color: theme.fn.themeColor(color, theme.colorScheme === "dark" ? 4 : 7),
        [orientation === "horizontal" ? "borderBottomColor" : "borderRightColor"]: theme.fn.themeColor(color, theme.colorScheme === "dark" ? 4 : 7)
      }
    },
    outline: {
      borderBottomLeftRadius: orientation === "vertical" ? theme.radius.sm : 0,
      borderTopRightRadius: orientation === "horizontal" ? theme.radius.sm : 0,
      borderTopLeftRadius: theme.radius.sm,
      border: "1px solid transparent",
      borderBottom: orientation === "horizontal" ? 0 : "1px solid transparent",
      borderRight: orientation === "vertical" ? 0 : "1px solid transparent",
      color: theme.colorScheme === "dark" ? theme.colors.dark[1] : theme.colors.gray[7],
      [`&.${tabActive.ref}`]: {
        color: theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.black,
        borderColor: theme.colorScheme === "dark" ? theme.colors.dark[4] : theme.colors.gray[2],
        background: theme.colorScheme === "dark" ? theme.colors.dark[7] : theme.white
      }
    },
    pills: {
      borderRadius: theme.radius.sm,
      backgroundColor: "transparent",
      color: theme.colorScheme === "dark" ? theme.colors.dark[1] : theme.colors.gray[7],
      fontSize: theme.fontSizes.sm,
      height: "auto",
      padding: `${theme.spacing.xs}px ${theme.spacing.lg}px`,
      fontWeight: 500,
      "&:hover": {
        background: theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.colors.gray[0]
      },
      [`&.${tabActive.ref}`]: {
        color: theme.colorScheme === "dark" ? theme.white : theme.black,
        background: theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.colors.gray[0]
      }
    },
    tabInner: {
      boxSizing: "border-box",
      display: "flex",
      alignItems: "center",
      justifyContent: orientation === "horizontal" ? "center" : "flex-start",
      lineHeight: 1,
      height: "100%"
    },
    tabIcon: {
      "&:not(:only-child)": {
        marginRight: theme.spacing.xs
      },
      "& *": {
        display: "block"
      }
    }
  };
});

export default useStyles;
//# sourceMappingURL=TabControl.styles.js.map
