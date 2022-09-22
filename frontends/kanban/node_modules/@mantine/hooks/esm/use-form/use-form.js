import { useState } from 'react';
import { getInputOnChange } from '../use-input-state/use-input-state.js';

var __defProp = Object.defineProperty;
var __defProps = Object.defineProperties;
var __getOwnPropDescs = Object.getOwnPropertyDescriptors;
var __getOwnPropSymbols = Object.getOwnPropertySymbols;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __propIsEnum = Object.prototype.propertyIsEnumerable;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __spreadValues = (a, b) => {
  for (var prop in b || (b = {}))
    if (__hasOwnProp.call(b, prop))
      __defNormalProp(a, prop, b[prop]);
  if (__getOwnPropSymbols)
    for (var prop of __getOwnPropSymbols(b)) {
      if (__propIsEnum.call(b, prop))
        __defNormalProp(a, prop, b[prop]);
    }
  return a;
};
var __spreadProps = (a, b) => __defProps(a, __getOwnPropDescs(b));
function useForm({
  initialValues,
  validationRules = {},
  errorMessages = {}
}) {
  const initialErrors = Object.keys(initialValues).reduce((acc, field) => {
    acc[field] = null;
    return acc;
  }, {});
  const [errors, setErrors] = useState(initialErrors);
  const [values, setValues] = useState(initialValues);
  const resetErrors = () => setErrors(initialErrors);
  const reset = () => {
    setValues(initialValues);
    resetErrors();
  };
  const validate = () => {
    let isValid = true;
    const validationErrors = Object.keys(values).reduce((acc, field) => {
      if (validationRules && typeof validationRules[field] === "function" && !validationRules[field](values[field], values)) {
        acc[field] = errorMessages[field] || true;
        isValid = false;
      } else {
        acc[field] = null;
      }
      return acc;
    }, {});
    setErrors(validationErrors);
    return isValid;
  };
  const validateField = (field) => setErrors((currentErrors) => __spreadProps(__spreadValues({}, currentErrors), {
    [field]: typeof validationRules[field] === "function" ? validationRules[field](values[field], values) ? null : errorMessages[field] || true : null
  }));
  const setFieldError = (field, error) => setErrors((currentErrors) => __spreadProps(__spreadValues({}, currentErrors), { [field]: error }));
  const setFieldValue = (field, value) => {
    setValues((currentValues) => __spreadProps(__spreadValues({}, currentValues), { [field]: value }));
    setFieldError(field, null);
  };
  const onSubmit = (handleSubmit) => (event) => {
    event && event.preventDefault();
    validate() && handleSubmit(values);
  };
  const getInputProps = (field, options) => ({
    [(options == null ? void 0 : options.type) === "checkbox" ? "checked" : "value"]: values[field],
    onChange: getInputOnChange((val) => setFieldValue(field, val)),
    error: errors[field] || void 0
  });
  return {
    values,
    errors,
    validate,
    reset,
    setErrors,
    setValues,
    setFieldValue,
    setFieldError,
    validateField,
    resetErrors,
    onSubmit,
    getInputProps
  };
}

export { useForm };
//# sourceMappingURL=use-form.js.map
