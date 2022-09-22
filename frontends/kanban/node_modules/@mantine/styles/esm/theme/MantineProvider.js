import React, { createContext, useContext } from 'react';
import { Global } from '@emotion/react';
import { DEFAULT_THEME } from './default-theme.js';
import { mergeTheme } from './utils/merge-theme/merge-theme.js';
import { NormalizeCSS } from './NormalizeCSS.js';

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
const MantineThemeContext = createContext({
  theme: DEFAULT_THEME,
  styles: {},
  emotionOptions: { key: "mantine", prepend: true }
});
function useMantineTheme() {
  var _a;
  return ((_a = useContext(MantineThemeContext)) == null ? void 0 : _a.theme) || DEFAULT_THEME;
}
function useMantineThemeStyles() {
  var _a;
  return ((_a = useContext(MantineThemeContext)) == null ? void 0 : _a.styles) || {};
}
function useMantineEmotionOptions() {
  var _a;
  return ((_a = useContext(MantineThemeContext)) == null ? void 0 : _a.emotionOptions) || { key: "mantine", prepend: true };
}
function GlobalStyles() {
  const theme = useMantineTheme();
  return /* @__PURE__ */ React.createElement(Global, {
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
  return /* @__PURE__ */ React.createElement(MantineThemeContext.Provider, {
    value: { theme: mergeTheme(DEFAULT_THEME, theme), styles, emotionOptions }
  }, withNormalizeCSS && /* @__PURE__ */ React.createElement(NormalizeCSS, null), withGlobalStyles && /* @__PURE__ */ React.createElement(GlobalStyles, null), children);
}
MantineProvider.displayName = "@mantine/core/MantineProvider";

export { MantineProvider, useMantineEmotionOptions, useMantineTheme, useMantineThemeStyles };
//# sourceMappingURL=MantineProvider.js.map
