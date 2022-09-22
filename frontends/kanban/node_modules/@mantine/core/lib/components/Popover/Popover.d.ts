import React from 'react';
import { DefaultProps, MantineNumberSize, MantineShadow, ClassNames } from '@mantine/styles';
import { SharedPopperProps } from '../Popper';
import { PopoverBodyStylesNames } from './PopoverBody/PopoverBody';
import useStyles from './Popover.styles';
export declare type PopoverStylesNames = ClassNames<typeof useStyles> | PopoverBodyStylesNames;
export interface PopoverProps extends DefaultProps<PopoverStylesNames>, SharedPopperProps, Omit<React.ComponentPropsWithoutRef<'div'>, 'title'> {
    /** Disable closing by click outside */
    noClickOutside?: boolean;
    /** Disable focus trap (may impact close on escape feature) */
    noFocusTrap?: boolean;
    /** Disables close on escape */
    noEscape?: boolean;
    /** Adds close button */
    withCloseButton?: boolean;
    /** True to disable popover */
    disabled?: boolean;
    /** True to display popover */
    opened: boolean;
    /** Called when popover closes */
    onClose?(): void;
    /** Element which is used to position popover */
    target: React.ReactNode;
    /** Content inside popover */
    children: React.ReactNode;
    /** Optional popover title */
    title?: React.ReactNode;
    /** Popover body padding, value from theme.spacing or number to set padding in px */
    spacing?: MantineNumberSize;
    /** Popover body radius, value from theme.radius or number to set border-radius in px */
    radius?: MantineNumberSize;
    /** Popover shadow, value from theme.shadows or string to set box-shadow to any value */
    shadow?: MantineShadow;
    /** aria-label for close button */
    closeButtonLabel?: string;
    /** useEffect dependencies to force update tooltip position */
    positionDependencies?: any[];
    /** Whether to render the popover in a Portal */
    withinPortal?: boolean;
    /** Popover body width */
    width?: number | string;
    /** Events that should trigger outside clicks */
    clickOutsideEvents?: string[];
}
export declare function Popover({ className, children, target, title, onClose, opened, zIndex, arrowSize, withArrow, transition, transitionDuration, transitionTimingFunction, gutter, position, placement, disabled, noClickOutside, noFocusTrap, noEscape, withCloseButton, radius, spacing, shadow, closeButtonLabel, positionDependencies, withinPortal, id, classNames, styles, width, clickOutsideEvents, ...others }: PopoverProps): JSX.Element;
export declare namespace Popover {
    var displayName: string;
}
//# sourceMappingURL=Popover.d.ts.map