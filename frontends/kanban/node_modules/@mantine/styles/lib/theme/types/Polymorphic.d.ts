import React from 'react';
declare type ExtendedProps<_ExtendedProps = {}, OverrideProps = {}> = OverrideProps & Omit<_ExtendedProps, keyof OverrideProps>;
declare type PropsOf<C extends keyof JSX.IntrinsicElements | React.JSXElementConstructor<any>> = JSX.LibraryManagedAttributes<C, React.ComponentPropsWithoutRef<C>>;
declare type ComponentProp<C extends React.ElementType> = {
    /** Tag or component that should be used as root element */
    component?: C;
};
declare type InheritedProps<C extends React.ElementType, Props = {}> = ExtendedProps<PropsOf<C>, Props>;
export declare type PolymorphicRef<C extends React.ElementType> = React.ComponentPropsWithRef<C>['ref'];
export declare type PolymorphicComponentProps<C, Props = {}> = C extends React.ElementType ? InheritedProps<C, Props & ComponentProp<C>> & {
    ref?: PolymorphicRef<C>;
} : Props & {
    component: React.ElementType;
};
export {};
//# sourceMappingURL=Polymorphic.d.ts.map