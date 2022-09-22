import React from 'react';
import { MantineNumberSize, ClassNames, PolymorphicComponentProps, DefaultProps, MantineColor } from '@mantine/styles';
import useStyles from './MenuItem.styles';
export declare type MenuItemStylesNames = ClassNames<typeof useStyles>;
export interface SharedMenuItemProps extends DefaultProps<MenuItemStylesNames> {
    /** Item label */
    children: React.ReactNode;
    /** Icon rendered on the left side of label */
    icon?: React.ReactNode;
    /** Any color from theme.colors */
    color?: MantineColor;
    /** Any react node to render on the right side of item, for example, keyboard shortcut or badge */
    rightSection?: React.ReactNode;
    /** Is item disabled */
    disabled?: boolean;
    /** Is item hovered, controlled by parent Menu component */
    hovered?: boolean;
    /** Called when item is hovered, controlled by parent Menu component */
    onHover?(): void;
    /** Border radius, controlled by parent Menu component */
    radius?: MantineNumberSize;
}
export declare type MenuItemProps<C> = PolymorphicComponentProps<C, SharedMenuItemProps>;
export declare type MenuItemComponent = <C = 'button'>(props: MenuItemProps<C>) => React.ReactElement;
export interface MenuItemType {
    type: any;
    props: MenuItemProps<'button'>;
    ref?: React.RefObject<HTMLButtonElement> | ((instance: HTMLButtonElement) => void);
}
export declare const MenuItem: MenuItemComponent & {
    displayName?: string;
};
//# sourceMappingURL=MenuItem.d.ts.map