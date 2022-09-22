import React from 'react';
import { DefaultProps, MantineNumberSize, MantineColor, ClassNames } from '@mantine/styles';
import useStyles from './Burger.styles';
export declare type BurgerStylesNames = Exclude<ClassNames<typeof useStyles>, 'opened'>;
export interface BurgerProps extends DefaultProps<BurgerStylesNames>, React.ComponentPropsWithoutRef<'button'> {
    /** Burger state: true for cross, false for burger */
    opened: boolean;
    /** Burger color value, not connected to theme.colors, defaults to theme.black with light color scheme and theme.white with dark */
    color?: MantineColor;
    /** Predefined burger size or number to set width and height in px */
    size?: MantineNumberSize;
}
export declare const Burger: React.ForwardRefExoticComponent<BurgerProps & React.RefAttributes<HTMLButtonElement>>;
//# sourceMappingURL=Burger.d.ts.map