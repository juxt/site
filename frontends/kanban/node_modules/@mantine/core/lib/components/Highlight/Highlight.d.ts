import React from 'react';
import { MantineColor, PolymorphicComponentProps, MantineTheme, CSSObject } from '@mantine/styles';
import { SharedTextProps } from '../Text/Text';
interface _HighlightProps extends SharedTextProps {
    /** Substring or an array of substrings to highlight in children */
    highlight: string | string[];
    /** Color from theme that is used for highlighting */
    highlightColor?: MantineColor;
    /** Styles applied to highlighted part */
    highlightStyles?: CSSObject | ((theme: MantineTheme) => CSSObject);
    /** Full string part of which will be highlighted */
    children: string;
}
export declare type HighlightProps<C> = PolymorphicComponentProps<C, _HighlightProps>;
declare type HighlightComponent = (<C = 'div'>(props: HighlightProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const Highlight: HighlightComponent;
export {};
//# sourceMappingURL=Highlight.d.ts.map