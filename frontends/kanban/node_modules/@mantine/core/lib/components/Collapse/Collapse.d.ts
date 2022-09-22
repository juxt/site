import React from 'react';
import { DefaultProps } from '@mantine/styles';
export interface CollapseProps extends DefaultProps, React.ComponentPropsWithoutRef<'div'> {
    /** Content that should be collapsed */
    children: React.ReactNode;
    /** Opened state */
    in: boolean;
    /** Called each time transition ends */
    onTransitionEnd?: () => void;
    /** Transition duration in ms */
    transitionDuration?: number;
    /** Transition timing function */
    transitionTimingFunction?: string;
    /** Should opacity be animated */
    animateOpacity?: boolean;
}
export declare function Collapse({ children, in: opened, transitionDuration, transitionTimingFunction, style, onTransitionEnd, animateOpacity, ...others }: CollapseProps): JSX.Element;
export declare namespace Collapse {
    var displayName: string;
}
//# sourceMappingURL=Collapse.d.ts.map