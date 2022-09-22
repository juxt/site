import React from 'react';
import { DefaultProps, MantineNumberSize, ClassNames, MantineMargin } from '@mantine/styles';
import { MantineTransition } from '../Transition';
import { PaperProps } from '../Paper/Paper';
import useStyles from './Dialog.styles';
export declare type DialogStylesNames = ClassNames<typeof useStyles>;
export interface DialogProps extends Omit<DefaultProps<DialogStylesNames>, MantineMargin>, Omit<PaperProps<'div'>, 'classNames' | 'styles'> {
    /** Display close button at the top right corner */
    withCloseButton?: boolean;
    /** Called when close button is clicked */
    onClose?(): void;
    /** Dialog position (fixed in viewport) */
    position?: {
        top?: string | number;
        left?: string | number;
        bottom?: string | number;
        right?: string | number;
    };
    /** Dialog content */
    children?: React.ReactNode;
    /** Dialog container z-index */
    zIndex?: number;
    /** Opened state */
    opened: boolean;
    /** Appear/disappear transition */
    transition?: MantineTransition;
    /** Duration in ms of modal transitions, set to 0 to disable all animations */
    transitionDuration?: number;
    /** Transition timing function, defaults to theme.transitionTimingFunction */
    transitionTimingFunction?: string;
    /** Predefined dialog width or number to set width in px */
    size?: MantineNumberSize;
}
export declare function MantineDialog({ withCloseButton, onClose, position, shadow, padding, children, className, style, classNames, styles, opened, withBorder, size, transition, transitionDuration, transitionTimingFunction, ...others }: DialogProps): JSX.Element;
declare type DialogComponent = (props: DialogProps) => React.ReactElement;
export declare const Dialog: DialogComponent & {
    displayName?: string;
};
export {};
//# sourceMappingURL=Dialog.d.ts.map