import { TestBed } from '@angular/core/testing';
import { AuthorizationRecoveryService } from './authorization-recovery.service';

describe('AuthorizationRecoveryService', () => {
  beforeEach(() => {
    sessionStorage.removeItem('piip-authorization-recovery');
    TestBed.configureTestingModule({ providers: [AuthorizationRecoveryService] });
  });

  afterEach(() => sessionStorage.removeItem('piip-authorization-recovery'));

  it('persiste un aviso fail-closed y bloquea un segundo retry mientras rehidrata', async () => {
    const service = TestBed.inject(AuthorizationRecoveryService);
    let release!: () => void;
    const pending = new Promise<void>((resolve) => { release = resolve; });
    await service.enter('Acceso actualizado. Rehidrata tu sesión.', () => pending);

    const first = service.retry();
    expect(service.retrying()).toBe(true);
    expect(service.retry()).resolves.toBe(false);
    expect(JSON.parse(sessionStorage.getItem('piip-authorization-recovery') ?? '{}')).toEqual({ message: 'Acceso actualizado. Rehidrata tu sesión.' });

    release();
    await expect(first).resolves.toBe(true);
    expect(service.active()).toBe(false);
    expect(sessionStorage.getItem('piip-authorization-recovery')).toBeNull();
  });

  it('conserva el aviso si la rehidratación falla y permite intentarlo después', async () => {
    const service = TestBed.inject(AuthorizationRecoveryService);
    const retry = vi.fn<() => Promise<void>>()
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce(undefined);
    await service.enter('No fue posible actualizar tu acceso.', retry);

    await expect(service.retry()).resolves.toBe(false);
    expect(service.active()).toBe(true);
    expect(service.message()).toContain('No fue posible rehidratar');
    await expect(service.retry()).resolves.toBe(true);
    expect(retry).toHaveBeenCalledTimes(2);
  });

  it('restaura el estado persistido al crear el servicio', () => {
    sessionStorage.setItem('piip-authorization-recovery', JSON.stringify({ message: 'Reintenta la autorización.' }));
    const service = TestBed.inject(AuthorizationRecoveryService);
    expect(service.active()).toBe(true);
    expect(service.message()).toBe('Reintenta la autorización.');
  });
});
