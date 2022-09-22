import { MantineSize, MantineNumberSize, MantineSizes, MantineColor, MantineTheme } from '@mantine/styles';
export declare type ButtonVariant = 'filled' | 'outline' | 'light' | 'gradient' | 'white' | 'default' | 'subtle';
interface ButtonStylesProps {
    color: MantineColor;
    size: MantineSize;
    radius: MantineNumberSize;
    fullWidth: boolean;
    compact: boolean;
    gradientFrom: string;
    gradientTo: string;
    gradientDeg: number;
}
export declare const heights: MantineSizes;
declare const _default: (params: ButtonStylesProps, options?: import("@mantine/styles").UseStylesOptions<"outline" | "white" | "light" | "default" | "filled" | "icon" | "label" | "loading" | "gradient" | "subtle" | "root" | "leftIcon" | "rightIcon" | "inner">) => {
    classes: Record<"outline" | "white" | "light" | "default" | "filled" | "icon" | "label" | "loading" | "gradient" | "subtle" | "root" | "leftIcon" | "rightIcon" | "inner", string>;
    cx: (...args: any) => string;
    theme: MantineTheme;
};
export default _default;
//# sourceMappingURL=Button.styles.d.ts.map