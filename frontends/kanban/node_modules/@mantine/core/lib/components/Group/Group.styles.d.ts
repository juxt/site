import React from 'react';
import { MantineNumberSize } from '@mantine/styles';
export declare type GroupPosition = 'right' | 'center' | 'left' | 'apart';
interface GroupStyles {
    position: GroupPosition;
    noWrap: boolean;
    grow: boolean;
    spacing: MantineNumberSize;
    direction: 'row' | 'column';
    align: React.CSSProperties['alignItems'];
    count: number;
}
declare const _default: (params: GroupStyles, options?: import("@mantine/styles").UseStylesOptions<"child" | "root">) => {
    classes: Record<"child" | "root", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Group.styles.d.ts.map