import { MantineNumberSize, MantineTheme } from '@mantine/styles';
interface ColStyles {
    gutter: MantineNumberSize;
    columns: number;
    grow: boolean;
    offset: number;
    offsetXs: number;
    offsetSm: number;
    offsetMd: number;
    offsetLg: number;
    offsetXl: number;
    span: number;
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
}
declare const _default: (params: ColStyles, options?: import("@mantine/styles").UseStylesOptions<"root">) => {
    classes: Record<"root", string>;
    cx: (...args: any) => string;
    theme: MantineTheme;
};
export default _default;
//# sourceMappingURL=Col.styles.d.ts.map