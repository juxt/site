import React from 'react';
import { DefaultProps, MantineColor, ClassNames } from '@mantine/styles';
import useStyles from './RingProgress.styles';
export declare type RingProgressStylesNames = ClassNames<typeof useStyles>;
export interface RingProgressProps extends DefaultProps<RingProgressStylesNames>, React.ComponentPropsWithoutRef<'div'> {
    /** Label displayed in the center of the ring */
    label?: React.ReactNode;
    /** Ring thickness */
    thickness?: number;
    /** Width and height of the progress ring in px */
    size?: number;
    /** Sets whether the edges of the progress circle are rounded */
    roundCaps?: boolean;
    /** Ring sections */
    sections: {
        value: number;
        color: MantineColor;
    }[];
}
export declare const RingProgress: React.ForwardRefExoticComponent<RingProgressProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=RingProgress.d.ts.map