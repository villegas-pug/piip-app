import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { PendingChangesDialogComponent } from './pending-changes-dialog.component';

describe('PendingChangesDialogComponent', () => {
  it.each([
    ['Iniciativa', 'I-024-2026', 'iniciativa'],
    ['Proyecto', 'P-005-2026', 'proyecto'],
  ] as const)('muestra el contexto de %s y ofrece primero la acción segura', async (recordType, code, label) => {
    await TestBed.configureTestingModule({
      imports: [PendingChangesDialogComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { recordType, code } },
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(PendingChangesDialogComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const buttons = host.querySelectorAll<HTMLButtonElement>('button');

    expect(host.querySelector('#pending-changes-title')?.textContent).toContain('¿Descartar los cambios?');
    expect(host.querySelector('#pending-changes-description')?.textContent).toContain(`${label} ${code}`);
    expect(host.textContent).toContain('Esta acción no se puede deshacer.');
    expect(buttons[0]?.textContent).toContain('Seguir editando');
    expect(buttons[0]?.hasAttribute('cdkfocusinitial')).toBe(true);
    expect(buttons[1]?.textContent).toContain('Descartar y salir');
  });
});
