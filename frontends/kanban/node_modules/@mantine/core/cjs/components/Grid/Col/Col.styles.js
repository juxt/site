'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');

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
const getColumnWidth = (colSpan, columns) => `${100 / (columns / colSpan)}%`;
const getColumnOffset = (offset, columns) => offset ? `${100 / (columns / offset)}%` : void 0;
function getBreakpointsStyles({
  sizes,
  offsets,
  theme,
  columns,
  grow
}) {
  return styles.MANTINE_SIZES.reduce((acc, size) => {
    if (typeof sizes[size] === "number") {
      acc[`@media (min-width: ${theme.breakpoints[size] + 1}px)`] = {
        flexBasis: getColumnWidth(sizes[size], columns),
        flexShrink: 0,
        maxWidth: grow ? "unset" : getColumnWidth(sizes[size], columns),
        marginLeft: getColumnOffset(offsets[size], columns)
      };
    }
    return acc;
  }, {});
}
var useStyles = styles.createStyles((theme, {
  gutter,
  grow,
  offset,
  offsetXs,
  offsetSm,
  offsetMd,
  offsetLg,
  offsetXl,
  columns,
  span,
  xs,
  sm,
  md,
  lg,
  xl
}) => ({
  root: __spreadValues({
    boxSizing: "border-box",
    flexGrow: grow ? 1 : 0,
    padding: theme.fn.size({ size: gutter, sizes: theme.spacing }) / 2,
    marginLeft: getColumnOffset(offset, columns),
    flexBasis: getColumnWidth(span, columns),
    flexShrink: 0,
    maxWidth: grow ? "unset" : getColumnWidth(span, columns)
  }, getBreakpointsStyles({
    sizes: { xs, sm, md, lg, xl },
    offsets: { xs: offsetXs, sm: offsetSm, md: offsetMd, lg: offsetLg, xl: offsetXl },
    theme,
    columns,
    grow
  }))
}));

exports.default = useStyles;
//# sourceMappingURL=Col.styles.js.map
