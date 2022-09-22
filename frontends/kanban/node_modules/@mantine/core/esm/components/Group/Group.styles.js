import { createStyles } from '@mantine/styles';

const POSITIONS = {
  left: "flex-start",
  center: "center",
  right: "flex-end",
  apart: "space-between"
};
var useStyles = createStyles((theme, { spacing, position, noWrap, direction, grow, align, count }) => ({
  root: {
    boxSizing: "border-box",
    display: "flex",
    flexDirection: direction,
    alignItems: align || (direction === "row" ? "center" : grow ? "stretch" : position === "apart" ? "flex-start" : POSITIONS[position]),
    flexWrap: noWrap ? "nowrap" : "wrap",
    justifyContent: direction === "row" ? POSITIONS[position] : void 0,
    gap: theme.fn.size({ size: spacing, sizes: theme.spacing })
  },
  child: {
    boxSizing: "border-box",
    maxWidth: grow && direction === "row" ? `calc(${100 / count}% - ${theme.fn.size({ size: spacing, sizes: theme.spacing }) - theme.fn.size({ size: spacing, sizes: theme.spacing }) / count}px)` : void 0,
    flexGrow: grow ? 1 : 0
  }
}));

export default useStyles;
//# sourceMappingURL=Group.styles.js.map
