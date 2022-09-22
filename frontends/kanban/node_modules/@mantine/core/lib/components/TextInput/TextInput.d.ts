import React from 'react';
import { DefaultProps, MantineSize } from '@mantine/styles';
import { InputBaseProps, InputStylesNames } from '../Input/Input';
import { InputWrapperBaseProps, InputWrapperStylesNames } from '../InputWrapper/InputWrapper';
export declare type TextInputStylesNames = InputStylesNames | InputWrapperStylesNames;
export interface TextInputProps extends DefaultProps<TextInputStylesNames>, InputBaseProps, InputWrapperBaseProps, Omit<React.ComponentPropsWithoutRef<'input'>, 'size'> {
    /** id is used to bind input and label, if not passed unique id will be generated for each input */
    id?: string;
    /** Adds icon on the left side of input */
    icon?: React.ReactNode;
    /** Input element type */
    type?: 'text' | 'password' | 'email' | 'search' | 'tel' | 'url' | 'number';
    /** Props passed to root element (InputWrapper component) */
    wrapperProps?: {
        [key: string]: any;
    };
    /** Input size */
    size?: MantineSize;
    /** Static css selector base */
    __staticSelector?: string;
}
export declare const TextInput: React.ForwardRefExoticComponent<TextInputProps & React.RefAttributes<HTMLInputElement>>;
//# sourceMappingURL=TextInput.d.ts.map