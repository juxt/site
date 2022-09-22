import { MantineNumberSize, MantineSize, MantineColor } from '@mantine/styles';
export declare const WRAPPER_PADDING = 4;
interface SegmentedControlStyles {
    fullWidth: boolean;
    color: MantineColor;
    radius: MantineNumberSize;
    shouldAnimate: boolean;
    transitionDuration: number;
    transitionTimingFunction: string;
    size: MantineSize;
    orientation: 'vertical' | 'horizontal';
}
declare const _default: (params: SegmentedControlStyles, options?: import("@mantine/styles").UseStylesOptions<"active" | "disabled" | "input" | "label" | "root" | "control" | "controlActive" | "labelActive">) => {
    classes: Record<"active" | "disabled" | "input" | "label" | "root" | "control" | "controlActive" | "labelActive", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=SegmentedControl.styles.d.ts.map