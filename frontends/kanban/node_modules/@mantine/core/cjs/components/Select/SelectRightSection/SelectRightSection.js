'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var React = require('react');
var CloseButton = require('../../ActionIcon/CloseButton/CloseButton.js');
var ChevronIcon = require('./ChevronIcon.js');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e['default'] : e; }

var React__default = /*#__PURE__*/_interopDefaultLegacy(React);

function SelectRightSection({
  shouldClear,
  clearButtonLabel,
  onClear,
  size,
  error
}) {
  return shouldClear ? /* @__PURE__ */ React__default.createElement(CloseButton.CloseButton, {
    variant: "transparent",
    "aria-label": clearButtonLabel,
    onClick: onClear,
    size
  }) : /* @__PURE__ */ React__default.createElement(ChevronIcon.ChevronIcon, {
    error,
    size
  });
}
SelectRightSection.displayName = "@mantine/core/SelectRightSection";

exports.SelectRightSection = SelectRightSection;
//# sourceMappingURL=SelectRightSection.js.map
