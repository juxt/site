import React from 'react';
import { PolymorphicComponentProps } from '@mantine/styles';
import { SharedTextProps } from '../Text/Text';
export declare type AnchorProps<C> = PolymorphicComponentProps<C, SharedTextProps>;
declare type AnchorComponent = (<C = 'a'>(props: AnchorProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const Anchor: AnchorComponent;
export {};
//# sourceMappingURL=Anchor.d.ts.map