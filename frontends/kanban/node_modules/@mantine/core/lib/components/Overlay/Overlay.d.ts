import React from 'react';
import { PolymorphicComponentProps, MantineNumberSize, DefaultProps } from '@mantine/styles';
interface _OverlayProps extends DefaultProps {
    /** Overlay opacity */
    opacity?: React.CSSProperties['opacity'];
    /** Overlay background-color */
    color?: React.CSSProperties['backgroundColor'];
    /** Use gradient instead of background-color */
    gradient?: string;
    /** Overlay z-index */
    zIndex?: React.CSSProperties['zIndex'];
    /** Value from theme.radius or number to set border-radius in px */
    radius?: MantineNumberSize;
}
export declare type OverlayProps<C> = PolymorphicComponentProps<C, _OverlayProps>;
declare type OverlayComponent = (<C = 'div'>(props: OverlayProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const Overlay: OverlayComponent;
export {};
//# sourceMappingURL=Overlay.d.ts.map