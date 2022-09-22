import { MantineNumberSize } from '@mantine/styles';
interface ContainerStyles {
    fluid: boolean;
    size: MantineNumberSize;
    padding: MantineNumberSize;
}
export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
declare const _default: (params: ContainerStyles, options?: import("@mantine/styles").UseStylesOptions<"root">) => {
    classes: Record<"root", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Container.styles.d.ts.map