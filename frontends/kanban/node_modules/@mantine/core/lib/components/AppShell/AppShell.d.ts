import React from 'react';
import { MantineNumberSize, DefaultProps, ClassNames, MantineMargin } from '@mantine/styles';
import useStyles from './AppShell.styles';
export declare type AppShellStylesNames = ClassNames<typeof useStyles>;
export interface AppShellProps extends Omit<DefaultProps<AppShellStylesNames>, MantineMargin> {
    /** <Navbar /> component */
    navbar?: React.ReactElement;
    /** <Header /> component */
    header?: React.ReactElement;
    /** zIndex prop passed to Navbar and Header components */
    zIndex?: number;
    /** true to switch from static layout to fixed */
    fixed?: boolean;
    /** AppShell content */
    children: React.ReactNode;
    /** Content padding */
    padding?: MantineNumberSize;
    /** Breakpoint at which Navbar component should no longer be offset with padding-left, applicable only for fixed position */
    navbarOffsetBreakpoint?: MantineNumberSize;
}
export declare const AppShell: React.ForwardRefExoticComponent<AppShellProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=AppShell.d.ts.map