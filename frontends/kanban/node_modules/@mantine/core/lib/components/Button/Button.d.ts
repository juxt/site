import React from 'react';
import { DefaultProps, MantineSize, MantineNumberSize, MantineGradient, MantineColor, ClassNames, PolymorphicComponentProps } from '@mantine/styles';
import useStyles, { ButtonVariant } from './Button.styles';
import { LoaderProps } from '../Loader';
export declare type ButtonStylesNames = ClassNames<typeof useStyles>;
export interface SharedButtonProps extends DefaultProps<ButtonStylesNames> {
    /** Predefined button size */
    size?: MantineSize;
    /** Button type attribute */
    type?: 'submit' | 'button' | 'reset';
    /** Button color from theme */
    color?: MantineColor;
    /** Adds icon before button label  */
    leftIcon?: React.ReactNode;
    /** Adds icon after button label  */
    rightIcon?: React.ReactNode;
    /** Sets button width to 100% of parent element */
    fullWidth?: boolean;
    /** Button border-radius from theme or number to set border-radius in px */
    radius?: MantineNumberSize;
    /** Controls button appearance */
    variant?: ButtonVariant;
    /** Controls gradient settings in gradient variant only */
    gradient?: MantineGradient;
    /** Set text-transform to uppercase */
    uppercase?: boolean;
    /** Reduces vertical and horizontal spacing */
    compact?: boolean;
    /** Indicate loading state */
    loading?: boolean;
    /** Props spread to Loader component */
    loaderProps?: LoaderProps;
    /** Loader position relative to button label */
    loaderPosition?: 'left' | 'right';
}
export declare type ButtonProps<C> = PolymorphicComponentProps<C, SharedButtonProps>;
declare type ButtonComponent = (<C = 'button'>(props: ButtonProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const Button: ButtonComponent;
export {};
//# sourceMappingURL=Button.d.ts.map