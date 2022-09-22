import React from 'react';
import { DefaultProps, MantineSize, MantineColor, ClassNames } from '@mantine/styles';
import useStyles from './Radio.styles';
export declare type RadioStylesNames = Exclude<ClassNames<typeof useStyles>, 'labelDisabled'>;
export interface RadioProps extends DefaultProps<RadioStylesNames>, Omit<React.ComponentPropsWithoutRef<'input'>, 'size'> {
    /** Radio label */
    children?: React.ReactNode;
    /** Radio value */
    value: string;
    /** Active radio color from theme.colors */
    color?: MantineColor;
    /** Predefined label fontSize, radio width, height and border-radius */
    size?: MantineSize;
    /** Static selector base */
    __staticSelector?: string;
}
export declare const Radio: React.ForwardRefExoticComponent<RadioProps & React.RefAttributes<HTMLInputElement>>;
//# sourceMappingURL=Radio.d.ts.map