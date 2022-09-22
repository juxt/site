export declare type AccordionIconPosition = 'right' | 'left';
interface AccordionItemStyles {
    transitionDuration: number;
    disableIconRotation: boolean;
    iconPosition: AccordionIconPosition;
    offsetIcon: boolean;
    iconSize: number;
}
declare const _default: (params: AccordionItemStyles, options?: import("@mantine/styles").UseStylesOptions<"content" | "icon" | "label" | "item" | "itemOpened" | "itemTitle" | "control" | "contentInner">) => {
    classes: Record<"content" | "icon" | "label" | "item" | "itemOpened" | "itemTitle" | "control" | "contentInner", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=AccordionItem.styles.d.ts.map