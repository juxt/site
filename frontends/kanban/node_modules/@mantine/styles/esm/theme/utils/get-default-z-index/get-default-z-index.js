const elevations = {
  app: 100,
  modal: 200,
  popover: 300,
  overlay: 400
};
function getDefaultZIndex(level) {
  return elevations[level];
}

export { getDefaultZIndex };
//# sourceMappingURL=get-default-z-index.js.map
