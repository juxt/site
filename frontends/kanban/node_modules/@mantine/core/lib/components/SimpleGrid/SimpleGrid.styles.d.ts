import { MantineNumberSize } from '@mantine/styles';
export interface SimpleGridBreakpoint {
    maxWidth?: MantineNumberSize;
    minWidth?: MantineNumberSize;
    cols: number;
    spacing?: MantineNumberSize;
}
interface SimpleGridStyles {
    spacing: MantineNumberSize;
    breakpoints: SimpleGridBreakpoint[];
    cols: number;
}
declare const _default: (params: SimpleGridStyles, options?: import("@mantine/styles").UseStylesOptions<"root">) => {
    classes: Record<"root", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=SimpleGrid.styles.d.ts.map