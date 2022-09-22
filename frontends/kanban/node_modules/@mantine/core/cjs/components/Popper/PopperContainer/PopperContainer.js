'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var styles = require('@mantine/styles');
var Portal = require('../../Portal/Portal.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

function PopperContainer({
  children,
  zIndex = styles.getDefaultZIndex("popover"),
  className,
  withinPortal = true
}) {
  if (withinPortal) {
    return /* @__PURE__ */ React__default.createElement(Portal.Portal, {
      className,
      zIndex
    }, children);
  }
  return /* @__PURE__ */ React__default.createElement("div", {
    className,
    style: { position: "relative", zIndex }
  }, children);
}
PopperContainer.displayName = "@mantine/core/PopperContainer";

exports.PopperContainer = PopperContainer;
//# sourceMappingURL=PopperContainer.js.map
