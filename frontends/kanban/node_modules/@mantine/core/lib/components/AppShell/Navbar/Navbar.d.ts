import React from 'react';
import { ClassNames, DefaultProps, MantineNumberSize, ForwardRefWithStaticComponents } from '@mantine/styles';
import { NavbarSection } from './NavbarSection/NavbarSection';
import useStyles, { NavbarPosition, NavbarWidth } from './Navbar.styles';
export declare type NavbarStylesNames = ClassNames<typeof useStyles>;
export interface NavbarProps extends DefaultProps<NavbarStylesNames>, React.ComponentPropsWithRef<'nav'> {
    /** Navbar width with breakpoints */
    width?: NavbarWidth;
    /** Navbar height */
    height?: string | number;
    /** Navbar content */
    children: React.ReactNode;
    /** Navbar padding from theme.spacing or number to set padding in px */
    padding?: MantineNumberSize;
    /** Set position to fixed */
    fixed?: boolean;
    /** Position for fixed Navbar */
    position?: NavbarPosition;
    /** Breakpoint at which navbar will be hidden if hidden prop is true */
    hiddenBreakpoint?: MantineNumberSize;
    /** Set to true to hide breakpoint at hiddenBreakpoint */
    hidden?: boolean;
    /** z-index */
    zIndex?: number;
}
declare type NavbarComponent = ForwardRefWithStaticComponents<NavbarProps, {
    Section: typeof NavbarSection;
}>;
export declare const Navbar: NavbarComponent;
export {};
//# sourceMappingURL=Navbar.d.ts.map