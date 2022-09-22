import React from 'react';
import { DefaultProps, MantineNumberSize, MantineSize, ClassNames, PolymorphicComponentProps } from '@mantine/styles';
import useStyles, { InputVariant } from './Input.styles';
export declare type InputStylesNames = ClassNames<typeof useStyles>;
export interface InputBaseProps {
    /** Sets border color to red and aria-invalid=true on input element */
    invalid?: boolean;
    /** Adds icon on the left side of input */
    icon?: React.ReactNode;
    /** Width of icon section in px */
    iconWidth?: number;
    /** Right section of input, similar to icon but on the right */
    rightSection?: React.ReactNode;
    /** Width of right section, is used to calculate input padding-right */
    rightSectionWidth?: number;
    /** Props spread to rightSection div element */
    rightSectionProps?: React.ComponentPropsWithoutRef<'div'>;
    /** Properties spread to root element */
    wrapperProps?: {
        [key: string]: any;
    };
    /** Sets required on input element */
    required?: boolean;
    /** Input border-radius from theme or number to set border-radius in px */
    radius?: MantineNumberSize;
    /** Defines input appearance, defaults to default in light color scheme and filled in dark */
    variant?: InputVariant;
    /** Will input have multiple lines? */
    multiline?: boolean;
    /** Disabled input state */
    disabled?: boolean;
    /** Input size */
    size?: MantineSize;
}
interface _InputProps extends InputBaseProps, DefaultProps<InputStylesNames> {
    /** Static css selector base */
    __staticSelector?: string;
}
export declare type InputProps<C> = PolymorphicComponentProps<C, _InputProps>;
declare type InputComponent = (<C = 'input'>(props: InputProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const Input: InputComponent;
export {};
//# sourceMappingURL=Input.d.ts.map