/// <reference types="react" />
import { MantineColor } from '@mantine/styles';
interface CurveProps {
    value?: number;
    size: number;
    offset: number;
    sum: number;
    thickness: number;
    lineRoundCaps: boolean;
    root?: boolean;
    color?: MantineColor;
}
export declare function Curve({ size, value, offset, sum, thickness, root, color, lineRoundCaps, }: CurveProps): JSX.Element;
export declare namespace Curve {
    var displayName: string;
}
export {};
//# sourceMappingURL=Curve.d.ts.map