import { canEditInitiative, canEditProject } from './portfolio-edit-permissions';

describe('portfolio edit permissions', () => {
  it('evalúa la edición únicamente por código de estado', () => {
    expect(canEditInitiative({ initiative: { status: { code: 'PRESENTED', name: 'Renombrado', active: true } }, derivedProject: undefined } as never, true)).toBe(true);
    expect(canEditProject({ project: { status: { code: 'PROJECT_IN_PROGRESS', name: 'Renombrado', active: true } } } as never, true)).toBe(true);
  });
  it('allows only a presented initiative without a derived project', () => {
    const detail = { initiative: { status: { code: 'PRESENTED' } }, derivedProject: undefined } as never;
    expect(canEditInitiative(detail, true)).toBe(true);
    expect(canEditInitiative(detail, false)).toBe(false);
    expect(canEditInitiative({ initiative: { status: { code: 'INITIATIVE_APPROVED' } }, derivedProject: undefined } as never, true)).toBe(false);
  });

  it('allows projects only while in execution', () => {
    const detail = { project: { status: { code: 'PROJECT_IN_PROGRESS' } } } as never;
    expect(canEditProject(detail, true)).toBe(true);
    expect(canEditProject({ project: { status: { code: 'FINISHED' } } } as never, true)).toBe(false);
  });
});
