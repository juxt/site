import type { MantineTheme } from '../theme';
import type { CSSObject } from './types';
export interface UseStylesOptions<Key extends string> {
    classNames?: Partial<Record<Key, string>>;
    styles?: Partial<Record<Key, CSSObject>> | ((theme: MantineTheme) => Partial<Record<Key, CSSObject>>);
    name: string;
}
export declare function createStyles<Key extends string = string, Params = void>(getCssObjectOrCssObject: ((theme: MantineTheme, params: Params, createRef: (refName: string) => string) => Record<Key, CSSObject>) | Record<Key, CSSObject>): (params: Params, options?: UseStylesOptions<Key>) => {
    classes: Record<Key, string>;
    cx: (...args: any) => string;
    theme: MantineTheme;
};
//# sourceMappingURL=create-styles.d.ts.map