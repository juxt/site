import React from 'react';
import { DefaultProps, ClassNames } from '@mantine/styles';
import { InputWrapperBaseProps, InputWrapperStylesNames } from '../InputWrapper';
import { InputBaseProps, InputStylesNames } from '../Input';
import { SelectDropdownStylesNames } from '../Select/SelectDropdown/SelectDropdown';
import useStyles from './Autocomplete.styles';
import { SelectSharedProps } from '../Select/Select';
export declare type AutocompleteStylesNames = InputStylesNames | InputWrapperStylesNames | SelectDropdownStylesNames | ClassNames<typeof useStyles>;
export interface AutocompleteItem {
    value: string;
    [key: string]: any;
}
export interface AutocompleteProps extends DefaultProps<AutocompleteStylesNames>, InputBaseProps, InputWrapperBaseProps, SelectSharedProps<AutocompleteItem, string>, Omit<React.ComponentPropsWithoutRef<'input'>, 'size' | 'onChange' | 'value' | 'defaultValue'> {
    /** Called when item from dropdown was selected */
    onItemSubmit?(item: AutocompleteItem): void;
}
export declare function defaultFilter(value: string, item: AutocompleteItem): boolean;
export declare const Autocomplete: React.ForwardRefExoticComponent<AutocompleteProps & React.RefAttributes<HTMLInputElement>>;
//# sourceMappingURL=Autocomplete.d.ts.map