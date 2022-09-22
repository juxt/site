import { MantineTheme } from '../../types';
interface GetSharedColorScheme {
    color?: string;
    variant: 'outline' | 'filled' | 'light' | 'gradient' | 'white' | 'default' | 'subtle';
    gradient?: {
        from: string;
        to: string;
        deg: number;
    };
    theme: MantineTheme;
}
export interface MantineGradient {
    from: string;
    to: string;
    deg?: number;
}
/**
 * Provides shared theme styles for components that use theme.colors:
 * Button, ActionIcon, Badge, ThemeIcon, etc.
 */
export declare function getSharedColorScheme({ color, theme, variant, gradient }: GetSharedColorScheme): {
    border: string;
    background: string;
    color: string;
    hover: string;
};
export {};
//# sourceMappingURL=get-shared-color-scheme.d.ts.map