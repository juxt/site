import { MantineNumberSize, CSSObject } from '@mantine/styles';
interface MediaQueryStyles {
    smallerThan: MantineNumberSize;
    largerThan: MantineNumberSize;
    styles: CSSObject;
    query: string;
}
declare const _default: (params: MediaQueryStyles, options?: import("@mantine/styles").UseStylesOptions<"media">) => {
    classes: Record<"media", string>;
    cx: (...args: any) => string;
    theme: import("@mantine/styles").MantineTheme;
};
export default _default;
//# sourceMappingURL=MediaQuery.styles.d.ts.map