import React from 'react';
import { DefaultProps, PolymorphicComponentProps } from '@mantine/styles';
interface _NavbarSectionProps extends DefaultProps {
    /** Section children */
    children: React.ReactNode;
    /** Force section to take all available height */
    grow?: boolean;
}
export declare type NavbarSectionProps<C> = PolymorphicComponentProps<C, _NavbarSectionProps>;
declare type NavbarSectionComponent = <C = 'div'>(props: NavbarSectionProps<C>) => React.ReactElement;
export declare const NavbarSection: NavbarSectionComponent & {
    displayName?: string;
};
export {};
//# sourceMappingURL=NavbarSection.d.ts.map