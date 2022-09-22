'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var styles = require('@mantine/styles');

var useStyles = styles.createStyles((theme, { tabPadding, orientation }, getRef) => {
  const tabsList = { ref: getRef("tabsList") };
  return {
    tabsListWrapper: {},
    tabsList,
    root: {
      display: orientation === "vertical" ? "flex" : "block"
    },
    pills: {
      marginRight: orientation === "vertical" ? 20 : 0
    },
    body: {
      [orientation === "horizontal" ? "paddingTop" : "paddingLeft"]: theme.fn.size({
        size: tabPadding,
        sizes: theme.spacing
      })
    },
    default: {
      [orientation === "horizontal" ? "borderBottom" : "borderRight"]: `2px solid ${theme.colorScheme === "dark" ? theme.colors.dark[4] : theme.colors.gray[2]}`,
      [`& .${tabsList.ref}`]: {
        [orientation === "horizontal" ? "marginBottom" : "marginRight"]: -2
      }
    },
    outline: {
      [orientation === "horizontal" ? "borderBottom" : "borderRight"]: `1px solid ${theme.colorScheme === "dark" ? theme.colors.dark[4] : theme.colors.gray[2]}`,
      [`& .${tabsList.ref}`]: {
        [orientation === "horizontal" ? "marginBottom" : "marginRight"]: -1
      }
    }
  };
});

exports.default = useStyles;
//# sourceMappingURL=Tabs.styles.js.map
