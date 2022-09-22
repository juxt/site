import React from 'react';
import { DefaultProps, MantineSize, ClassNames } from '@mantine/styles';
import useStyles from './InputWrapper.styles';
export declare type InputWrapperStylesNames = ClassNames<typeof useStyles>;
export interface InputWrapperBaseProps {
    /** Input label, displayed before input */
    label?: React.ReactNode;
    /** Input description, displayed after label */
    description?: React.ReactNode;
    /** Displays error message after input */
    error?: React.ReactNode;
    /** Adds red asterisk on the right side of label */
    required?: boolean;
    /** Props spread to label element */
    labelProps?: React.ComponentPropsWithoutRef<'label'> & {
        [key: string]: any;
    };
    /** Props spread to description element */
    descriptionProps?: React.ComponentPropsWithoutRef<'div'> & {
        [key: string]: any;
    };
    /** Props spread to error element */
    errorProps?: React.ComponentPropsWithoutRef<'div'> & {
        [key: string]: any;
    };
}
export interface InputWrapperProps extends DefaultProps<InputWrapperStylesNames>, InputWrapperBaseProps, React.ComponentPropsWithoutRef<'div'> {
    /** Input that should be wrapped */
    children: React.ReactNode;
    /** htmlFor label prop */
    id?: string;
    /** Render label as label with htmlFor or as div */
    labelElement?: 'label' | 'div';
    /** Controls all elements font-size */
    size?: MantineSize;
    /** Static css selector base */
    __staticSelector?: string;
}
export declare const InputWrapper: React.ForwardRefExoticComponent<InputWrapperProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=InputWrapper.d.ts.map