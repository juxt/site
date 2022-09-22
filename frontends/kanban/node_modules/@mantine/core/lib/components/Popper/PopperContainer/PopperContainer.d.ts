import React from 'react';
export interface PopperContainerProps {
    /** PopperContainer children, for example, modal or popover */
    children: React.ReactNode;
    /** Root element z-index property */
    zIndex?: number;
    /** Root element className */
    className?: string;
    /** Whether to render the target element in a Portal */
    withinPortal?: boolean;
}
export declare function PopperContainer({ children, zIndex, className, withinPortal, }: PopperContainerProps): JSX.Element;
export declare namespace PopperContainer {
    var displayName: string;
}
//# sourceMappingURL=PopperContainer.d.ts.map