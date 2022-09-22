import { MantineSize, MantineNumberSize, MantineColor } from '@mantine/styles';
interface BadgeStyles {
    color: MantineColor;
    size: MantineSize;
    radius: MantineNumberSize;
    gradientFrom: string;
    gradientTo: string;
    gradientDeg: number;
    fullWidth: boolean;
}
export declare const heights: Record<MantineSize, number>;
declare const _default: (params: BadgeStyles, options?: import("@mantine/styles").UseStylesOptions<"outline" | "light" | "dot" | "filled" | "rightSection" | "gradient" | "root" | "inner" | "leftSection">) => {
    classes: Record<"outline" | "light" | "dot" | "filled" | "rightSection" | "gradient" | "root" | "inner" | "leftSection", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Badge.styles.d.ts.map