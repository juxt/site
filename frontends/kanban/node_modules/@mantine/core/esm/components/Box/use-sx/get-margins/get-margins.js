function isValidMargin(margin) {
  return typeof margin === "string" || typeof margin === "number";
}
const MARGINS = {
  m: "margin",
  mt: "marginTop",
  mb: "marginBottom",
  ml: "marginLeft",
  mr: "marginRight"
};
function getMargins(margins, theme) {
  const styles = {};
  if (isValidMargin(margins.my)) {
    const margin = theme.fn.size({ size: margins.my, sizes: theme.spacing });
    styles.marginTop = margin;
    styles.marginBottom = margin;
  }
  if (isValidMargin(margins.mx)) {
    const margin = theme.fn.size({ size: margins.mx, sizes: theme.spacing });
    styles.marginLeft = margin;
    styles.marginRight = margin;
  }
  Object.keys(MARGINS).forEach((margin) => {
    if (isValidMargin(margins[margin])) {
      styles[MARGINS[margin]] = theme.fn.size({
        size: margins[margin],
        sizes: theme.spacing
      });
    }
  });
  return styles;
}

export { getMargins };
//# sourceMappingURL=get-margins.js.map
