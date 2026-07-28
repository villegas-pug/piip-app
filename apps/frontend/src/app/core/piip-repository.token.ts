import { InjectionToken } from '@angular/core';
import { PiipRepository } from './piip.repository';

export const PIIP_REPOSITORY = new InjectionToken<PiipRepository>('PIIP_REPOSITORY');
