import React from 'react';
import { DefaultProps, MantineSize, MantineGradient, MantineColor, PolymorphicComponentProps } from '@mantine/styles';
export interface SharedTextProps extends DefaultProps {
    /** Predefined font-size from theme.fontSizes */
    size?: MantineSize;
    /** Text color from theme */
    color?: MantineColor;
    /** Sets font-weight css property */
    weight?: React.CSSProperties['fontWeight'];
    /** Sets text-transform css property */
    transform?: 'capitalize' | 'uppercase' | 'lowercase' | 'none';
    /** Sets text-align css property */
    align?: 'left' | 'center' | 'right' | 'justify';
    /** Link or text variant */
    variant?: 'text' | 'link' | 'gradient';
    /** CSS -webkit-line-clamp property */
    lineClamp?: number;
    /** Sets line-height to 1 for centering */
    inline?: boolean;
    /** Underline the text */
    underline?: boolean;
    /** Inherit font properties from parent element */
    inherit?: boolean;
    /** Controls gradient settings in gradient variant only */
    gradient?: MantineGradient;
}
export declare type TextProps<C> = PolymorphicComponentProps<C, SharedTextProps>;
declare type TextComponent = (<C = 'div'>(props: TextProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const Text: TextComponent;
export {};
//# sourceMappingURL=Text.d.ts.map