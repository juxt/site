import React from 'react';
import { DefaultProps, MantineNumberSize, MantineShadow, ClassNames, MantineMargin } from '@mantine/styles';
import { MantineTransition } from '../Transition';
import useStyles, { DrawerPosition } from './Drawer.styles';
export declare type DrawerStylesNames = Exclude<ClassNames<typeof useStyles>, 'noOverlay'>;
export interface DrawerProps extends Omit<DefaultProps<DrawerStylesNames>, MantineMargin>, Omit<React.ComponentPropsWithoutRef<'div'>, 'title'> {
    /** If true drawer is mounted to the dom */
    opened: boolean;
    /** Called when drawer is closed (Escape key and click outside, depending on options) */
    onClose(): void;
    /** Drawer body position */
    position?: DrawerPosition;
    /** Drawer body width (right | left position) or height (top | bottom position), cannot exceed 100vh for height and 100% for width */
    size?: string | number;
    /** Drawer body shadow from theme or any css shadow value */
    shadow?: MantineShadow;
    /** Drawer body padding from theme or number for padding in px */
    padding?: MantineNumberSize;
    /** Drawer z-index property */
    zIndex?: number;
    /** Disables focus trap */
    noFocusTrap?: boolean;
    /** Disables scroll lock */
    noScrollLock?: boolean;
    /** Disable onClock trigger for outside events */
    noCloseOnClickOutside?: boolean;
    /** Disable onClock trigger for escape key press */
    noCloseOnEscape?: boolean;
    /** Drawer appear and disappear transition, see Transition component for full documentation */
    transition?: MantineTransition;
    /** Transition duration in ms */
    transitionDuration?: number;
    /** Drawer transitionTimingFunction css property */
    transitionTimingFunction?: string;
    /** Removes overlay entirely */
    noOverlay?: boolean;
    /** Sets overlay opacity, defaults to 0.75 in light theme and to 0.85 in dark theme */
    overlayOpacity?: number;
    /** Sets overlay color, defaults to theme.black in light theme and to theme.colors.dark[9] in dark theme */
    overlayColor?: string;
    /** Drawer title, displayed in header before close button */
    title?: React.ReactNode;
    /** Hides close button, modal still can be closed with escape key and by clicking outside */
    hideCloseButton?: boolean;
    /** Close button aria-label */
    closeButtonLabel?: string;
    /** Target element or selector where drawer portal should be rendered */
    target?: HTMLElement | string;
}
export declare function MantineDrawer({ className, opened, onClose, position, size, noFocusTrap, noScrollLock, noCloseOnClickOutside, noCloseOnEscape, transition, transitionDuration, transitionTimingFunction, zIndex, overlayColor, overlayOpacity, children, noOverlay, shadow, padding, title, hideCloseButton, closeButtonLabel, classNames, styles, target, ...others }: DrawerProps): JSX.Element;
export declare function Drawer({ zIndex, target, ...props }: React.ComponentPropsWithoutRef<typeof MantineDrawer>): JSX.Element;
export declare namespace Drawer {
    var displayName: string;
}
//# sourceMappingURL=Drawer.d.ts.map