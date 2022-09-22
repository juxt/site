import React from 'react';
import { DefaultProps, MantineColor, ClassNames, MantineNumberSize } from '@mantine/styles';
import { SharedPopperProps } from '../Popper';
import useStyles from './Tooltip.styles';
export declare type TooltipStylesNames = ClassNames<typeof useStyles>;
export interface TooltipProps extends DefaultProps<TooltipStylesNames>, SharedPopperProps, React.ComponentPropsWithoutRef<'div'> {
    /** Tooltip content */
    label: React.ReactNode;
    /** Any react node that should trigger tooltip */
    children: React.ReactNode;
    /** Tooltip opened state for controlled variant */
    opened?: boolean;
    /** Close delay in ms, 0 to disable delay */
    delay?: number;
    /** Any color from theme.colors, defaults to gray in light color scheme and dark in dark colors scheme */
    color?: MantineColor;
    /** Radius from theme.radius, or number to set border-radius in px */
    radius?: MantineNumberSize;
    /** True to disable tooltip */
    disabled?: boolean;
    /** Arrow size in px */
    arrowSize?: number;
    /** Tooltip width in px or auto */
    width?: number | 'auto';
    /** Allow multiline tooltip content */
    wrapLines?: boolean;
    /** Allow pointer events on tooltip, warning: this may break some animations */
    allowPointerEvents?: boolean;
    /** Get tooltip ref */
    tooltipRef?: React.ForwardedRef<HTMLDivElement>;
    /** Tooltip id to bind aria-describedby */
    tooltipId?: string;
    /** useEffect dependencies to force update tooltip position */
    positionDependencies?: any[];
    /** Whether to render the target element in a Portal */
    withinPortal?: boolean;
}
export declare const Tooltip: React.ForwardRefExoticComponent<TooltipProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=Tooltip.d.ts.map