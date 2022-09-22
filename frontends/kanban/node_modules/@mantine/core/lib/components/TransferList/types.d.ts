/// <reference types="react" />
export interface TransferListItem {
    value: string;
    label: string;
    group?: string;
    [key: string]: any;
}
export declare type TransferListData = [TransferListItem[], TransferListItem[]];
export interface TransferListItemComponentProps {
    data: TransferListItem;
    selected: boolean;
}
export declare type TransferListItemComponent = React.FC<TransferListItemComponentProps>;
//# sourceMappingURL=types.d.ts.map