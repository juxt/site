import { MantineNumberSize, MantineColor } from '@mantine/styles';
export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
interface ProgressStyles {
    color: MantineColor;
    radius: MantineNumberSize;
    size: MantineNumberSize;
    striped: boolean;
    animate: boolean;
}
declare const _default: (params: ProgressStyles, options?: import("@mantine/styles").UseStylesOptions<"label" | "root" | "bar">) => {
    classes: Record<"label" | "root" | "bar", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Progress.styles.d.ts.map