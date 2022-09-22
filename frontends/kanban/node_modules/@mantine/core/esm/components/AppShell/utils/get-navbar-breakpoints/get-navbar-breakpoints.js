import { getSortedBreakpoints } from '../get-sorted-breakpoints/get-sorted-breakpoints.js';

function getNavbarBreakpoints(element, theme) {
  var _a;
  const breakpoints = (_a = element == null ? void 0 : element.props) == null ? void 0 : _a.width;
  return breakpoints != null ? getSortedBreakpoints(breakpoints, theme) : [];
}

export { getNavbarBreakpoints };
//# sourceMappingURL=get-navbar-breakpoints.js.map
