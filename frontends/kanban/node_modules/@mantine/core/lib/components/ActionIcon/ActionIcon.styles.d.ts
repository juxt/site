import { MantineNumberSize, MantineColor, MantineTheme } from '@mantine/styles';
export declare type ActionIconVariant = 'hover' | 'filled' | 'outline' | 'light' | 'default' | 'transparent';
interface ActionIconStyles {
    color: MantineColor;
    size: MantineNumberSize;
    radius: MantineNumberSize;
}
export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
declare const _default: (params: ActionIconStyles, options?: import("@mantine/styles").UseStylesOptions<"outline" | "transparent" | "light" | "default" | "filled" | "loading" | "root" | "hover">) => {
    classes: Record<"outline" | "transparent" | "light" | "default" | "filled" | "loading" | "root" | "hover", string>;
    cx: (...args: any) => string;
    theme: MantineTheme;
};
export default _default;
//# sourceMappingURL=ActionIcon.styles.d.ts.map