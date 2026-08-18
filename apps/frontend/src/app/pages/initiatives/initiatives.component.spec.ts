import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PiipMockRepository } from '../../core/piip-mock.repository';
import { PIIP_REPOSITORY } from '../../core/piip-repository.token';
import { InitiativesComponent } from './initiatives.component';

describe('InitiativesComponent pagination', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InitiativesComponent],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }],
    }).compileComponents();
  });

  it('shows five initiatives and returns to the first page when filtering', () => {
    const repository = TestBed.inject(PiipMockRepository);
    const template = repository.initiatives()[0];
    repository.initiatives.set(Array.from({ length: 6 }, (_, index) => ({ ...template, code: `I-10${index}-2026`, name: `Iniciativa ${index + 1}` })));
    const fixture = TestBed.createComponent(InitiativesComponent);
    const component = fixture.componentInstance;

    expect(component.pagedInitiatives()).toHaveLength(5);
    component.pageIndex.set(1);
    expect(component.pagedInitiatives()).toHaveLength(1);

    component.filters.patchValue({ search: 'I-100-2026' });
    expect(component.currentPage()).toBe(0);
    expect(component.pagedInitiatives()).toHaveLength(1);
  });

  it('shows only initiative states in the status filter and keeps the list consultive', () => {
    const fixture = TestBed.createComponent(InitiativesComponent);
    fixture.detectChanges();
    const nativeElement = fixture.nativeElement as HTMLElement;
    const options = Array.from(nativeElement.querySelectorAll('select[formcontrolname="status"] option'))
      .map((option) => option.textContent?.trim());

    expect(options).toEqual(expect.arrayContaining(['Presentado', 'Iniciativa aprobada', 'Iniciativa archivada', 'No Admisible']));
    expect(options).not.toEqual(expect.arrayContaining(['Proyecto en ejecución', 'Producto aprobado', 'Suspendido', 'Finalizado', 'No Aplicable']));
    expect(nativeElement.textContent).not.toContain('Cambiar estado');
  });
});
