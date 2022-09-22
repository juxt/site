import React from 'react';
import { DefaultProps, ClassNames } from '@mantine/styles';
import useStyles, { AccordionIconPosition } from './AccordionItem.styles';
export type { AccordionIconPosition };
export declare type AccordionItemStylesNames = ClassNames<typeof useStyles>;
export interface PublicAccordionItemProps extends DefaultProps<AccordionItemStylesNames>, React.ComponentPropsWithoutRef<'div'> {
    label?: React.ReactNode;
    icon?: React.ReactNode;
    children?: React.ReactNode;
    disableIconRotation?: boolean;
    iconPosition?: AccordionIconPosition;
    controlRef?: React.ForwardedRef<HTMLButtonElement>;
}
export interface AccordionItemProps extends PublicAccordionItemProps {
    opened?: boolean;
    onToggle?(): void;
    transitionDuration?: number;
    id?: string;
    onControlKeyDown?: (event: React.KeyboardEvent<HTMLButtonElement>) => void;
    offsetIcon?: boolean;
    iconSize?: number;
    order?: 2 | 3 | 4 | 5 | 6;
}
export declare function AccordionItem({ opened, onToggle, label, children, className, classNames, styles, transitionDuration, icon, disableIconRotation, offsetIcon, iconSize, iconPosition, order, id, controlRef, onControlKeyDown, ...others }: AccordionItemProps): JSX.Element;
export declare namespace AccordionItem {
    var displayName: string;
}
//# sourceMappingURL=AccordionItem.d.ts.map