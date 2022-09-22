import type { SelectItem } from '../types';
interface FilterData {
    data: SelectItem[];
    limit: number;
    searchable: boolean;
    searchValue: string;
    filter(value: string, item: SelectItem): boolean;
}
export declare function filterData({ data, searchable, limit, searchValue, filter }: FilterData): any[];
export {};
//# sourceMappingURL=filter-data.d.ts.map