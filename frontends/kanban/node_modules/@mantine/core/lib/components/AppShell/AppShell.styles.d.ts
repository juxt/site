import { MantineNumberSize, MantineTheme } from '@mantine/styles';
interface AppShellStyles {
    padding: MantineNumberSize;
    fixed: boolean;
    headerHeight: string;
    navbarBreakpoints: [number, {
        width: string | number;
    }][];
    navbarWidth: string;
    navbarOffsetBreakpoint: MantineNumberSize;
}
declare const _default: (params: AppShellStyles, options?: import("@mantine/styles").UseStylesOptions<"body" | "main" | "root">) => {
    classes: Record<"body" | "main" | "root", string>;
    cx: (...args: any) => string;
    theme: MantineTheme;
};
export default _default;
//# sourceMappingURL=AppShell.styles.d.ts.map