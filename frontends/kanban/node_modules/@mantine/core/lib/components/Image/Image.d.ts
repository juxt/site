import React from 'react';
import { DefaultProps, MantineNumberSize, ClassNames } from '@mantine/styles';
import useStyles from './Image.styles';
export declare type ImageStylesNames = ClassNames<typeof useStyles>;
export interface ImageProps extends DefaultProps<ImageStylesNames>, Omit<React.ComponentPropsWithoutRef<'div'>, 'placeholder'> {
    /** Image src */
    src?: string;
    /** Image alt text, used as title for placeholder if image was not loaded */
    alt?: string;
    /** Image object-fit property */
    fit?: 'contain' | 'cover';
    /** Image width, defaults to 100%, cannot exceed 100% */
    width?: number | string;
    /** Image height, defaults to original image height adjusted to given width */
    height?: number | string;
    /** Predefined border-radius value from theme.radius or number for border-radius in px */
    radius?: MantineNumberSize;
    /** Enable placeholder when image is loading and when image fails to load */
    withPlaceholder?: boolean;
    /** Customize placeholder content */
    placeholder?: React.ReactNode;
    /** Props spread to img element */
    imageProps?: React.ComponentPropsWithoutRef<'img'>;
    /** Get image element ref */
    imageRef?: React.ForwardedRef<HTMLImageElement>;
    /** Image figcaption, displayed bellow image */
    caption?: React.ReactNode;
}
export declare const Image: React.ForwardRefExoticComponent<ImageProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=Image.d.ts.map