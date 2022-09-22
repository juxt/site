import React from 'react';
import { DefaultProps, MantineNumberSize, ClassNames, ForwardRefWithStaticComponents } from '@mantine/styles';
import { ListItem, ListItemStylesNames } from './ListItem/ListItem';
import useStyles from './List.styles';
export declare type ListStylesNames = ListItemStylesNames | ClassNames<typeof useStyles>;
export interface ListProps extends DefaultProps<ListStylesNames>, React.ComponentPropsWithoutRef<'ul'> {
    /** <List.Item /> components only */
    children: React.ReactNode;
    /** List type: ol or ul */
    type?: 'order' | 'unordered';
    /** Include padding-left to offset list from main content */
    withPadding?: boolean;
    /** Font size from theme or number to set value in px */
    size?: MantineNumberSize;
    /** Icon that should replace list item dot */
    icon?: React.ReactNode;
    /** Spacing between items from theme or number to set value in px */
    spacing?: MantineNumberSize;
    /** Center items with icon */
    center?: boolean;
    /** List style */
    listStyleType?: React.CSSProperties['listStyleType'];
}
declare type ListComponent = ForwardRefWithStaticComponents<ListProps, {
    Item: typeof ListItem;
}>;
export declare const List: ListComponent;
export {};
//# sourceMappingURL=List.d.ts.map