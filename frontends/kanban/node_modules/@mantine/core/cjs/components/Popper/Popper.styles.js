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
const horizontalPlacement = (arrowSize, distance, classes) => ({
  [`&.${classes.center}`]: {
    top: `calc(50% - ${arrowSize}px)`
  },
  [`&.${classes.end}`]: {
    bottom: arrowSize * distance
  },
  [`&.${classes.start}`]: {
    top: arrowSize * distance
  }
});
const verticalPlacement = (arrowSize, distance, classes, theme) => ({
  [`&.${classes.center}`]: {
    left: `calc(50% - ${arrowSize}px)`
  },
  [`&.${classes.end}`]: {
    right: theme.dir === "ltr" ? arrowSize * distance : void 0,
    left: theme.dir === "rtl" ? arrowSize * distance : void 0
  },
  [`&.${classes.start}`]: {
    left: theme.dir === "ltr" ? arrowSize * distance : void 0,
    right: theme.dir === "rtl" ? arrowSize * distance : void 0
  }
});
var useStyles = styles.createStyles((theme, { arrowSize, arrowDistance }, getRef) => {
  const center = { ref: getRef("center") };
  const start = { ref: getRef("start") };
  const end = { ref: getRef("end") };
  const placementClasses = {
    center: center.ref,
    start: start.ref,
    end: end.ref
  };
  return {
    center,
    start,
    end,
    arrow: {
      width: arrowSize * 2,
      height: arrowSize * 2,
      position: "absolute",
      transform: "rotate(45deg)",
      border: "1px solid transparent",
      zIndex: 1
    },
    left: __spreadProps(__spreadValues({}, horizontalPlacement(arrowSize, arrowDistance, placementClasses)), {
      right: theme.dir === "ltr" ? -arrowSize : "unset",
      left: theme.dir === "rtl" ? -arrowSize : "unset",
      borderLeft: theme.dir === "ltr" ? 0 : void 0,
      borderRight: theme.dir === "rtl" ? 0 : void 0,
      borderBottom: 0
    }),
    right: __spreadProps(__spreadValues({}, horizontalPlacement(arrowSize, arrowDistance, placementClasses)), {
      left: theme.dir === "ltr" ? -arrowSize : "unset",
      right: theme.dir === "rtl" ? -arrowSize : "unset",
      borderRight: theme.dir === "ltr" ? 0 : void 0,
      borderLeft: theme.dir === "rtl" ? 0 : void 0,
      borderTop: 0
    }),
    top: __spreadProps(__spreadValues({}, verticalPlacement(arrowSize, arrowDistance, placementClasses, theme)), {
      bottom: -arrowSize,
      borderLeft: theme.dir === "ltr" ? 0 : void 0,
      borderRight: theme.dir === "rtl" ? 0 : void 0,
      borderTop: 0
    }),
    bottom: __spreadProps(__spreadValues({}, verticalPlacement(arrowSize, arrowDistance, placementClasses, theme)), {
      top: -arrowSize,
      borderRight: theme.dir === "ltr" ? 0 : void 0,
      borderLeft: theme.dir === "rtl" ? 0 : void 0,
      borderBottom: 0
    })
  };
});

exports.default = useStyles;
//# sourceMappingURL=Popper.styles.js.map
