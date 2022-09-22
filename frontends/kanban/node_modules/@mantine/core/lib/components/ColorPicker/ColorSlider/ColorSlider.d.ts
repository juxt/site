import React from 'react';
import { DefaultProps, MantineSize, ClassNames } from '@mantine/styles';
import { ThumbStylesNames } from '../Thumb/Thumb';
import useStyles from './ColorSlider.styles';
export declare type ColorSliderStylesNames = Exclude<ClassNames<typeof useStyles>, 'sliderThumb'> | ThumbStylesNames;
export interface BaseColorSliderProps extends DefaultProps<ColorSliderStylesNames>, Omit<React.ComponentPropsWithoutRef<'div'>, 'value' | 'onChange'> {
    value: number;
    onChange(value: number): void;
    size?: MantineSize;
    focusable?: boolean;
    __staticSelector?: string;
}
export interface ColorSliderProps extends BaseColorSliderProps {
    maxValue: number;
    overlays: React.CSSProperties[];
    round: boolean;
    thumbColor?: string;
}
export declare const ColorSlider: React.ForwardRefExoticComponent<ColorSliderProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=ColorSlider.d.ts.map