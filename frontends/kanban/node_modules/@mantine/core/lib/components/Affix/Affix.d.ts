import React from 'react';
import { DefaultProps, MantineMargin } from '@mantine/styles';
export interface AffixProps extends Omit<DefaultProps, MantineMargin>, React.ComponentPropsWithoutRef<'div'> {
    /** Element where portal should be rendered, by default new div element is created and appended to document.body */
    target?: HTMLDivElement;
    /** Root element z-index property */
    zIndex?: number;
    /** Fixed position in px */
    position?: {
        top?: string | number;
        left?: string | number;
        bottom?: string | number;
        right?: string | number;
    };
}
export declare const Affix: React.ForwardRefExoticComponent<AffixProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=Affix.d.ts.map