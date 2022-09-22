import { MantineTheme } from '@mantine/styles';
export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
    full: string;
};
export declare type DrawerPosition = 'top' | 'bottom' | 'left' | 'right';
interface DrawerStyles {
    position: DrawerPosition;
    size: number | string;
}
declare const _default: (params: DrawerStyles, options?: import("@mantine/styles").UseStylesOptions<"overlay" | "header" | "title" | "root" | "closeButton" | "noOverlay" | "drawer">) => {
    classes: Record<"overlay" | "header" | "title" | "root" | "closeButton" | "noOverlay" | "drawer", string>;
    cx: (...args: any) => string;
    theme: MantineTheme;
};
export default _default;
//# sourceMappingURL=Drawer.styles.d.ts.map