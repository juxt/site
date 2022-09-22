import { createStyles } from '@mantine/styles';

var useStyles = createStyles((_theme, { transitionDuration }) => ({
  control: {},
  root: {
    position: "relative"
  },
  content: {
    overflow: "hidden",
    transitionProperty: "max-height",
    transitionTimingFunction: "ease",
    transitionDuration: `${transitionDuration}ms`,
    "@media (prefers-reduced-motion)": {
      transitionDuration: "0ms"
    }
  }
}));

export default useStyles;
//# sourceMappingURL=Spoiler.styles.js.map
