import React from 'react';
import { DefaultProps, ForwardRefWithStaticComponents } from '@mantine/styles';
import { AccordionItem, AccordionItemStylesNames, AccordionIconPosition } from './AccordionItem/AccordionItem';
import { AccordionState } from './use-accordion-state/use-accordion-state';
export interface AccordionProps extends DefaultProps<AccordionItemStylesNames>, Omit<React.ComponentPropsWithRef<'div'>, 'onChange'> {
    /** <AccordionItem /> components only */
    children: React.ReactNode;
    /** Index of item which is initially opened (uncontrolled component) */
    initialItem?: number;
    /** Initial state (controls opened state of accordion items) for uncontrolled component */
    initialState?: AccordionState;
    /** Controlled state (controls opened state of accordion items) */
    state?: AccordionState;
    /** onChange handler for controlled component */
    onChange?(state: AccordionState): void;
    /** Allow multiple items to be opened at the same time */
    multiple?: boolean;
    /** Open/close item transition duration in ms */
    transitionDuration?: number;
    /** Used to connect accordion items controls to related content */
    id?: string;
    /** Replace icon on all items */
    icon?: React.ReactNode;
    /** Should icon rotation be disabled */
    disableIconRotation?: boolean;
    /** Change icon position: left or right */
    iconPosition?: AccordionIconPosition;
    /** Should icon be offset with padding, applicable only when iconPosition is right */
    offsetIcon?: boolean;
    /** Icon width in px */
    iconSize?: number;
    /** Heading level used for items */
    order?: 2 | 3 | 4 | 5 | 6;
}
declare type AccordionComponent = ForwardRefWithStaticComponents<AccordionProps, {
    Item: typeof AccordionItem;
}>;
export declare const Accordion: AccordionComponent;
export {};
//# sourceMappingURL=Accordion.d.ts.map