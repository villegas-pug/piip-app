import type { InitiativeDetail, ProjectDetail } from './piip.models';

/** Decisión pura de visibilidad; el backend conserva la autorización efectiva. */
export function canEditInitiative(detail: InitiativeDetail | undefined, administersExecutingUnit: boolean): boolean {
  return Boolean(detail && administersExecutingUnit && detail.initiative.status === 'Presentado' && !detail.derivedProject);
}

export function canEditProject(detail: ProjectDetail | undefined, administersExecutingUnit: boolean): boolean {
  return Boolean(detail && administersExecutingUnit && detail.project.status === 'Proyecto en ejecución');
}
