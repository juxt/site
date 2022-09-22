'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var getSortedBreakpoints = require('../get-sorted-breakpoints/get-sorted-breakpoints.js');

function getNavbarBreakpoints(element, theme) {
  var _a;
  const breakpoints = (_a = element == null ? void 0 : element.props) == null ? void 0 : _a.width;
  return breakpoints != null ? getSortedBreakpoints.getSortedBreakpoints(breakpoints, theme) : [];
}

exports.getNavbarBreakpoints = getNavbarBreakpoints;
//# sourceMappingURL=get-navbar-breakpoints.js.map
