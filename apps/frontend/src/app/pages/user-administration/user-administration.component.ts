import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { catchError, finalize, of } from 'rxjs';
import { resolveApiUrl } from '../../core/piip-http.repository';

interface UserScope {
  id: number; role: 'ADMINISTRADOR_PIIP' | 'CONSULTA_EXTERNA'; institution: string;
  executingUnit: string; active: boolean; version: number;
}

interface UserItem {
  id: number; subject: string; fullName: string; email: string; active: boolean; scopes: UserScope[];
}
interface InstitutionItem { id: number; code: string; name: string; }
interface ExecutingUnitItem { id: number; code: string; name: string; institutionId: number; }

@Component({
  selector: 'app-user-administration',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressSpinnerModule, MatSelectModule, MatSnackBarModule],
  templateUrl: './user-administration.component.html',
  styleUrl: './user-administration.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserAdministrationComponent {
  private readonly http = inject(HttpClient);
  private readonly formBuilder = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly apiUrl = resolveApiUrl();
  readonly users = signal<UserItem[]>([]);
  readonly institutions = signal<InstitutionItem[]>([]);
  readonly executingUnits = signal<ExecutingUnitItem[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly assignmentOpen = signal(false);
  readonly assigning = signal(false);
  readonly suspendingScopeId = signal<number | null>(null);
  readonly assignmentForm = this.formBuilder.nonNullable.group({
    userSubject: ['', Validators.required], role: ['CONSULTA_EXTERNA' as 'ADMINISTRADOR_PIIP' | 'CONSULTA_EXTERNA', Validators.required],
    institutionId: [0, Validators.min(1)], executingUnitId: [0],
  });

  constructor() { this.load(); }

  suspend(scope: UserScope): void {
    if (this.suspendingScopeId() !== null) return;
    this.suspendingScopeId.set(scope.id);
    this.http.delete(`${this.apiUrl}/admin/role-assignments/${scope.id}`, { params: { version: scope.version } })
      .pipe(finalize(() => this.suspendingScopeId.set(null)))
      .subscribe({ next: () => { this.snackBar.open('Asignación suspendida.', 'Cerrar', { duration: 2600 }); this.load(); },
        error: () => this.snackBar.open('No se pudo suspender la asignación.', 'Cerrar', { duration: 3200 }) });
  }

  assign(): void {
    if (this.assigning()) return;
    this.assignmentForm.markAllAsTouched();
    if (this.assignmentForm.invalid) return;
    const value = this.assignmentForm.getRawValue();
    this.assigning.set(true);
    this.http.post(`${this.apiUrl}/admin/role-assignments`, { ...value, executingUnitId: value.executingUnitId || null })
      .pipe(finalize(() => this.assigning.set(false)))
      .subscribe({ next: () => { this.assignmentOpen.set(false); this.snackBar.open('Rol asignado.', 'Cerrar', { duration: 2600 }); this.load(); },
        error: () => this.snackBar.open('No se pudo crear la asignación.', 'Cerrar', { duration: 3200 }) });
  }

  private load(): void {
    this.loading.set(true); this.error.set('');
    this.http.get<UserItem[]>(`${this.apiUrl}/admin/users`)
      .pipe(catchError(() => { this.error.set('No fue posible consultar la API de usuarios.'); return of([]); }), finalize(() => this.loading.set(false)))
      .subscribe((users) => this.users.set(users));
    this.http.get<InstitutionItem[]>(`${this.apiUrl}/institutions`).pipe(catchError(() => of([]))).subscribe((items) => this.institutions.set(items));
    this.http.get<ExecutingUnitItem[]>(`${this.apiUrl}/executing-units`).pipe(catchError(() => of([]))).subscribe((items) => this.executingUnits.set(items));
  }
}
