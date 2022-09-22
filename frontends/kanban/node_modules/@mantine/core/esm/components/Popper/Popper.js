import React, { useState } from 'react';
import { usePopper } from 'react-popper';
import { getDefaultZIndex } from '@mantine/styles';
import { useDidUpdate } from '@mantine/hooks';
import { parsePopperPosition } from './parse-popper-position/parse-popper-position.js';
import { PopperContainer } from './PopperContainer/PopperContainer.js';
import useStyles from './Popper.styles.js';
import { Transition } from '../Transition/Transition.js';

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
function flipPlacement(placement, dir) {
  if (placement === "center") {
    return placement;
  }
  if (dir === "rtl") {
    if (placement === "end") {
      return "start";
    }
    return "end";
  }
  return placement;
}
function flipPosition(position, dir) {
  if (position === "top" || position === "bottom") {
    return position;
  }
  if (dir === "rtl") {
    if (position === "left") {
      return "right";
    }
    return "left";
  }
  return position;
}
function Popper({
  position = "top",
  placement = "center",
  gutter = 5,
  arrowSize = 2,
  arrowDistance = 2,
  withArrow = false,
  referenceElement,
  children,
  mounted,
  transition = "pop-top-left",
  transitionDuration,
  exitTransitionDuration = transitionDuration,
  transitionTimingFunction,
  arrowClassName,
  arrowStyle,
  zIndex = getDefaultZIndex("popover"),
  forceUpdateDependencies = [],
  modifiers = [],
  onTransitionEnd,
  withinPortal = true
}) {
  var _a;
  const padding = withArrow ? gutter + arrowSize : gutter;
  const { classes, cx, theme } = useStyles({ arrowSize, arrowDistance }, { name: "Popper" });
  const [popperElement, setPopperElement] = useState(null);
  const _placement = flipPlacement(placement, theme.dir);
  const _position = flipPosition(position, theme.dir);
  const initialPlacement = _placement === "center" ? _position : `${_position}-${_placement}`;
  const { styles, attributes, forceUpdate } = usePopper(referenceElement, popperElement, {
    placement: initialPlacement,
    modifiers: [
      {
        name: "offset",
        options: {
          offset: [0, padding]
        }
      },
      ...modifiers
    ]
  });
  const parsedAttributes = parsePopperPosition((_a = attributes.popper) == null ? void 0 : _a["data-popper-placement"]);
  useDidUpdate(() => {
    typeof forceUpdate === "function" && forceUpdate();
  }, forceUpdateDependencies);
  return /* @__PURE__ */ React.createElement(Transition, {
    mounted: mounted && !!referenceElement,
    duration: transitionDuration,
    exitDuration: exitTransitionDuration,
    transition,
    timingFunction: transitionTimingFunction,
    onExited: onTransitionEnd
  }, (transitionStyles) => /* @__PURE__ */ React.createElement("div", null, /* @__PURE__ */ React.createElement(PopperContainer, {
    withinPortal,
    zIndex
  }, /* @__PURE__ */ React.createElement("div", __spreadValues({
    ref: setPopperElement,
    style: __spreadProps(__spreadValues({}, styles.popper), { pointerEvents: "none" })
  }, attributes.popper), /* @__PURE__ */ React.createElement("div", {
    style: transitionStyles
  }, children, withArrow && /* @__PURE__ */ React.createElement("div", {
    style: arrowStyle,
    className: cx(classes.arrow, classes[parsedAttributes.placement], classes[parsedAttributes.position], arrowClassName)
  }))))));
}
Popper.displayName = "@mantine/core/Popper";

export { Popper };
//# sourceMappingURL=Popper.js.map
