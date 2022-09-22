import { MantineNumberSize, MantineSize, MantineColor } from '@mantine/styles';
export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
interface ChipStyles {
    radius: MantineNumberSize;
    size: MantineSize;
    color: MantineColor;
}
declare const _default: (params: ChipStyles, options?: import("@mantine/styles").UseStylesOptions<"outline" | "filled" | "disabled" | "input" | "label" | "checked" | "root" | "iconWrapper" | "checkIcon">) => {
    classes: Record<"outline" | "filled" | "disabled" | "input" | "label" | "checked" | "root" | "iconWrapper" | "checkIcon", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Chip.styles.d.ts.map