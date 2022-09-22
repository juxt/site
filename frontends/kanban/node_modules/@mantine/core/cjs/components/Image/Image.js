'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var hooks = require('@mantine/hooks');
var ImageIcon = require('./ImageIcon.js');
var Image_styles = require('./Image.styles.js');
var Box = require('../Box/Box.js');
var Text = require('../Text/Text.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

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
var __objRest = (source, exclude) => {
  var target = {};
  for (var prop in source)
    if (__hasOwnProp.call(source, prop) && exclude.indexOf(prop) < 0)
      target[prop] = source[prop];
  if (source != null && __getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(source)) {
      if (exclude.indexOf(prop) < 0 && __propIsEnum.call(source, prop))
        target[prop] = source[prop];
    }
  return target;
};
const Image = React.forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    alt,
    src,
    fit = "cover",
    width = "100%",
    height = "auto",
    radius = 0,
    imageProps,
    withPlaceholder = false,
    placeholder,
    imageRef,
    classNames,
    styles,
    caption
  } = _b, others = __objRest(_b, [
    "className",
    "alt",
    "src",
    "fit",
    "width",
    "height",
    "radius",
    "imageProps",
    "withPlaceholder",
    "placeholder",
    "imageRef",
    "classNames",
    "styles",
    "caption"
  ]);
  const { classes, cx } = Image_styles['default']({ radius }, { classNames, styles, name: "Image" });
  const [loaded, setLoaded] = React.useState(false);
  const [error, setError] = React.useState(!src);
  const isPlaceholder = withPlaceholder && (!loaded || error);
  hooks.useDidUpdate(() => {
    setLoaded(false);
    setError(false);
  }, [src]);
  return /* @__PURE__ */ React__default.createElement(Box.Box, __spreadValues({
    className: cx(classes.root, className),
    ref
  }, others), /* @__PURE__ */ React__default.createElement("figure", {
    className: classes.figure
  }, /* @__PURE__ */ React__default.createElement("div", {
    className: classes.imageWrapper
  }, /* @__PURE__ */ React__default.createElement("img", __spreadValues({
    className: classes.image,
    src,
    alt,
    style: { objectFit: fit, width, height },
    ref: imageRef,
    onLoad: (event) => {
      setLoaded(true);
      typeof (imageProps == null ? void 0 : imageProps.onLoad) === "function" && imageProps.onLoad(event);
    },
    onError: (event) => {
      setError(true);
      typeof (imageProps == null ? void 0 : imageProps.onError) === "function" && imageProps.onError(event);
    }
  }, imageProps)), isPlaceholder && /* @__PURE__ */ React__default.createElement("div", {
    className: classes.placeholder,
    title: alt
  }, placeholder || /* @__PURE__ */ React__default.createElement(ImageIcon.ImageIcon, {
    style: { width: 40, height: 40 }
  }))), !!caption && /* @__PURE__ */ React__default.createElement(Text.Text, {
    component: "figcaption",
    size: "sm",
    align: "center",
    className: classes.caption
  }, caption)));
});
Image.displayName = "@mantine/core/Image";

exports.Image = Image;
//# sourceMappingURL=Image.js.map
