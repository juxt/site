import React from 'react';
import { DefaultProps, ClassNames } from '@mantine/styles';
import useStyles from './ScrollArea.styles';
export declare type ScrollAreaStylesNames = ClassNames<typeof useStyles>;
export interface ScrollAreaProps extends DefaultProps<ScrollAreaStylesNames>, React.ComponentPropsWithoutRef<'div'> {
    /** Scrollbar size in px */
    scrollbarSize?: number;
    /** Scrollbars type */
    type?: 'auto' | 'always' | 'scroll' | 'hover';
    /** Scroll hide delay in ms, for scroll and hover types only */
    scrollHideDelay?: number;
    /** Reading direction of the scroll area */
    dir?: 'ltr' | 'rtl';
    /** Should scrollbars be offset with padding */
    offsetScrollbars?: boolean;
    /** Get viewport ref */
    viewportRef?: React.ForwardedRef<HTMLDivElement>;
    /** Subscribe to scroll position changes */
    onScrollPositionChange?(position: {
        x: number;
        y: number;
    }): void;
}
export declare const ScrollArea: React.ForwardRefExoticComponent<ScrollAreaProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=ScrollArea.d.ts.map