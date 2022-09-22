export declare function createAccordionState(length: number, initialItem?: number): AccordionState;
export declare type AccordionState = Record<string, boolean>;
interface UseAccordionState {
    multiple?: boolean;
    initialState?: AccordionState;
    state?: AccordionState;
    total: number;
    initialItem?: number;
    onChange?(state: Record<string, boolean>): void;
}
export declare function useAccordionState({ initialState, total, initialItem, state, onChange, multiple, }: UseAccordionState): readonly [Record<string, boolean>, {
    readonly toggle: (index: number) => void;
    readonly setState: (nextValue: Record<string, boolean>) => void;
}];
export {};
//# sourceMappingURL=use-accordion-state.d.ts.map