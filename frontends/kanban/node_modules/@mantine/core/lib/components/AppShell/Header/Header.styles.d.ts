import { MantineNumberSize } from '@mantine/styles';
export interface HeaderPosition {
    top?: number;
    left?: number;
    bottom?: number;
    right?: number;
}
interface HeaderStyles {
    height: number | string;
    padding: MantineNumberSize;
    fixed: boolean;
    position: HeaderPosition;
    zIndex: number;
}
declare const _default: (params: HeaderStyles, options?: import("@mantine/styles").UseStylesOptions<"root">) => {
    classes: Record<"root", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Header.styles.d.ts.map