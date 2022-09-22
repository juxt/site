import { MantineNumberSize, MantineSize } from '@mantine/styles';
interface NumberInputStyles {
    radius: MantineNumberSize;
    size: MantineSize;
}
export declare const CONTROL_SIZES: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
declare const _default: (params: NumberInputStyles, options?: import("@mantine/styles").UseStylesOptions<"rightSection" | "control" | "controlUp" | "controlDown">) => {
    classes: Record<"rightSection" | "control" | "controlUp" | "controlDown", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=NumberInput.styles.d.ts.map