import { createStyles } from '@mantine/styles';

var useStyles = createStyles((theme, { padding, first, last }) => ({
  cardSection: {
    display: "block",
    marginLeft: -1 * theme.fn.size({ size: padding, sizes: theme.spacing }),
    marginRight: -1 * theme.fn.size({ size: padding, sizes: theme.spacing }),
    marginTop: first ? -1 * theme.fn.size({ size: padding, sizes: theme.spacing }) : void 0,
    marginBottom: last ? -1 * theme.fn.size({ size: padding, sizes: theme.spacing }) : void 0
  }
}));

export default useStyles;
//# sourceMappingURL=CardSection.styles.js.map
