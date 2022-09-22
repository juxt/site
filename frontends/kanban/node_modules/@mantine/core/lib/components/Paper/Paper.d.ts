import React from 'react';
import { DefaultProps, MantineNumberSize, MantineShadow, PolymorphicComponentProps } from '@mantine/styles';
export interface SharedPaperProps extends DefaultProps {
    /** Predefined padding value from theme.spacing or number for padding in px */
    padding?: MantineNumberSize;
    /** Predefined box-shadow from theme.shadows (xs, sm, md, lg, xl) or any valid css box-shadow property */
    shadow?: MantineShadow;
    /** Predefined border-radius value from theme.radius or number for border-radius in px */
    radius?: MantineNumberSize;
    /** Adds 1px border with theme.colors.gray[2] color in light color scheme and theme.colors.dark[6] in dark color scheme */
    withBorder?: boolean;
}
export declare type PaperProps<C> = PolymorphicComponentProps<C, SharedPaperProps>;
declare type PaperComponent = (<C = 'div'>(props: PaperProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const Paper: PaperComponent;
export {};
//# sourceMappingURL=Paper.d.ts.map