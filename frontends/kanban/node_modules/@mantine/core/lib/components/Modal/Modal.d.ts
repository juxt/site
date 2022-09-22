import React from 'react';
import { DefaultProps, MantineNumberSize, MantineShadow, ClassNames, MantineMargin } from '@mantine/styles';
import { MantineTransition } from '../Transition';
import useStyles from './Modal.styles';
export declare type ModalStylesNames = ClassNames<typeof useStyles>;
export interface ModalProps extends Omit<DefaultProps<ModalStylesNames>, MantineMargin>, Omit<React.ComponentPropsWithoutRef<'div'>, 'title'> {
    /** Mounts modal if true */
    opened: boolean;
    /** Called when close button clicked and when escape key is pressed */
    onClose(): void;
    /** Modal title, displayed in header before close button */
    title?: React.ReactNode;
    /** Modal z-index property */
    zIndex?: number;
    /** Control vertical overflow behavior */
    overflow?: 'outside' | 'inside';
    /** Hides close button, modal still can be closed with escape key and by clicking outside */
    hideCloseButton?: boolean;
    /** Overlay below modal opacity, defaults to 0.75 in light theme and to 0.85 in dark theme */
    overlayOpacity?: number;
    /** Overlay below modal color, defaults to theme.black in light theme and to theme.colors.dark[9] in dark theme */
    overlayColor?: string;
    /** Modal radius */
    radius?: MantineNumberSize;
    /** Modal body width */
    size?: string | number;
    /** Modal body transition */
    transition?: MantineTransition;
    /** Duration in ms of modal transitions, set to 0 to disable all animations */
    transitionDuration?: number;
    /** Modal body transitionTimingFunction, defaults to theme.transitionTimingFunction */
    transitionTimingFunction?: string;
    /** Close button aria-label */
    closeButtonLabel?: string;
    /** id base, used to generate ids to connect modal title and body with aria- attributes, defaults to random id */
    id?: string;
    /** Modal shadow from theme or css value */
    shadow?: MantineShadow;
    /** Modal padding from theme or number value for padding in px */
    padding?: MantineNumberSize;
    /** Should modal be closed when outside click was registered? */
    closeOnClickOutside?: boolean;
    /** Should modal be closed when escape is pressed? */
    closeOnEscape?: boolean;
    /** Disables focus trap */
    noFocusTrap?: boolean;
    /** Controls if modal should be centered */
    centered?: boolean;
    /** Target element or selector where modal portal should be rendered */
    target?: HTMLElement | string;
}
export declare function MantineModal({ className, opened, title, onClose, children, hideCloseButton, overlayOpacity, size, transitionDuration, closeButtonLabel, overlayColor, overflow, transition, padding, shadow, radius, id, classNames, styles, closeOnClickOutside, noFocusTrap, closeOnEscape, centered, target, ...others }: ModalProps): JSX.Element;
export declare function Modal({ zIndex, target, ...props }: React.ComponentPropsWithoutRef<typeof MantineModal>): JSX.Element;
export declare namespace Modal {
    var displayName: string;
}
//# sourceMappingURL=Modal.d.ts.map