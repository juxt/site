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
var useStyles = createStyles((theme, { color, radius, variant }, getRef) => {
  const closeButton = getRef("closeButton");
  return {
    root: __spreadProps(__spreadValues({}, theme.fn.fontStyles()), {
      position: "relative",
      overflow: "hidden",
      padding: `${theme.spacing.sm}px ${theme.spacing.md}px`,
      borderRadius: theme.fn.size({ size: radius, sizes: theme.radius }),
      border: "1px solid transparent"
    }),
    wrapper: {
      display: "flex"
    },
    body: {
      flex: 1
    },
    title: {
      boxSizing: "border-box",
      margin: 0,
      marginBottom: 7,
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      lineHeight: theme.lineHeight,
      fontSize: theme.fontSizes.sm,
      fontWeight: 700
    },
    label: {
      display: "block",
      overflow: "hidden",
      textOverflow: "ellipsis"
    },
    light: {
      backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[5] : theme.fn.themeColor(color, 0),
      color: theme.fn.themeColor(color, theme.colorScheme === "dark" ? 5 : 7)
    },
    filled: {
      backgroundColor: theme.fn.rgba(theme.fn.themeColor(color, 8), theme.colorScheme === "dark" ? 0.65 : 1),
      color: theme.white,
      [`& .${closeButton}`]: {
        color: theme.white
      }
    },
    outline: {
      color: theme.fn.themeColor(color, theme.colorScheme === "dark" ? 5 : 6),
      borderColor: theme.fn.themeColor(color, theme.colorScheme === "dark" ? 5 : 6)
    },
    icon: {
      lineHeight: 1,
      width: 20,
      height: 20,
      display: "flex",
      alignItems: "center",
      justifyContent: "flex-start",
      marginRight: theme.spacing.md,
      marginTop: 1
    },
    message: __spreadProps(__spreadValues({}, theme.fn.fontStyles()), {
      lineHeight: theme.lineHeight,
      textOverflow: "ellipsis",
      overflow: "hidden",
      fontSize: theme.fontSizes.sm,
      color: variant === "filled" ? theme.white : theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.black
    }),
    closeButton: {
      ref: closeButton,
      marginTop: 2
    }
  };
});

export default useStyles;
//# sourceMappingURL=Alert.styles.js.map
