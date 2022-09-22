import React from 'react';
import { DefaultProps, MantineSize, MantineNumberSize, MantineGradient, MantineColor, ClassNames, PolymorphicComponentProps } from '@mantine/styles';
import useStyles from './Badge.styles';
export declare type BadgeVariant = 'light' | 'filled' | 'outline' | 'dot' | 'gradient';
export declare type BadgeStylesNames = ClassNames<typeof useStyles>;
interface _BadgeProps extends DefaultProps<BadgeStylesNames> {
    /** Badge color from theme */
    color?: MantineColor;
    /** Controls badge background, color and border styles */
    variant?: BadgeVariant;
    /** Controls gradient settings in gradient variant only */
    gradient?: MantineGradient;
    /** Defines badge height and font-size */
    size?: MantineSize;
    /** Predefined border-radius value from theme.radius or number for border-radius in px */
    radius?: MantineNumberSize;
    /** Sets badge width to 100% of parent element, hides overflow text with text-overflow: ellipsis */
    fullWidth?: boolean;
    /** Section rendered on the left side of label */
    leftSection?: React.ReactNode;
    /** Section rendered on the right side of label */
    rightSection?: React.ReactNode;
}
export declare type BadgeProps<C> = PolymorphicComponentProps<C, _BadgeProps>;
declare type BadgeComponent = (<C = 'div'>(props: BadgeProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const Badge: BadgeComponent;
export {};
//# sourceMappingURL=Badge.d.ts.map