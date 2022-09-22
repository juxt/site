import { MantineSize, MantineColor, MantineNumberSize } from '@mantine/styles';
export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
interface CheckboxStyles {
    size: MantineSize;
    radius: MantineNumberSize;
    color: MantineColor;
    transitionDuration: number;
}
declare const _default: (params: CheckboxStyles, options?: import("@mantine/styles").UseStylesOptions<"icon" | "input" | "label" | "root" | "inner">) => {
    classes: Record<"icon" | "input" | "label" | "root" | "inner", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Checkbox.styles.d.ts.map