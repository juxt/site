import { MantineNumberSize, MantineColor } from '@mantine/styles';
export declare type ThemeIconVariant = 'filled' | 'light' | 'gradient';
interface ThemeIconStyles {
    color: MantineColor;
    size: MantineNumberSize;
    radius: MantineNumberSize;
    variant: ThemeIconVariant;
    gradientFrom: string;
    gradientTo: string;
    gradientDeg: number;
}
export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
declare const _default: (params: ThemeIconStyles, options?: import("@mantine/styles").UseStylesOptions<"root">) => {
    classes: Record<"root", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=ThemeIcon.styles.d.ts.map