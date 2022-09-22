'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');

var useStyles = styles.createStyles((theme, { spacing, center }, getRef) => {
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

exports.default = useStyles;
//# sourceMappingURL=ListItem.styles.js.map
