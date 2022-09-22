import React from 'react';
import { DefaultProps, PolymorphicComponentProps } from '@mantine/styles';
export interface _CenterProps extends DefaultProps {
    /** Content that should be centered vertically and horizontally */
    children: React.ReactNode;
    /** Set to true to use inline-flex instead of flex */
    inline?: boolean;
}
export declare type CenterProps<C> = PolymorphicComponentProps<C, _CenterProps>;
declare type CenterComponent = (<C = 'div'>(props: CenterProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const Center: CenterComponent;
export {};
//# sourceMappingURL=Center.d.ts.map