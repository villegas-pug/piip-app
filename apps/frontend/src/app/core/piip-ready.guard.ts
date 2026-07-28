import { inject } from '@angular/core';
import { CanActivateChildFn } from '@angular/router';
import { PIIP_REPOSITORY } from './piip-repository.token';

export const piipReadyGuard: CanActivateChildFn = async () => {
  await Promise.resolve(inject(PIIP_REPOSITORY).initialize());
  return true;
};
