import React from 'react';
import { DefaultProps, MantineNumberSize, MantineColor, ClassNames } from '@mantine/styles';
import useStyles from './Progress.styles';
export declare type ProgressStylesNames = ClassNames<typeof useStyles>;
export interface ProgressProps extends DefaultProps<ProgressStylesNames>, React.ComponentPropsWithoutRef<'div'> {
    /** Percent of filled bar (0-100) */
    value?: number;
    /** Progress color from theme */
    color?: MantineColor;
    /** Predefined progress height or number for height in px */
    size?: MantineNumberSize;
    /** Predefined progress radius from theme.radius or number for height in px */
    radius?: MantineNumberSize;
    /** Adds stripes */
    striped?: boolean;
    /** Whether to animate striped progress bars */
    animate?: boolean;
    /** Text to be placed inside the progress bar */
    label?: string;
    /** Replaces value if present, renders multiple sections instead of single one */
    sections?: {
        value: number;
        color: MantineColor;
        label?: string;
    }[];
}
export declare const Progress: React.ForwardRefExoticComponent<ProgressProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=Progress.d.ts.map