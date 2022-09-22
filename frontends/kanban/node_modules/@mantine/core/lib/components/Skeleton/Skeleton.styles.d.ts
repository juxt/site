import { MantineNumberSize } from '@mantine/styles';
interface SkeletonStylesProps {
    height: number | string;
    width: number | string;
    circle: boolean;
    radius: MantineNumberSize;
    animate: boolean;
}
export declare const fade: import("@emotion/serialize").Keyframes;
declare const _default: (params: SkeletonStylesProps, options?: import("@mantine/styles").UseStylesOptions<"visible" | "root">) => {
    classes: Record<"visible" | "root", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Skeleton.styles.d.ts.map