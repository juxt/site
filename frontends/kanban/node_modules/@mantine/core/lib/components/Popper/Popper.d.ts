import React from 'react';
import { StrictModifier } from 'react-popper';
import { MantineTransition } from '../Transition';
export interface SharedPopperProps {
    /** Position relative to reference element */
    position?: 'top' | 'left' | 'bottom' | 'right';
    /** Placement relative to reference element */
    placement?: 'start' | 'center' | 'end';
    /** Spacing between element and popper in px  */
    gutter?: number;
    /** Arrow size in px */
    arrowSize?: number;
    /** Arrow distance to the left/right * arrowSize */
    arrowDistance?: number;
    /** Renders arrow if true */
    withArrow?: boolean;
    /** Popper z-index */
    zIndex?: number;
    /** Customize mount/unmount transition */
    transition?: MantineTransition;
    /** Mount transition duration in ms */
    transitionDuration?: number;
    /** Unmount transition duration in ms */
    exitTransitionDuration?: number;
    /** Mount/unmount transition timing function, defaults to theme.transitionTimingFunction */
    transitionTimingFunction?: string;
}
export interface PopperProps<T extends HTMLElement> extends SharedPopperProps {
    /** Element at which popper should be attached */
    referenceElement: T;
    /** Popper content */
    children: React.ReactNode;
    /** True to show popper, false to hide */
    mounted: boolean;
    /** Arrow class name */
    arrowClassName?: string;
    /** Arrow inline styles */
    arrowStyle?: React.CSSProperties;
    /** useEffect dependencies to force update popper position */
    forceUpdateDependencies?: any[];
    /** Called when transition ends */
    onTransitionEnd?(): void;
    /** Popperjs modifiers array */
    modifiers?: StrictModifier[];
    /** Whether to render the target element in a Portal */
    withinPortal?: boolean;
}
export declare function Popper<T extends HTMLElement = HTMLDivElement>({ position, placement, gutter, arrowSize, arrowDistance, withArrow, referenceElement, children, mounted, transition, transitionDuration, exitTransitionDuration, transitionTimingFunction, arrowClassName, arrowStyle, zIndex, forceUpdateDependencies, modifiers, onTransitionEnd, withinPortal, }: PopperProps<T>): JSX.Element;
export declare namespace Popper {
    var displayName: string;
}
//# sourceMappingURL=Popper.d.ts.map