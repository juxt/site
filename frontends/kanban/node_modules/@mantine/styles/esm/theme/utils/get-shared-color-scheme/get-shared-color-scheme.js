const DEFAULT_GRADIENT = {
  from: "indigo",
  to: "cyan",
  deg: 45
};
function getSharedColorScheme({ color, theme, variant, gradient }) {
  if (variant === "light") {
    return {
      border: "transparent",
      background: theme.fn.rgba(theme.fn.themeColor(color, theme.colorScheme === "dark" ? 8 : 0), theme.colorScheme === "dark" ? 0.35 : 1),
      color: color === "dark" ? theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.colors.dark[9] : theme.fn.themeColor(color, theme.colorScheme === "dark" ? 2 : 6),
      hover: theme.fn.rgba(theme.fn.themeColor(color, theme.colorScheme === "dark" ? 7 : 1), theme.colorScheme === "dark" ? 0.45 : 0.65)
    };
  }
  if (variant === "default") {
    return {
      border: theme.colorScheme === "dark" ? theme.colors.dark[5] : theme.colors.gray[4],
      background: theme.colorScheme === "dark" ? theme.colors.dark[5] : theme.white,
      color: theme.colorScheme === "dark" ? theme.white : theme.black,
      hover: theme.colorScheme === "dark" ? theme.colors.dark[4] : theme.colors.gray[0]
    };
  }
  if (variant === "white") {
    return {
      border: "transparent",
      background: theme.white,
      color: theme.fn.themeColor(color, 7),
      hover: null
    };
  }
  if (variant === "outline") {
    return {
      border: theme.fn.rgba(theme.fn.themeColor(color, theme.colorScheme === "dark" ? 4 : 7), 0.75),
      background: "transparent",
      color: theme.fn.themeColor(color, theme.colorScheme === "dark" ? 4 : 7),
      hover: theme.colorScheme === "dark" ? theme.fn.rgba(theme.fn.themeColor(color, 4), 0.05) : theme.fn.rgba(theme.fn.themeColor(color, 0), 0.35)
    };
  }
  if (variant === "gradient") {
    const merged = {
      from: (gradient == null ? void 0 : gradient.from) || DEFAULT_GRADIENT.from,
      to: (gradient == null ? void 0 : gradient.to) || DEFAULT_GRADIENT.to,
      deg: (gradient == null ? void 0 : gradient.deg) || DEFAULT_GRADIENT.deg
    };
    return {
      background: `linear-gradient(${merged.deg}deg, ${theme.fn.themeColor(merged.from, 6)} 0%, ${theme.fn.themeColor(merged.to, 6)} 100%)`,
      color: theme.white,
      border: "transparent",
      hover: null
    };
  }
  if (variant === "subtle") {
    return {
      border: "transparent",
      background: "transparent",
      color: color === "dark" ? theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.colors.dark[9] : theme.fn.themeColor(color, theme.colorScheme === "dark" ? 2 : 6),
      hover: theme.fn.rgba(theme.fn.themeColor(color, theme.colorScheme === "dark" ? 8 : 0), theme.colorScheme === "dark" ? 0.35 : 1)
    };
  }
  return {
    border: "transparent",
    background: theme.fn.themeColor(color, theme.colorScheme === "dark" ? 8 : 6),
    color: theme.white,
    hover: theme.fn.themeColor(color, 7)
  };
}

export { getSharedColorScheme };
//# sourceMappingURL=get-shared-color-scheme.js.map
