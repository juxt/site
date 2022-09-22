import { MantineNumberSize, MantineColor } from '@mantine/styles';
interface AvatarStyles {
    size: MantineNumberSize;
    radius: MantineNumberSize;
    color: MantineColor;
}
export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
declare const _default: (params: AvatarStyles, options?: import("@mantine/styles").UseStylesOptions<"image" | "placeholder" | "root" | "placeholderIcon">) => {
    classes: Record<"image" | "placeholder" | "root" | "placeholderIcon", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Avatar.styles.d.ts.map