function getNavbarBaseWidth(element) {
  var _a, _b;
  const width = (_b = (_a = element == null ? void 0 : element.props) == null ? void 0 : _a.width) == null ? void 0 : _b.base;
  return typeof width === "number" ? `${width}px` : typeof width === "string" ? width : "0px";
}

export { getNavbarBaseWidth };
//# sourceMappingURL=get-navbar-base-width.js.map
