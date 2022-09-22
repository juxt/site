import React from 'react';
import { DefaultProps } from '@mantine/styles';
export declare type TitleOrder = 1 | 2 | 3 | 4 | 5 | 6;
export interface TitleProps extends DefaultProps, React.ComponentPropsWithoutRef<'h1'> {
    /** Defines component and styles which will be used */
    order?: TitleOrder;
    /** Defined text-align */
    align?: 'right' | 'left' | 'center' | 'justify';
}
export declare const Title: React.ForwardRefExoticComponent<TitleProps & React.RefAttributes<HTMLHeadingElement>>;
//# sourceMappingURL=Title.d.ts.map