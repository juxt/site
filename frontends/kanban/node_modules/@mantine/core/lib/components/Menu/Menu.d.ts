import React from 'react';
import { DefaultProps, MantineNumberSize, MantineShadow, ClassNames, ForwardRefWithStaticComponents } from '@mantine/styles';
import { SharedPopperProps } from '../Popper';
import { MenuItem, MenuItemStylesNames } from './MenuItem/MenuItem';
import { MenuLabel } from './MenuLabel/MenuLabel';
import useStyles from './Menu.styles';
export declare type MenuStylesNames = ClassNames<typeof useStyles> | MenuItemStylesNames;
export interface MenuProps extends DefaultProps<MenuStylesNames>, SharedPopperProps, React.ComponentPropsWithRef<'div'> {
    /** <MenuItem /> and <Divider /> components only, children are passed to MenuBody component  */
    children: React.ReactNode;
    /** React element that will be used as menu control */
    control?: React.ReactElement;
    /** Use opened and onClose props to setup controlled menu */
    opened?: boolean;
    /** Called every time menu is closed */
    onClose?(): void;
    /** Called every time menu is opened */
    onOpen?(): void;
    /** Menu button aria-label and title props */
    menuButtonLabel?: string;
    /** Predefined menu width or number for width in px */
    size?: MantineNumberSize | 'auto';
    /** Predefined shadow from theme or box-shadow value */
    shadow?: MantineShadow;
    /** Should menu close on item click */
    closeOnItemClick?: boolean;
    /** Id attribute of menu */
    menuId?: string;
    /** Control prop to get element ref */
    controlRefProp?: string;
    /** Menu body z-index */
    zIndex?: number;
    /** Event which should open menu */
    trigger?: 'click' | 'hover';
    /** Close delay for hover trigger */
    delay?: number;
    /** Menu body and items border-radius */
    radius?: MantineNumberSize;
    /** Close menu on scroll */
    closeOnScroll?: boolean;
    /** Whether to render the dropdown in a Portal */
    withinPortal?: boolean;
    /** Should focus be trapped when menu is opened */
    trapFocus?: boolean;
    /** Events that should trigger outside clicks */
    clickOutsideEvents?: string[];
}
declare type MenuComponent = ForwardRefWithStaticComponents<MenuProps, {
    Item: typeof MenuItem;
    Label: typeof MenuLabel;
}>;
export declare const Menu: MenuComponent;
export {};
//# sourceMappingURL=Menu.d.ts.map