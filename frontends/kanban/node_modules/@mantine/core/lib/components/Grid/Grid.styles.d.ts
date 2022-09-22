/// <reference types="react" />
import { MantineNumberSize } from '@mantine/styles';
interface GridStyles {
    gutter: MantineNumberSize;
    justify?: React.CSSProperties['justifyContent'];
    align?: React.CSSProperties['alignContent'];
}
declare const _default: (params: GridStyles, options?: import("@mantine/styles").UseStylesOptions<"root">) => {
    classes: Record<"root", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=Grid.styles.d.ts.map