import React from 'react';
import { DefaultProps, MantineSize, MantineColor, ClassNames, MantineNumberSize } from '@mantine/styles';
import useStyles from './Checkbox.styles';
export declare type CheckboxStylesNames = ClassNames<typeof useStyles>;
export interface CheckboxProps extends DefaultProps<CheckboxStylesNames>, Omit<React.ComponentPropsWithoutRef<'input'>, 'type' | 'size'> {
    /** Checkbox checked and indeterminate state color from theme, defaults to theme.primaryColor */
    color?: MantineColor;
    /** Radius from theme.radius, or number to set border-radius in px */
    radius?: MantineNumberSize;
    /** Predefined label font-size and checkbox width and height in px */
    size?: MantineSize;
    /** Checkbox label */
    label?: React.ReactNode;
    /** Indeterminate state of checkbox, overwrites checked */
    indeterminate?: boolean;
    /** Props spread to wrapper element */
    wrapperProps?: {
        [key: string]: any;
    };
    /** Id is used to bind input and label, if not passed unique id will be generated for each input */
    id?: string;
    /** Check/uncheck transition duration, set to 0 to disable all transitions */
    transitionDuration?: number;
    /** Replace default icon */
    icon?: React.FC<{
        indeterminate: boolean;
        className: string;
    }>;
}
export declare const Checkbox: React.ForwardRefExoticComponent<CheckboxProps & React.RefAttributes<HTMLInputElement>>;
//# sourceMappingURL=Checkbox.d.ts.map