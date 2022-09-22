import React from 'react';
import { DefaultProps, MantineShadow, ClassNames } from '@mantine/styles';
import { MantineTransition } from '../../Transition';
import useStyles from './SelectDropdown.styles';
export declare type SelectDropdownStylesNames = ClassNames<typeof useStyles>;
interface SelectDropdownProps extends DefaultProps<SelectDropdownStylesNames> {
    mounted: boolean;
    transition: MantineTransition;
    transitionDuration: number;
    transitionTimingFunction: string;
    uuid: string;
    shadow: MantineShadow;
    maxDropdownHeight?: number | string;
    withinPortal?: boolean;
    children: React.ReactNode;
    __staticSelector: string;
    dropdownComponent?: React.FC<any>;
    referenceElement?: HTMLElement;
    direction?: React.CSSProperties['flexDirection'];
    onDirectionChange?: (direction: React.CSSProperties['flexDirection']) => void;
    switchDirectionOnFlip?: boolean;
    zIndex?: number;
    dropdownPosition?: 'bottom' | 'top' | 'flip';
}
export declare const SelectDropdown: React.ForwardRefExoticComponent<SelectDropdownProps & React.RefAttributes<HTMLDivElement>>;
export {};
//# sourceMappingURL=SelectDropdown.d.ts.map