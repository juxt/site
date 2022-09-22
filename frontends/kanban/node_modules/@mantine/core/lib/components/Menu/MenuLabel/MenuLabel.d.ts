import React from 'react';
import { DefaultProps } from '@mantine/styles';
import { SharedTextProps } from '../../Text/Text';
export interface MenuLabelProps extends DefaultProps, SharedTextProps, Omit<React.ComponentPropsWithoutRef<'div'>, 'color'> {
    /** Label content */
    children: React.ReactNode;
}
export interface MenuLabelType {
    type: any;
    props: MenuLabelProps;
    ref?: React.RefObject<HTMLButtonElement> | ((instance: HTMLButtonElement) => void);
}
export declare function MenuLabel(props: MenuLabelProps): any;
export declare namespace MenuLabel {
    var displayName: string;
}
//# sourceMappingURL=MenuLabel.d.ts.map