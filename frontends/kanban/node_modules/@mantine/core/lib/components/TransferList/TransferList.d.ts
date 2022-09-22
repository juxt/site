import React from 'react';
import { DefaultProps, MantineNumberSize } from '@mantine/styles';
import { RenderListStylesNames } from './RenderList/RenderList';
import { Selection } from './use-selection-state/use-selection-state';
import { TransferListData, TransferListItemComponent, TransferListItem } from './types';
export declare type TransferListStylesNames = RenderListStylesNames;
export interface TransferListProps extends DefaultProps<TransferListStylesNames>, Omit<React.ComponentPropsWithoutRef<'div'>, 'value' | 'onChange'> {
    /** Current value */
    value: TransferListData;
    /** Called when value changes */
    onChange(value: TransferListData): void;
    /** Initial items selection */
    initialSelection?: Selection;
    /** Custom item component */
    itemComponent?: TransferListItemComponent;
    /** Search fields placeholder */
    searchPlaceholder?: string;
    /** Nothing found message */
    nothingFound?: React.ReactNode;
    /** Function to filter search results */
    filter?(query: string, item: TransferListItem): boolean;
    /** Lists titles */
    titles?: [string, string];
    /** List items height */
    listHeight?: number;
    /** Change list component, can be used to add custom scrollbars */
    listComponent?: any;
    /** Breakpoint at which list will collapse to single column layout */
    breakpoint?: MantineNumberSize;
    /** Whether to hide the transfer all button */
    showTransferAll?: boolean;
    /** Limit amount of items showed at a time */
    limit?: number;
}
export declare function defaultFilter(query: string, item: TransferListItem): boolean;
export declare const TransferList: React.ForwardRefExoticComponent<TransferListProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=TransferList.d.ts.map