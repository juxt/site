'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var react = require('@emotion/react');
var defaultTheme = require('./default-theme.js');
var mergeTheme = require('./utils/merge-theme/merge-theme.js');
var NormalizeCSS = require('./NormalizeCSS.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

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
const MantineThemeContext = React.createContext({
  theme: defaultTheme.DEFAULT_THEME,
  styles: {},
  emotionOptions: { key: "mantine", prepend: true }
});
function useMantineTheme() {
  var _a;
  return ((_a = React.useContext(MantineThemeContext)) == null ? void 0 : _a.theme) || defaultTheme.DEFAULT_THEME;
}
function useMantineThemeStyles() {
  var _a;
  return ((_a = React.useContext(MantineThemeContext)) == null ? void 0 : _a.styles) || {};
}
function useMantineEmotionOptions() {
  var _a;
  return ((_a = React.useContext(MantineThemeContext)) == null ? void 0 : _a.emotionOptions) || { key: "mantine", prepend: true };
}
function GlobalStyles() {
  const theme = useMantineTheme();
  return /* @__PURE__ */ React__default.createElement(react.Global, {
    styles: {
      "*, *::before, *::after": {
        boxSizing: "border-box"
      },
      body: __spreadProps(__spreadValues({}, theme.fn.fontStyles()), {
        backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[7] : theme.white,
        color: theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.black,
        lineHeight: theme.lineHeight,
        fontSize: theme.fontSizes.md
      })
    }
  });
}
function MantineProvider({
  theme,
  styles = {},
  emotionOptions,
  withNormalizeCSS = false,
  withGlobalStyles = false,
  children
}) {
  return /* @__PURE__ */ React__default.createElement(MantineThemeContext.Provider, {
    value: { theme: mergeTheme.mergeTheme(defaultTheme.DEFAULT_THEME, theme), styles, emotionOptions }
  }, withNormalizeCSS && /* @__PURE__ */ React__default.createElement(NormalizeCSS.NormalizeCSS, null), withGlobalStyles && /* @__PURE__ */ React__default.createElement(GlobalStyles, null), children);
}
MantineProvider.displayName = "@mantine/core/MantineProvider";

exports.MantineProvider = MantineProvider;
exports.useMantineEmotionOptions = useMantineEmotionOptions;
exports.useMantineTheme = useMantineTheme;
exports.useMantineThemeStyles = useMantineThemeStyles;
//# sourceMappingURL=MantineProvider.js.map
