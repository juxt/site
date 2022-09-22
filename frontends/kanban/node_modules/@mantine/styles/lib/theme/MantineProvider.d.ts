import React from 'react';
import type { Options as EmotionCacheOptions } from '@emotion/cache';
import type { MantineThemeOverride, MantineTheme } from './types';
import type { CSSObject } from '../tss';
declare type ProviderStyles = Record<string, Record<string, CSSObject> | ((theme: MantineTheme) => Record<string, CSSObject>)>;
export declare function useMantineTheme(): MantineTheme;
export declare function useMantineThemeStyles(): ProviderStyles;
export declare function useMantineEmotionOptions(): EmotionCacheOptions;
export interface MantineProviderProps {
    theme?: MantineThemeOverride;
    styles?: ProviderStyles;
    emotionOptions?: EmotionCacheOptions;
    withNormalizeCSS?: boolean;
    withGlobalStyles?: boolean;
    children: React.ReactNode;
}
export declare function MantineProvider({ theme, styles, emotionOptions, withNormalizeCSS, withGlobalStyles, children, }: MantineProviderProps): JSX.Element;
export declare namespace MantineProvider {
    var displayName: string;
}
export {};
//# sourceMappingURL=MantineProvider.d.ts.map