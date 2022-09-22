import React from 'react';
import { DefaultProps, MantineNumberSize, MantineSize, MantineColor } from '@mantine/styles';
import { InputWrapperBaseProps, InputWrapperStylesNames } from '../InputWrapper/InputWrapper';
import { RadioStylesNames } from './Radio/Radio';
export declare type RadioGroupStylesNames = InputWrapperStylesNames | RadioStylesNames;
export interface RadioGroupProps extends DefaultProps<RadioGroupStylesNames>, InputWrapperBaseProps, Omit<React.ComponentPropsWithoutRef<'div'>, 'onChange'> {
    /** <Radio /> components only */
    children: React.ReactNode;
    /** Input name attribute, used to bind radios in one group, by default generated randomly with use-id hook */
    name?: string;
    /** Value of currently selected radio */
    value?: string;
    /** Called when value changes */
    onChange?(value: string): void;
    /** Initial value for uncontrolled component */
    defaultValue?: string;
    /** Radios position */
    variant?: 'horizontal' | 'vertical';
    /** Spacing between radios in horizontal variant */
    spacing?: MantineNumberSize;
    /** Active radio color from theme.colors */
    color?: MantineColor;
    /** Predefined label fontSize, radio width, height and border-radius */
    size?: MantineSize;
    /** Props spread to InputWrapper */
    wrapperProps?: {
        [key: string]: any;
    };
}
export declare const RadioGroup: React.ForwardRefExoticComponent<RadioGroupProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=RadioGroup.d.ts.map