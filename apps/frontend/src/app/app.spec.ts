import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { PiipMockRepository } from './core/piip-mock.repository';
import { PIIP_REPOSITORY } from './core/piip-repository.token';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), PiipMockRepository, { provide: PIIP_REPOSITORY, useExisting: PiipMockRepository }],
    }).compileComponents();
  });

  it('creates the root application with a router outlet', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.nativeElement.querySelector('router-outlet')).toBeTruthy();
  });

  it('shows an accessible blocking state while the repository initializes', () => {
    const repository = TestBed.inject(PiipMockRepository);
    repository.loading.set(true);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.loading-backdrop')?.textContent).toContain('Cargando PIIP');
    expect(fixture.nativeElement.getAttribute('aria-busy')).toBe('true');
  });
});
