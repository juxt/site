export declare function useLocalStorageValue<T extends string>({ key, defaultValue, }: {
    key: string;
    defaultValue?: T;
}): readonly [T, (val: T | ((prevState: T) => T)) => void];
//# sourceMappingURL=use-local-storage-value.d.ts.map