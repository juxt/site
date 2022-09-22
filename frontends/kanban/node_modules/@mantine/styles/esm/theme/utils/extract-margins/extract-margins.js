var __getOwnPropSymbols = Object.getOwnPropertySymbols;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __propIsEnum = Object.prototype.propertyIsEnumerable;
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
function extractMargins(others) {
  const _a = others, { m, mx, my, mt, mb, ml, mr } = _a, rest = __objRest(_a, ["m", "mx", "my", "mt", "mb", "ml", "mr"]);
  const margins = { m, mx, my, mt, mb, ml, mr };
  Object.keys(margins).forEach((key) => {
    if (margins[key] === void 0) {
      delete margins[key];
    }
  });
  return { margins, rest };
}

export { extractMargins };
//# sourceMappingURL=extract-margins.js.map
