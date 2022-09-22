import React from 'react';
import { DefaultProps, MantineColor, ClassNames, MantineNumberSize } from '@mantine/styles';
import useStyles from './Alert.styles';
export declare type AlertVariant = 'filled' | 'outline' | 'light';
export declare type AlertStylesNames = ClassNames<typeof useStyles>;
export interface AlertProps extends DefaultProps<AlertStylesNames>, Omit<React.ComponentPropsWithoutRef<'div'>, 'title'> {
    /** Alert title */
    title?: React.ReactNode;
    /** Controls Alert background, color and border styles */
    variant?: AlertVariant;
    /** Alert message */
    children: React.ReactNode;
    /** Color from theme.colors */
    color?: MantineColor;
    /** Icon displayed next to title */
    icon?: React.ReactNode;
    /** True to display close button */
    withCloseButton?: boolean;
    /** Called when close button is clicked */
    onClose?(): void;
    /** Close button aria-label */
    closeButtonLabel?: string;
    /** Radius from theme.radius, or number to set border-radius in px */
    radius?: MantineNumberSize;
}
export declare const Alert: React.ForwardRefExoticComponent<AlertProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=Alert.d.ts.map