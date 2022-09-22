import React from 'react';
import { DefaultProps, MantineNumberSize, PolymorphicComponentProps } from '@mantine/styles';
export interface _CardSectionProps extends DefaultProps {
    padding?: MantineNumberSize;
    first?: boolean;
    last?: boolean;
}
export declare type CardSectionProps<C> = PolymorphicComponentProps<C, _CardSectionProps>;
declare type CardSectionComponent = (<C = 'div'>(props: CardSectionProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const CardSection: CardSectionComponent;
export {};
//# sourceMappingURL=CardSection.d.ts.map