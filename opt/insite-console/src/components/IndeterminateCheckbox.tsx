/* eslint-disable @typescript-eslint/ban-ts-comment */
/* eslint-disable no-param-reassign */
/* eslint-disable react/require-default-props */
import {Checkbox} from '@mui/material';
import React, {useEffect, forwardRef} from 'react';

interface Props {
  indeterminate?: boolean;
  name: string;
}

const useCombinedRefs = (
  ...refs: React.Ref<HTMLInputElement>[]
): React.MutableRefObject<HTMLInputElement | null | undefined> => {
  const targetRef = React.useRef<HTMLInputElement | null | undefined>();

  React.useEffect(() => {
    refs.forEach((ref) => {
      if (!ref) return;

      if (targetRef?.current && typeof ref === 'function') {
        ref(targetRef.current);
      } else {
        // @ts-ignore
        ref.current = targetRef.current;
      }
    });
  }, [refs]);

  return targetRef;
};

const IndeterminateCheckbox = forwardRef<HTMLInputElement, Props>(
  ({indeterminate, ...rest}, ref: React.Ref<HTMLInputElement>) => {
    const defaultRef = React.useRef(null);
    const combinedRef = useCombinedRefs(ref, defaultRef);

    useEffect(() => {
      if (combinedRef?.current) {
        combinedRef.current.indeterminate = indeterminate ?? false;
      }
    }, [combinedRef, indeterminate]);
    // @ts-expect-error
    return <Checkbox ref={combinedRef} {...rest} />;
  }
);

export default IndeterminateCheckbox;
