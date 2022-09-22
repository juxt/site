import React from 'react';
import { PolymorphicComponentProps } from '@mantine/styles';
import { SharedPaperProps } from '../Paper/Paper';
import { CardSection } from './CardSection/CardSection';
interface _CardProps extends SharedPaperProps {
    /** Card content */
    children: React.ReactNode;
}
export declare type CardProps<C> = PolymorphicComponentProps<C, _CardProps>;
declare type CardComponent = (<C = 'div'>(props: CardProps<C>) => React.ReactElement) & {
    displayName?: string;
    Section: typeof CardSection;
};
export declare const Card: CardComponent;
export {};
//# sourceMappingURL=Card.d.ts.map