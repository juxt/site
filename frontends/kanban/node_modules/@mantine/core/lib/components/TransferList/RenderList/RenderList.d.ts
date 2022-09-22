import React from 'react';
import { DefaultProps, ClassNames } from '@mantine/styles';
import { TransferListItem, TransferListItemComponent } from '../types';
import useStyles from './RenderList.styles';
export declare type RenderListStylesNames = ClassNames<typeof useStyles>;
export interface RenderListProps extends DefaultProps<RenderListStylesNames> {
    data: TransferListItem[];
    onSelect(value: string): void;
    selection: string[];
    itemComponent: TransferListItemComponent;
    searchPlaceholder: string;
    filter(query: string, item: TransferListItem): boolean;
    nothingFound?: React.ReactNode;
    title?: React.ReactNode;
    reversed?: boolean;
    showTransferAll?: boolean;
    onMoveAll(): void;
    onMove(): void;
    height: number;
    listComponent?: React.FC<any>;
    limit?: number;
}
export declare function RenderList({ className, data, onSelect, selection, itemComponent: ItemComponent, listComponent, searchPlaceholder, filter, nothingFound, title, showTransferAll, reversed, onMoveAll, onMove, height, classNames, styles, limit, }: RenderListProps): JSX.Element;
export declare namespace RenderList {
    var displayName: string;
}
//# sourceMappingURL=RenderList.d.ts.map