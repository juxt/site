import React from 'react';
import { DefaultProps, MantineNumberSize, MantineColor, PolymorphicComponentProps, ClassNames } from '@mantine/styles';
import useStyles, { ActionIconVariant } from './ActionIcon.styles';
import { LoaderProps } from '../Loader';
export declare type ActionIconStylesNames = ClassNames<typeof useStyles>;
interface _ActionIconProps extends DefaultProps<ActionIconStylesNames> {
    /** Icon rendered inside button */
    children: React.ReactNode;
    /** Controls appearance */
    variant?: ActionIconVariant;
    /** Button hover, active and icon colors from theme */
    color?: MantineColor;
    /** Button border-radius from theme or number to set border-radius in px */
    radius?: MantineNumberSize;
    /** Predefined icon size or number to set width and height in px */
    size?: MantineNumberSize;
    /** Props spread to Loader component */
    loaderProps?: LoaderProps;
    /** Indicate loading state */
    loading?: boolean;
}
export declare type ActionIconProps<C> = PolymorphicComponentProps<C, _ActionIconProps>;
declare type ActionIconComponent = (<C = 'button'>(props: ActionIconProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const ActionIcon: ActionIconComponent;
export {};
//# sourceMappingURL=ActionIcon.d.ts.map