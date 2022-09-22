import React from 'react';
declare type Ref<T> = React.Dispatch<React.SetStateAction<T>> | React.ForwardedRef<T>;
export declare function useMergedRef<T = any>(...refs: Ref<T>[]): (node: T | null) => void;
export declare function mergeRefs<T = any>(...refs: Ref<T>[]): (node: T | null) => void;
export {};
//# sourceMappingURL=use-merged-ref.d.ts.map