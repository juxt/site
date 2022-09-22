import React from 'react';
import { DefaultProps, PolymorphicComponentProps } from '@mantine/styles';
import { BoxSx } from './use-sx/use-sx';
interface _BoxProps extends Omit<DefaultProps, 'sx'> {
    sx?: BoxSx;
}
export declare type BoxProps<C> = PolymorphicComponentProps<C, _BoxProps>;
declare type BoxComponent = (<C = 'div'>(props: BoxProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const Box: BoxComponent;
export {};
//# sourceMappingURL=Box.d.ts.map