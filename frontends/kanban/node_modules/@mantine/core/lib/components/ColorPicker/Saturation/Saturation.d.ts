/// <reference types="react" />
import { DefaultProps, MantineSize, ClassNames } from '@mantine/styles';
import { HsvaColor } from '../types';
import { ThumbStylesNames } from '../Thumb/Thumb';
import useStyles from './Saturation.styles';
export declare type SaturationStylesNames = Exclude<ClassNames<typeof useStyles>, 'saturationOverlay' | 'saturationThumb'> | ThumbStylesNames;
interface SaturationProps extends DefaultProps<SaturationStylesNames> {
    value: HsvaColor;
    onChange(color: Partial<HsvaColor>): void;
    saturationLabel?: string;
    size: MantineSize;
    color: string;
    focusable?: boolean;
    __staticSelector?: string;
}
export declare function Saturation({ value, onChange, focusable, __staticSelector, size, color, saturationLabel, classNames, styles, }: SaturationProps): JSX.Element;
export declare namespace Saturation {
    var displayName: string;
}
export {};
//# sourceMappingURL=Saturation.d.ts.map