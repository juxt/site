/// <reference types="react" />
import { CSSObject } from '@emotion/react';
import type { MantineTheme } from '../theme/types';
interface GlobalStylesProps {
    styles(theme: MantineTheme): CSSObject;
}
export declare function Global({ styles }: GlobalStylesProps): JSX.Element;
export {};
//# sourceMappingURL=Global.d.ts.map