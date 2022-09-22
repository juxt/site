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
var useStyles = createStyles((theme, { size }) => ({
  root: __spreadProps(__spreadValues({}, theme.fn.fontStyles()), {
    lineHeight: theme.lineHeight
  }),
  label: {
    display: "inline-block",
    marginBottom: 4,
    fontSize: theme.fn.size({ size, sizes: theme.fontSizes }),
    fontWeight: 500,
    color: theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.colors.gray[9],
    wordBreak: "break-word",
    cursor: "default",
    WebkitTapHighlightColor: "transparent"
  },
  error: {
    marginTop: theme.spacing.xs / 2,
    wordBreak: "break-word",
    color: `${theme.colors.red[theme.colorScheme === "dark" ? 6 : 7]} !important`
  },
  description: {
    marginTop: -3,
    marginBottom: 7,
    wordBreak: "break-word",
    color: `${theme.colorScheme === "dark" ? theme.colors.dark[2] : theme.colors.gray[6]} !important`,
    fontSize: `${theme.fn.size({ size, sizes: theme.fontSizes }) - 2}px !important`,
    lineHeight: 1.2
  },
  required: {
    color: theme.colorScheme === "dark" ? theme.colors.red[5] : theme.colors.red[7]
  }
}));

export default useStyles;
//# sourceMappingURL=InputWrapper.styles.js.map
