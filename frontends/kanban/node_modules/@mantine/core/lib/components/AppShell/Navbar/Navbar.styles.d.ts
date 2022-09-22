import { MantineNumberSize } from '@mantine/styles';
export declare type NavbarWidth = Partial<Record<string, string | number>>;
export interface NavbarPosition {
    top?: number;
    left?: number;
    bottom?: number;
    right?: number;
}
interface NavbarStyles {
    width: Partial<Record<string, string | number>>;
    height: string | number;
    padding: MantineNumberSize;
    position: NavbarPosition;
    hiddenBreakpoint: MantineNumberSize;
    fixed: boolean;
    zIndex: number;
}
declare const _default: (params: NavbarStyles, options?: import("@mantine/styles").UseStylesOptions<"hidden" | "root">) => {
    classes: Record<"hidden" | "root", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Navbar.styles.d.ts.map