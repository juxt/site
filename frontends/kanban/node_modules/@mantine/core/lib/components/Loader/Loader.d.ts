import React from 'react';
import { DefaultProps, MantineNumberSize, MantineColor, MantineTheme } from '@mantine/styles';
export declare const LOADER_SIZES: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
};
export interface LoaderProps extends DefaultProps, React.ComponentPropsWithoutRef<'svg'> {
    /** Defines width of loader */
    size?: MantineNumberSize;
    /** Loader color from theme */
    color?: MantineColor;
    /** Loader appearance */
    variant?: MantineTheme['loader'];
}
export declare function Loader({ size, color, variant, ...others }: LoaderProps): JSX.Element;
export declare namespace Loader {
    var displayName: string;
}
//# sourceMappingURL=Loader.d.ts.map