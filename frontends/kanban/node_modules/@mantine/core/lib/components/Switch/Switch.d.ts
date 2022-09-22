import React from 'react';
import { DefaultProps, MantineNumberSize, MantineSize, MantineColor, ClassNames } from '@mantine/styles';
import useStyles from './Switch.styles';
export declare type SwitchStylesNames = ClassNames<typeof useStyles>;
export interface SwitchProps extends DefaultProps<SwitchStylesNames>, Omit<React.ComponentPropsWithoutRef<'input'>, 'type' | 'size'> {
    /** Id is used to bind input and label, if not passed unique id will be generated for each input */
    id?: string;
    /** Switch label */
    label?: React.ReactNode;
    /** Inner label when Switch is in unchecked state */
    offLabel?: string;
    /** Inner label when Switch is in checked state */
    onLabel?: string;
    /** Switch checked state color from theme.colors, defaults to theme.primaryColor */
    color?: MantineColor;
    /** Predefined size value */
    size?: MantineSize;
    /** Radius from theme.radius or number to set border-radius in px */
    radius?: MantineNumberSize;
    /** Props spread to wrapper element */
    wrapperProps?: {
        [key: string]: any;
    };
}
export declare const Switch: React.ForwardRefExoticComponent<SwitchProps & React.RefAttributes<HTMLInputElement>>;
//# sourceMappingURL=Switch.d.ts.map