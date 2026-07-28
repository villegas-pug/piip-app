import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PiipHttpRepository } from './piip-http.repository';

describe('PiipHttpRepository', () => {
  let http: HttpTestingController;
  let repository: PiipHttpRepository;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), PiipHttpRepository] });
    http = TestBed.inject(HttpTestingController);
    repository = TestBed.inject(PiipHttpRepository);
  });

  afterEach(() => http.verify());

  it('starts empty and never exposes mock portfolio data', () => {
    expect(repository.portfolioRecords()).toEqual([]);
    expect(repository.initiatives()).toEqual([]);
    expect(repository.projects()).toEqual([]);

    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { title: 'Acceso denegado', detail: 'Usuario sin asignación local activa', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );
  });

  it('surfaces backend ProblemDetail errors', async () => {
    const initialization = repository.initialize();
    http.expectOne('http://127.0.0.1:4001/api/v1/identity/me').flush(
      { title: 'Acceso denegado', detail: 'Usuario sin asignación local activa', status: 403 },
      { status: 403, statusText: 'Forbidden' },
    );

    await initialization;
    expect(repository.lastError()).toBe('Usuario sin asignación local activa');
    expect(repository.role()).toBe('Consulta externa');
  });
});
