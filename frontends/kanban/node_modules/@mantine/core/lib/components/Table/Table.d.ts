import React from 'react';
import { DefaultProps, MantineNumberSize } from '@mantine/styles';
export interface TableProps extends DefaultProps, React.ComponentPropsWithoutRef<'table'> {
    /** If true every odd row of table will have gray background color */
    striped?: boolean;
    /** If true row will have hover color */
    highlightOnHover?: boolean;
    /** Table caption position */
    captionSide?: 'top' | 'bottom';
    /** Horizontal cells spacing from theme.spacing or number to set value in px */
    horizontalSpacing?: MantineNumberSize;
    /** Vertical cells spacing from theme.spacing or number to set value in px */
    verticalSpacing?: MantineNumberSize;
}
export declare const Table: React.ForwardRefExoticComponent<TableProps & React.RefAttributes<HTMLTableElement>>;
//# sourceMappingURL=Table.d.ts.map