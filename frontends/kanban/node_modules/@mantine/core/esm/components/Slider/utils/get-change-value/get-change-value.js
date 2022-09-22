function getChangeValue({ value, containerWidth, min, max, step }) {
  const left = !containerWidth ? value : Math.min(Math.max(value, 0), containerWidth) / containerWidth;
  const dx = left * (max - min);
  return (dx !== 0 ? Math.round(dx / step) * step : 0) + min;
}

export { getChangeValue };
//# sourceMappingURL=get-change-value.js.map
