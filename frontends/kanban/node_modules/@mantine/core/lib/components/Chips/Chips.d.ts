import React from 'react';
import { DefaultProps, MantineNumberSize, MantineSize, MantineColor } from '@mantine/styles';
import { GroupProps } from '../Group/Group';
import { ChipStylesNames } from './Chip/Chip';
export interface ChipsProps<T extends boolean = false> extends DefaultProps<ChipStylesNames>, Omit<GroupProps, 'value' | 'defaultValue' | 'onChange' | 'classNames' | 'styles'> {
    /** Spacing between chips from theme or number to set value in px */
    spacing?: MantineNumberSize;
    /** Chip border-radius from theme or number to set value in px */
    radius?: MantineNumberSize;
    /** Predefined chip size */
    size?: MantineSize;
    /** Allow multiple values to be picked */
    multiple?: T;
    /** Controlled component value */
    value?: T extends true ? string[] : string;
    /** Uncontrolled component value */
    defaultValue?: T extends true ? string[] : string;
    /** Called when value changes */
    onChange?(value: T extends true ? string[] : string): void;
    /** Static id, used to generate inputs names */
    id?: string;
    /** <Chip /> components only */
    children?: React.ReactNode;
    /** Controls chip appearance, defaults to filled with dark theme and to outline in light theme */
    variant?: 'filled' | 'outline';
    /** Active chip color, defaults to theme.primaryColor */
    color?: MantineColor;
    /** Inputs name attribute */
    name?: string;
}
export declare function Chips<T extends boolean>({ value, defaultValue, onChange, color, spacing, radius, size, name, variant, multiple, children, id, classNames, styles, ...others }: ChipsProps<T>): JSX.Element;
export declare namespace Chips {
    var displayName: string;
}
//# sourceMappingURL=Chips.d.ts.map