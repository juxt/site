import React from 'react';
import { DefaultProps, MantineNumberSize } from '@mantine/styles';
import { HeaderPosition } from './Header.styles';
export interface HeaderProps extends DefaultProps, React.ComponentPropsWithoutRef<'nav'> {
    /** Header content */
    children: React.ReactNode;
    /** Header height */
    height: number | string;
    /** Header padding from theme.spacing or number to set padding in px */
    padding?: MantineNumberSize;
    /** Changes position to fixed, controlled by AppShell component if rendered inside */
    fixed?: boolean;
    /** Control top, left, right or bottom position values, controlled by AppShell component if rendered inside */
    position?: HeaderPosition;
    /** z-index */
    zIndex?: number;
}
export declare const Header: React.ForwardRefExoticComponent<HeaderProps & React.RefAttributes<HTMLElement>>;
//# sourceMappingURL=Header.d.ts.map