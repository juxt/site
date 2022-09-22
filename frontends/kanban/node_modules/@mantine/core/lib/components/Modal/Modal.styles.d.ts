export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
    full: string;
};
interface ModalStyles {
    overflow: 'outside' | 'inside';
    size: string | number;
    centered: boolean;
}
declare const _default: (params: ModalStyles, options?: import("@mantine/styles").UseStylesOptions<"overlay" | "body" | "header" | "title" | "root" | "inner" | "modal" | "close">) => {
    classes: Record<"overlay" | "body" | "header" | "title" | "root" | "inner" | "modal" | "close", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Modal.styles.d.ts.map