import type { MantineColor } from '@mantine/styles';
interface CurveData {
    value: number;
    color: MantineColor;
}
interface GetCurves {
    sections: CurveData[];
    size: number;
    thickness: number;
    renderRoundedLineCaps: boolean;
}
interface Curve {
    sum: number;
    offset: number;
    root: boolean;
    data: CurveData;
    lineRoundCaps?: boolean;
}
export declare function getCurves({ size, thickness, sections, renderRoundedLineCaps }: GetCurves): Curve[];
export {};
//# sourceMappingURL=get-curves.d.ts.map