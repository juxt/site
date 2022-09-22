import { MantineNumberSize, MantineColor } from '@mantine/styles';
interface DividerStyles {
    size: MantineNumberSize;
    variant: 'solid' | 'dashed' | 'dotted';
    color: MantineColor;
}
export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
declare const _default: (params: DividerStyles, options?: import("@mantine/styles").UseStylesOptions<"left" | "right" | "horizontal" | "vertical" | "label" | "withLabel">) => {
    classes: Record<"left" | "right" | "horizontal" | "vertical" | "label" | "withLabel", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Divider.styles.d.ts.map