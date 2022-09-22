import React from 'react';
import { DefaultProps, ClassNames } from '@mantine/styles';
import { DefaultValueStylesNames } from './DefaultValue/DefaultValue';
import { SelectItem, BaseSelectProps, BaseSelectStylesNames } from '../Select/types';
import useStyles from './MultiSelect.styles';
import { SelectSharedProps } from '../Select/Select';
export declare type MultiSelectStylesNames = DefaultValueStylesNames | Exclude<ClassNames<typeof useStyles>, 'searchInputEmpty' | 'searchInputInputHidden' | 'searchInputPointer'> | Exclude<BaseSelectStylesNames, 'selected'>;
export interface MultiSelectProps extends DefaultProps<MultiSelectStylesNames>, BaseSelectProps, Omit<SelectSharedProps<SelectItem, string[]>, 'filter'> {
    /** Component used to render values */
    valueComponent?: React.FC<any>;
    /** Maximum dropdown height in px */
    maxDropdownHeight?: number;
    /** Enable items searching */
    searchable?: boolean;
    /** Function based on which items in dropdown are filtered */
    filter?(value: string, selected: boolean, item: SelectItem): boolean;
    /** Clear search value when item is selected */
    clearSearchOnChange?: boolean;
    /** Allow to clear item */
    clearable?: boolean;
    /** aria-label for clear button */
    clearButtonLabel?: string;
    /** Clear search field value on blur */
    clearSearchOnBlur?: boolean;
    /** Called each time search query changes */
    onSearchChange?(query: string): void;
    /** Allow creatable option  */
    creatable?: boolean;
    /** Function to get create Label */
    getCreateLabel?: (query: string) => React.ReactNode;
    /** Function to determine if create label should be displayed */
    shouldCreate?: (query: string, data: SelectItem[]) => boolean;
    /** Called when create option is selected */
    onCreate?: (query: string) => void;
    /** Change dropdown component, can be used to add custom scrollbars */
    dropdownComponent?: any;
    /** Limit amount of items selected */
    maxSelectedValues?: number;
    /** Select highlighted item on blur */
    selectOnBlur?: boolean;
}
export declare function defaultFilter(value: string, selected: boolean, item: SelectItem): boolean;
export declare function defaultShouldCreate(query: string, data: SelectItem[]): boolean;
export declare const MultiSelect: React.ForwardRefExoticComponent<MultiSelectProps & React.RefAttributes<HTMLInputElement>>;
//# sourceMappingURL=MultiSelect.d.ts.map