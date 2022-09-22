export declare type UncontrolledMode = 'initial' | 'controlled' | 'uncontrolled';
export interface UncontrolledOptions<T> {
    value: T | null | undefined;
    defaultValue: T | null | undefined;
    finalValue: T | null;
    onChange(value: T | null): void;
    onValueUpdate?(value: T | null): void;
    rule: (value: T | null | undefined) => boolean;
}
export declare function useUncontrolled<T>({ value, defaultValue, finalValue, rule, onChange, onValueUpdate, }: UncontrolledOptions<T>): readonly [T | null, (nextValue: T | null) => void, UncontrolledMode];
//# sourceMappingURL=use-uncontrolled.d.ts.map