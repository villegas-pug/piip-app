import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { ProjectDetailComponent } from './project-detail.component';

describe('ProjectDetailComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectDetailComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }, { provide: MatSnackBar, useValue: { open: vi.fn() } }, { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ code: 'P-005-2026' })), snapshot: { paramMap: convertToParamMap({ code: 'P-005-2026' }) } } }],
    }).compileComponents();
  });

  it('exposes only contextual project destinations', () => {
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    expect(fixture.componentInstance.transitionOptions()).toEqual(['Producto aprobado', 'Producto no aprobado', 'Suspendido', 'Cancelado']);
    expect(fixture.componentInstance.transitionOptions()).not.toContain('Iniciativa aprobada');
    expect(fixture.componentInstance.transitionOptions()).not.toContain('No Aplicable');
  });

  it('does not expose destinations for a terminal project', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.projects.update((projects) => projects.map((project) => project.code === 'P-005-2026' ? { ...project, status: 'Finalizado' } : project));
    repository.portfolioRecords.update((records) => records.map((record) => record.code === 'P-005-2026' ? { ...record, status: 'Finalizado', closingDate: '2026-08-18' } : record));
    const fixture = TestBed.createComponent(ProjectDetailComponent);
    expect(fixture.componentInstance.transitionOptions()).toEqual([]);
  });
});
