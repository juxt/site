import React from 'react';
import { DefaultProps, MantineNumberSize, ClassNames } from '@mantine/styles';
import useStyles from './ListItem.styles';
export declare type ListItemStylesNames = ClassNames<typeof useStyles>;
export interface ListItemProps extends DefaultProps<ListItemStylesNames>, React.ComponentPropsWithoutRef<'li'> {
    /** Icon to replace bullet */
    icon?: React.ReactNode;
    /** Item content */
    children: React.ReactNode;
    /** Predefined spacing between items or number to set value in px */
    spacing?: MantineNumberSize;
    /** Center item content with icon */
    center?: boolean;
}
export declare function ListItem({ className, children, icon, classNames, styles, spacing, center, ...others }: ListItemProps): JSX.Element;
export declare namespace ListItem {
    var displayName: string;
}
//# sourceMappingURL=ListItem.d.ts.map