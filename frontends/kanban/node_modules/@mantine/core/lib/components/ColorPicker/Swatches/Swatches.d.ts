import React from 'react';
import { DefaultProps, ClassNames } from '@mantine/styles';
import { HsvaColor } from '../types';
import useStyles from './Swatches.styles';
export declare type SwatchesStylesNames = ClassNames<typeof useStyles>;
export interface SwatchesProps extends DefaultProps<SwatchesStylesNames>, Omit<React.ComponentPropsWithoutRef<'div'>, 'onSelect'> {
    data: string[];
    onSelect(color: HsvaColor): void;
    swatchesPerRow?: number;
    focusable?: boolean;
    __staticSelector?: string;
}
export declare function Swatches({ data, onSelect, swatchesPerRow, focusable, classNames, styles, __staticSelector, ...others }: SwatchesProps): JSX.Element;
export declare namespace Swatches {
    var displayName: string;
}
//# sourceMappingURL=Swatches.d.ts.map