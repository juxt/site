import { createStyles } from '@mantine/styles';

var useStyles = createStyles((theme, { spacing, center }, getRef) => {
  const itemWrapper = {
    ref: getRef("itemWrapper"),
    display: "inline"
  };
  return {
    itemWrapper,
    item: {
      lineHeight: center ? 1 : theme.lineHeight,
      "&:not(:first-of-type)": {
        marginTop: theme.fn.size({ size: spacing, sizes: theme.spacing })
      }
    },
    withIcon: {
      listStyle: "none",
      [`& .${itemWrapper.ref}`]: {
        display: "inline-flex",
        alignItems: center ? "center" : "flex-start"
      }
    },
    itemIcon: {
      display: "inline-block",
      verticalAlign: "middle",
      marginRight: theme.spacing.sm
    }
  };
});

export default useStyles;
//# sourceMappingURL=ListItem.styles.js.map
