import { canEditInitiative, canEditProject } from './portfolio-edit-permissions';

describe('portfolio edit permissions', () => {
  it('allows only a presented initiative without a derived project', () => {
    const detail = { initiative: { status: 'Presentado' }, derivedProject: undefined } as never;
    expect(canEditInitiative(detail, true)).toBe(true);
    expect(canEditInitiative(detail, false)).toBe(false);
    expect(canEditInitiative({ initiative: { status: 'Iniciativa aprobada' }, derivedProject: undefined } as never, true)).toBe(false);
  });

  it('allows projects only while in execution', () => {
    const detail = { project: { status: 'Proyecto en ejecución' } } as never;
    expect(canEditProject(detail, true)).toBe(true);
    expect(canEditProject({ project: { status: 'Finalizado' } } as never, true)).toBe(false);
  });
});
