import { MantineColor, MantineNumberSize } from '@mantine/styles';
interface AlertStyles {
    color: MantineColor;
    radius: MantineNumberSize;
    variant: 'filled' | 'outline' | 'light';
}
declare const _default: (params: AlertStyles, options?: import("@mantine/styles").UseStylesOptions<"outline" | "light" | "filled" | "icon" | "body" | "label" | "title" | "wrapper" | "root" | "message" | "closeButton">) => {
    classes: Record<"outline" | "light" | "filled" | "icon" | "body" | "label" | "title" | "wrapper" | "root" | "message" | "closeButton", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Alert.styles.d.ts.map