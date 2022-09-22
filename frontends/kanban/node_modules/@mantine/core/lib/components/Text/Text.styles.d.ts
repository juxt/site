/// <reference types="react" />
import { MantineTheme, MantineSize, MantineColor } from '@mantine/styles';
interface TextStyles {
    color: MantineColor;
    variant: 'text' | 'link' | 'gradient';
    size: MantineSize;
    lineClamp: number;
    inline: boolean;
    inherit: boolean;
    underline: boolean;
    gradientFrom: string;
    gradientTo: string;
    gradientDeg: number;
    transform: 'capitalize' | 'uppercase' | 'lowercase' | 'none';
    align: 'left' | 'center' | 'right' | 'justify';
    weight: React.CSSProperties['fontWeight'];
}
declare const _default: (params: TextStyles, options?: import("@mantine/styles").UseStylesOptions<"gradient" | "root">) => {
    classes: Record<"gradient" | "root", string>;
    cx: (...args: any) => string;
    theme: MantineTheme;
};
export default _default;
//# sourceMappingURL=Text.styles.d.ts.map