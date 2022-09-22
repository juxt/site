import { MantineColor, MantineNumberSize, MantineSize } from '@mantine/styles';
interface StepStyles {
    color: MantineColor;
    iconSize: number;
    size: MantineSize;
    radius: MantineNumberSize;
    allowStepClick: boolean;
    iconPosition: 'right' | 'left';
}
export declare const iconSizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
declare const _default: (params: StepStyles, options?: import("@mantine/styles").UseStylesOptions<"step" | "stepLoader" | "stepIcon" | "stepCompletedIcon" | "stepProgress" | "stepCompleted" | "stepBody" | "stepLabel" | "stepDescription">) => {
    classes: Record<"step" | "stepLoader" | "stepIcon" | "stepCompletedIcon" | "stepProgress" | "stepCompleted" | "stepBody" | "stepLabel" | "stepDescription", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Step.styles.d.ts.map