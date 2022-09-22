import { createStyles } from '@mantine/styles';
import { sizes } from '../SliderRoot/SliderRoot.styles.js';

var useStyles = createStyles((theme, { size, color }) => ({
  markWrapper: {
    position: "absolute",
    top: 0,
    zIndex: 2
  },
  mark: {
    boxSizing: "border-box",
    border: `${theme.fn.size({ size, sizes }) >= 8 ? "2px" : "1px"} solid ${theme.colorScheme === "dark" ? theme.colors.dark[4] : theme.colors.gray[2]}`,
    height: theme.fn.size({ sizes, size }),
    width: theme.fn.size({ sizes, size }),
    borderRadius: 1e3,
    transform: `translateX(-${theme.fn.size({ sizes, size }) / 2}px)`,
    backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[0] : theme.white
  },
  markFilled: {
    borderColor: theme.fn.themeColor(color, 6)
  },
  markLabel: {
    transform: "translate(-50%, 0)",
    fontSize: theme.fontSizes.sm,
    color: theme.colorScheme === "dark" ? theme.colors.dark[2] : theme.colors.gray[6],
    marginTop: theme.spacing.xs / 2
  }
}));

export default useStyles;
//# sourceMappingURL=Marks.styles.js.map
