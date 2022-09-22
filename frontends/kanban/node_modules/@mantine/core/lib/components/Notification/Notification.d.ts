import React from 'react';
import { DefaultProps, MantineColor, ClassNames, MantineNumberSize } from '@mantine/styles';
import useStyles from './Notification.styles';
export declare type NotificationStylesNames = Exclude<ClassNames<typeof useStyles>, 'withIcon'>;
export interface NotificationProps extends DefaultProps<NotificationStylesNames>, Omit<React.ComponentPropsWithoutRef<'div'>, 'title'> {
    /** Called when close button is clicked */
    onClose(): void;
    /** Notification line or icon color */
    color?: MantineColor;
    /** Radius from theme.radius, or number to set border-radius in px */
    radius?: MantineNumberSize;
    /** Notification icon, replaces color line */
    icon?: React.ReactNode;
    /** Notification title, displayed before body */
    title?: React.ReactNode;
    /** Notification body, place main text here */
    children?: React.ReactNode;
    /** Replaces colored line or icon with Loader component */
    loading?: boolean;
    /** Removes close button */
    disallowClose?: boolean;
    /** Props spread to close button */
    closeButtonProps?: React.ComponentPropsWithoutRef<'button'> & {
        [key: string]: any;
    };
}
export declare const Notification: React.ForwardRefExoticComponent<NotificationProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=Notification.d.ts.map