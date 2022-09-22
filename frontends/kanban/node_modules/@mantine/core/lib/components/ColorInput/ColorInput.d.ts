import React from 'react';
import { DefaultProps, ClassNames } from '@mantine/styles';
import { InputWrapperBaseProps, InputWrapperStylesNames } from '../InputWrapper';
import { InputBaseProps, InputStylesNames } from '../Input';
import { MantineTransition } from '../Transition';
import { ColorPickerBaseProps, ColorPickerStylesNames } from '../ColorPicker/ColorPicker';
import useStyles from './ColorInput.styles';
export declare type ColorInputStylesNames = InputWrapperStylesNames | InputStylesNames | ColorPickerStylesNames | ClassNames<typeof useStyles>;
export interface ColorInputProps extends InputWrapperBaseProps, InputBaseProps, ColorPickerBaseProps, DefaultProps<ColorInputStylesNames>, Omit<React.ComponentPropsWithoutRef<'input'>, 'size' | 'onChange' | 'defaultValue' | 'value'> {
    /** Disallow free input */
    disallowInput?: boolean;
    /** call onChange with last valid value onBlur */
    fixOnBlur?: boolean;
    /** Dropdown element z-index */
    dropdownZIndex?: number;
    /** Display swatch with color preview on the left side of input */
    withPreview?: boolean;
    /** Dropdown transition name or object */
    transition?: MantineTransition;
    /** Dropdown appear/disappear transition duration in ms */
    transitionDuration?: number;
    /** Dropdown transition timing function, defaults to theme.transitionTimingFunction */
    transitionTimingFunction?: string;
    /** Whether to render the dropdown in a Portal */
    withinPortal?: boolean;
}
export declare const ColorInput: React.ForwardRefExoticComponent<ColorInputProps & React.RefAttributes<HTMLInputElement>>;
//# sourceMappingURL=ColorInput.d.ts.map