import React, { forwardRef, useRef, Children } from 'react';
import { useUncontrolled, clamp } from '@mantine/hooks';
import { TabControl } from './TabControl/TabControl.js';
import useStyles from './Tabs.styles.js';
import { Box } from '../Box/Box.js';
import { Group } from '../Group/Group.js';

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
function getPreviousTab(active, tabs) {
  for (let i = active - 1; i >= 0; i -= 1) {
    if (!tabs[i].props.disabled) {
      return i;
    }
  }
  return active;
}
function getNextTab(active, tabs) {
  for (let i = active + 1; i < tabs.length; i += 1) {
    if (!tabs[i].props.disabled) {
      return i;
    }
  }
  return active;
}
function findInitialTab(tabs) {
  for (let i = 0; i < tabs.length; i += 1) {
    if (!tabs[i].props.disabled) {
      return i;
    }
  }
  return -1;
}
const Tabs = forwardRef((_a, ref) => {
  var _b = _a, {
    className,
    children,
    initialTab,
    active,
    position = "left",
    grow = false,
    onTabChange,
    color,
    variant = "default",
    classNames,
    styles,
    tabPadding = "xs",
    orientation = "horizontal"
  } = _b, others = __objRest(_b, [
    "className",
    "children",
    "initialTab",
    "active",
    "position",
    "grow",
    "onTabChange",
    "color",
    "variant",
    "classNames",
    "styles",
    "tabPadding",
    "orientation"
  ]);
  const { classes, cx, theme } = useStyles({ tabPadding, orientation }, { classNames, styles, name: "Tabs" });
  const controlRefs = useRef({});
  const tabs = Children.toArray(children);
  const [_activeTab, handleActiveTabChange] = useUncontrolled({
    value: active,
    defaultValue: initialTab,
    finalValue: findInitialTab(tabs),
    rule: (value) => typeof value === "number",
    onChange: (value) => {
      if (onTabChange) {
        tabs.some((tab) => tab.props.tabKey) ? onTabChange(value, tabs[value].props.tabKey) : onTabChange(value);
      }
    }
  });
  const activeTab = clamp({ value: _activeTab, min: 0, max: tabs.length - 1 });
  const nextTabCode = orientation === "horizontal" ? theme.dir === "ltr" ? "ArrowRight" : "ArrowLeft" : "ArrowDown";
  const previousTabCode = orientation === "horizontal" ? theme.dir === "ltr" ? "ArrowLeft" : "ArrowRight" : "ArrowUp";
  const handleKeyDown = (event) => {
    if (event.nativeEvent.code === nextTabCode) {
      event.preventDefault();
      const nextTab = getNextTab(activeTab, tabs);
      handleActiveTabChange(nextTab);
      controlRefs.current[nextTab].focus();
    }
    if (event.nativeEvent.code === previousTabCode) {
      event.preventDefault();
      const previousTab = getPreviousTab(activeTab, tabs);
      handleActiveTabChange(previousTab);
      controlRefs.current[previousTab].focus();
    }
  };
  const panes = tabs.map((tab, index) => React.cloneElement(tab, {
    key: index,
    active: activeTab === index,
    onKeyDown: handleKeyDown,
    color: tab.props.color || color,
    variant,
    orientation,
    elementRef: (node) => {
      controlRefs.current[index] = node;
    },
    onClick: () => activeTab !== index && handleActiveTabChange(index),
    classNames,
    styles
  }));
  const content = tabs[activeTab].props.children;
  return /* @__PURE__ */ React.createElement(Box, __spreadValues({
    ref,
    className: cx(classes.root, className)
  }, others), /* @__PURE__ */ React.createElement("div", {
    className: cx(classes.tabsListWrapper, classes[variant])
  }, /* @__PURE__ */ React.createElement(Group, {
    className: classes.tabsList,
    role: "tablist",
    direction: orientation === "horizontal" ? "row" : "column",
    "aria-orientation": orientation,
    spacing: variant === "pills" ? 5 : 0,
    position,
    grow
  }, panes)), content && /* @__PURE__ */ React.createElement("div", {
    role: "tabpanel",
    className: classes.body,
    key: activeTab
  }, content));
});
Tabs.displayName = "@mantine/core/Tabs";
Tabs.Tab = TabControl;

export { Tabs };
//# sourceMappingURL=Tabs.js.map
