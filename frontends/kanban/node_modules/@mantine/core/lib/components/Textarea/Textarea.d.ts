import React from 'react';
import { DefaultProps, MantineSize } from '@mantine/styles';
import { InputWrapperBaseProps } from '../InputWrapper/InputWrapper';
import { TextInputStylesNames } from '../TextInput/TextInput';
import { InputBaseProps } from '../Input/Input';
export interface TextareaProps extends DefaultProps<TextInputStylesNames>, InputWrapperBaseProps, InputBaseProps, React.ComponentPropsWithoutRef<'textarea'> {
    /** Id is used to bind input and label, if not passed unique id will be generated for each input */
    id?: string;
    /** If true textarea will grow with content until maxRows are reached  */
    autosize?: boolean;
    /** Defines maxRows in autosize variant, not applicable to regular variant */
    maxRows?: number;
    /** Defined minRows in autosize variant and rows in regular variant */
    minRows?: number;
    /** Props passed to root element (InputWrapper component) */
    wrapperProps?: {
        [key: string]: any;
    };
    /** Input size */
    size?: MantineSize;
    /** Static selectors base */
    __staticSelector?: string;
}
export declare const Textarea: React.ForwardRefExoticComponent<TextareaProps & React.RefAttributes<HTMLTextAreaElement>>;
//# sourceMappingURL=Textarea.d.ts.map