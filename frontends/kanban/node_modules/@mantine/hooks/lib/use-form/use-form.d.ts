import React from 'react';
export declare type ValidationRule<T> = {
    readonly [P in keyof T]?: (value: T[P], values?: T) => boolean;
};
export declare type UseFormErrors<T> = {
    readonly [P in keyof T]?: React.ReactNode | null;
};
interface UseFormInput<T> {
    validationRules?: ValidationRule<T>;
    errorMessages?: UseFormErrors<T>;
    initialValues: T;
}
export interface UseForm<T> {
    values: T;
    errors: Record<keyof T, React.ReactNode>;
    validate: () => boolean;
    reset: () => void;
    setErrors: React.Dispatch<React.SetStateAction<Record<keyof T, React.ReactNode>>>;
    setValues: React.Dispatch<React.SetStateAction<T>>;
    setFieldValue: <K extends keyof T, U extends T[K]>(field: K, value: U) => void;
    setFieldError: (field: keyof T, error: React.ReactNode) => void;
    validateField: (field: keyof T) => void;
    resetErrors: () => void;
    onSubmit: (handleSubmit: (values: T) => any) => (event?: React.FormEvent) => void;
    getInputProps: <K extends keyof T>(field: K, options?: {
        type?: 'checkbox' | 'default';
    }) => {
        [x: string]: any;
        onChange: any;
        error: Record<keyof T, React.ReactNode>[K];
    };
}
export declare function useForm<T extends {
    [key: string]: any;
}>({ initialValues, validationRules, errorMessages, }: UseFormInput<T>): UseForm<T>;
export {};
//# sourceMappingURL=use-form.d.ts.map