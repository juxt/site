import React from 'react';
import { DefaultProps, MantineNumberSize, MantineSize, MantineColor, ClassNames } from '@mantine/styles';
import useStyles from './Chip.styles';
export declare type ChipStylesNames = ClassNames<typeof useStyles>;
export interface ChipProps extends DefaultProps<ChipStylesNames>, Omit<React.ComponentPropsWithoutRef<'input'>, 'size' | 'onChange'> {
    /** Chip radius from theme or number to set value in px */
    radius?: MantineNumberSize;
    /** Predefined chip size */
    size?: MantineSize;
    /** Chip input type */
    type?: 'radio' | 'checkbox';
    /** Controls chip appearance, defaults to filled with dark theme and to outline in light theme */
    variant?: 'outline' | 'filled';
    /** Chip label */
    children: React.ReactNode;
    /** Checked state for controlled component */
    checked?: boolean;
    /** Default value for uncontrolled component */
    defaultChecked?: boolean;
    /** Calls when checked state changes */
    onChange?(checked: boolean): void;
    /** Active color from theme, defaults to theme.primaryColor */
    color?: MantineColor;
    /** Static id to bind input with label */
    id?: string;
    /** Static selector base */
    __staticSelector?: string;
    /** Props spread to wrapper element */
    wrapperProps?: {
        [key: string]: any;
    };
}
export declare const Chip: React.ForwardRefExoticComponent<ChipProps & React.RefAttributes<HTMLInputElement>>;
//# sourceMappingURL=Chip.d.ts.map