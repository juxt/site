declare type HeadingElement = 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
interface TitleStyles {
    element: HeadingElement;
    align: 'right' | 'left' | 'center' | 'justify';
}
declare const _default: (params: TitleStyles, options?: import("@mantine/styles").UseStylesOptions<"root">) => {
    classes: Record<"root", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Title.styles.d.ts.map