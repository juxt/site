interface UseFocusReturn {
    opened: boolean;
    transitionDuration: number;
    shouldReturnFocus?: boolean;
}
/** Returns focus to last active element, used in Modal and Drawer */
export declare function useFocusReturn({ opened, transitionDuration, shouldReturnFocus, }: UseFocusReturn): () => void;
export {};
//# sourceMappingURL=use-focus-return.d.ts.map