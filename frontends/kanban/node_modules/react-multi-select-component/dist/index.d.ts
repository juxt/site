import { ReactNode } from 'react';

interface Option {
    value: any;
    label: string;
    key?: string;
    disabled?: boolean;
}
interface SelectProps {
    options: Option[];
    value: Option[];
    onChange?: any;
    valueRenderer?: (selected: Option[], options: Option[]) => ReactNode;
    ItemRenderer?: any;
    ArrowRenderer?: ({ expanded }: {
        expanded: any;
    }) => JSX.Element;
    isLoading?: boolean;
    disabled?: boolean;
    disableSearch?: boolean;
    shouldToggleOnHover?: boolean;
    hasSelectAll?: boolean;
    filterOptions?: (options: Option[], filter: string) => Promise<Option[]> | Option[];
    overrideStrings?: {
        [key: string]: string;
    };
    labelledBy: string;
    className?: string;
    onMenuToggle?: any;
    ClearIcon?: ReactNode;
    debounceDuration?: number;
    ClearSelectedIcon?: ReactNode;
    defaultIsOpen?: boolean;
    isOpen?: boolean;
    isCreatable?: boolean;
    onCreateOption?: any;
    closeOnChangedValue?: boolean;
}

declare const MultiSelect: (props: SelectProps) => JSX.Element;

declare const Dropdown: () => JSX.Element;

declare const SelectPanel: () => JSX.Element;

interface ISelectItemProps {
    itemRenderer: any;
    option: Option;
    checked?: boolean;
    tabIndex?: number;
    disabled?: boolean;
    onSelectionChanged: (checked: boolean) => void;
    onClick: any;
}
declare const SelectItem: ({ itemRenderer: ItemRenderer, option, checked, tabIndex, disabled, onSelectionChanged, onClick, }: ISelectItemProps) => JSX.Element;

export { Dropdown, MultiSelect, Option, SelectItem, SelectPanel, SelectProps };
