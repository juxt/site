import { MantineNumberSize, MantineColor } from '@mantine/styles';
interface BurgerStyles {
    size: MantineNumberSize;
    color: MantineColor;
}
export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
declare const _default: (params: BurgerStyles, options?: import("@mantine/styles").UseStylesOptions<"opened" | "root" | "burger">) => {
    classes: Record<"opened" | "root" | "burger", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Burger.styles.d.ts.map