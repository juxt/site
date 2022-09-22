import React from 'react';
import { DefaultProps, MantineSize } from '@mantine/styles';
import { InputWrapperBaseProps, InputWrapperStylesNames } from '../InputWrapper/InputWrapper';
import { InputBaseProps, InputStylesNames } from '../Input/Input';
import { SelectItem } from '../Select/types';
export declare type NativeSelectStylesNames = InputStylesNames | InputWrapperStylesNames;
export interface NativeSelectProps extends DefaultProps<NativeSelectStylesNames>, InputWrapperBaseProps, InputBaseProps, Omit<React.ComponentPropsWithoutRef<'select'>, 'size'> {
    /** id is used to bind input and label, if not passed unique id will be generated for each input */
    id?: string;
    /** Adds hidden option to select and sets it as selected if value is not present */
    placeholder?: string;
    /** Data used to render options */
    data: (string | SelectItem)[];
    /** Style properties added to select element */
    inputStyle?: React.CSSProperties;
    /** Props passed to root element (InputWrapper component) */
    wrapperProps?: {
        [key: string]: any;
    };
    /** Input size */
    size?: MantineSize;
}
export declare const NativeSelect: React.ForwardRefExoticComponent<NativeSelectProps & React.RefAttributes<HTMLSelectElement>>;
//# sourceMappingURL=NativeSelect.d.ts.map