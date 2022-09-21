import {
  CreateHiringCardMutationVariables,
  UpdateHiringCardMutationVariables,
} from '@juxt-home/site';
import { ModalStateProps, Option } from '@juxt-home/ui-common';

export type AddHiringCardInput = CreateHiringCardMutationVariables & {
  project: Option;
  potentialClients?: Option[] | null;
  owners?: Option[] | null;
  workflowState: Option;
};

export type AddHiringCardModalProps = ModalStateProps;
export type EditCardModalProps = ModalStateProps;
export type UpdateHiringCardInput = UpdateHiringCardMutationVariables & {
  project: Option;
  owners?: Option[] | null;
  workflowState: Option;
  potentialClients?: Option[] | null;
  rejectionReasons?: Option[] | null;
};
