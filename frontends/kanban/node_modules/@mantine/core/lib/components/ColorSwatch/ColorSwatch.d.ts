import React from 'react';
import { DefaultProps, MantineNumberSize, PolymorphicComponentProps } from '@mantine/styles';
interface _ColorSwatchProps extends DefaultProps {
    /** Swatch color value in any css valid format (hex, rgb, etc.) */
    color: string;
    /** Width, height and border-radius in px */
    size?: number;
    /** Swatch border-radius predefined from theme or number for px value */
    radius?: MantineNumberSize;
}
export declare type ColorSwatchProps<C> = PolymorphicComponentProps<C, _ColorSwatchProps>;
declare type ColorSwatchComponent = (<C = 'div'>(props: ColorSwatchProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const ColorSwatch: ColorSwatchComponent;
export {};
//# sourceMappingURL=ColorSwatch.d.ts.map