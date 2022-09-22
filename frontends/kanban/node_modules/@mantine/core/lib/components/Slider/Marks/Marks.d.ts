import React from 'react';
import { DefaultProps, MantineNumberSize, MantineColor, ClassNames } from '@mantine/styles';
import useStyles from './Marks.styles';
export declare type MarksStylesNames = ClassNames<typeof useStyles>;
export interface MarksProps extends DefaultProps<MarksStylesNames> {
    marks: {
        value: number;
        label?: React.ReactNode;
    }[];
    size: MantineNumberSize;
    color: MantineColor;
    min: number;
    max: number;
    value: number;
    onChange(value: number): void;
    offset?: number;
}
export declare function Marks({ marks, color, size, min, max, value, classNames, styles, offset, onChange, }: MarksProps): JSX.Element;
export declare namespace Marks {
    var displayName: string;
}
//# sourceMappingURL=Marks.d.ts.map