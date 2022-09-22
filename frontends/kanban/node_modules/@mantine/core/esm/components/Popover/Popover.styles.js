import { createStyles } from '@mantine/styles';

var useStyles = createStyles((theme) => ({
  root: {
    position: "relative",
    display: "inline-block"
  },
  arrow: {
    borderColor: theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.colors.gray[2],
    background: theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.white
  },
  target: {
    zIndex: 1
  }
}));

export default useStyles;
//# sourceMappingURL=Popover.styles.js.map
