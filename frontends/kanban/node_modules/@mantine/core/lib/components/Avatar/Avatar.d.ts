import React from 'react';
import { DefaultProps, MantineNumberSize, MantineColor, ClassNames, PolymorphicComponentProps } from '@mantine/styles';
import useStyles from './Avatar.styles';
export declare type AvatarStylesNames = ClassNames<typeof useStyles>;
interface _AvatarProps extends DefaultProps<AvatarStylesNames> {
    /** Image url */
    src?: string | null;
    /** Image alt text or title for placeholder variant */
    alt?: string;
    /** Avatar width and height */
    size?: MantineNumberSize;
    /** Value from theme.radius or number to set border-radius in px */
    radius?: MantineNumberSize;
    /** Color from theme.colors used for letter and icon placeholders */
    color?: MantineColor;
    /** `img` element attributes */
    imageProps?: React.ComponentPropsWithoutRef<'img'>;
}
export declare type AvatarProps<C> = PolymorphicComponentProps<C, _AvatarProps>;
declare type AvatarComponent = (<C = 'div'>(props: AvatarProps<C>) => React.ReactElement) & {
    displayName?: string;
};
export declare const Avatar: AvatarComponent;
export {};
//# sourceMappingURL=Avatar.d.ts.map