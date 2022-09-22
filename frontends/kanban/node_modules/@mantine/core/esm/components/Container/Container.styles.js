import { createStyles } from '@mantine/styles';

const sizes = {
  xs: 540,
  sm: 720,
  md: 960,
  lg: 1140,
  xl: 1320
};
var useStyles = createStyles((theme, { fluid, size, padding }) => ({
  root: {
    maxWidth: fluid ? "100%" : theme.fn.size({ size, sizes }),
    marginLeft: "auto",
    marginRight: "auto",
    paddingLeft: theme.fn.size({ size: padding, sizes: theme.spacing }),
    paddingRight: theme.fn.size({ size: padding, sizes: theme.spacing })
  }
}));

export default useStyles;
export { sizes };
//# sourceMappingURL=Container.styles.js.map
