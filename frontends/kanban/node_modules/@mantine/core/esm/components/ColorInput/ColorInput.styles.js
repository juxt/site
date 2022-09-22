import { createStyles } from '@mantine/styles';

var useStyles = createStyles((theme, { disallowInput }) => ({
  dropdownBody: {
    backgroundColor: theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.white,
    border: `1px solid ${theme.colorScheme === "dark" ? theme.colors.dark[6] : theme.colors.gray[2]}`
  },
  input: {
    cursor: disallowInput ? "pointer" : void 0
  }
}));

export default useStyles;
//# sourceMappingURL=ColorInput.styles.js.map
