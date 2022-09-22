import React from 'react';
import { DefaultProps, MantineNumberSize, MantineColor, ClassNames, ForwardRefWithStaticComponents } from '@mantine/styles';
import { GroupPosition } from '../Group';
import { TabControl, TabControlStylesNames } from './TabControl/TabControl';
import useStyles from './Tabs.styles';
export declare type TabsVariant = 'default' | 'outline' | 'pills' | 'unstyled';
export declare type TabsStylesNames = Exclude<ClassNames<typeof useStyles>, TabsVariant> | TabControlStylesNames;
export interface TabsProps extends DefaultProps<TabsStylesNames>, React.ComponentPropsWithRef<'div'> {
    /** <Tab /> components only */
    children: React.ReactNode;
    /** Index of initial tab */
    initialTab?: number;
    /** Index of active tab, overrides internal state */
    active?: number;
    /** Active tab color from theme.colors */
    color?: MantineColor;
    /** True if tabs should take all available space */
    grow?: boolean;
    /** Tab controls position */
    position?: GroupPosition;
    /** Called when tab control is clicked with tab index */
    onTabChange?(tabIndex: number, tabKey?: string): void;
    /** Controls appearance */
    variant?: TabsVariant;
    /** Controls tab content padding-top */
    tabPadding?: MantineNumberSize;
    /** Controls tab orientation */
    orientation?: 'horizontal' | 'vertical';
}
declare type TabsComponent = ForwardRefWithStaticComponents<TabsProps, {
    Tab: typeof TabControl;
}>;
export declare const Tabs: TabsComponent;
export {};
//# sourceMappingURL=Tabs.d.ts.map