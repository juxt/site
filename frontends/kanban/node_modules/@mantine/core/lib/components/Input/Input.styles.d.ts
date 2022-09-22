import { MantineNumberSize, MantineSize } from '@mantine/styles';
export declare type InputVariant = 'default' | 'filled' | 'unstyled' | 'headless';
interface InputStyles {
    radius: MantineNumberSize;
    size: MantineSize;
    variant: InputVariant;
    multiline: boolean;
    invalid: boolean;
    rightSectionWidth: number;
    withRightSection: boolean;
    iconWidth: number;
}
export declare const sizes: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
declare const _default: (params: InputStyles, options?: import("@mantine/styles").UseStylesOptions<"icon" | "disabled" | "input" | "wrapper" | "defaultVariant" | "filledVariant" | "unstyledVariant" | "withIcon" | "invalid" | "rightSection">) => {
    classes: Record<"icon" | "disabled" | "input" | "wrapper" | "defaultVariant" | "filledVariant" | "unstyledVariant" | "withIcon" | "invalid" | "rightSection", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Input.styles.d.ts.map