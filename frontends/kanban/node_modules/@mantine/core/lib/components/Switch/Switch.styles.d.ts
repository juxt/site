import { MantineNumberSize, MantineSize, MantineColor } from '@mantine/styles';
interface SwitchStyles {
    color: MantineColor;
    size: MantineSize;
    radius: MantineNumberSize;
    offLabel: string;
    onLabel: string;
}
export declare const sizes: Record<MantineSize, {
    width: number;
    height: number;
}>;
declare const _default: (params: SwitchStyles, options?: import("@mantine/styles").UseStylesOptions<"input" | "label" | "root">) => {
    classes: Record<"input" | "label" | "root", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Switch.styles.d.ts.map