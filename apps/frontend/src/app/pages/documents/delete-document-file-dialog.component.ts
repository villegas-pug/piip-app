import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface DeleteDocumentFileDialogData {
  readonly documentName: string;
  readonly filename: string;
  readonly latestVersion: number;
}

@Component({
  selector: 'app-delete-document-file-dialog',
  imports: [MatDialogModule, MatIconModule],
  template: `
    <div class="delete-file-dialog">
      <header>
        <span class="warning-icon" aria-hidden="true"><mat-icon>delete_outline</mat-icon></span>
        <div><p>ELIMINACIÓN LÓGICA</p><h2>¿Eliminar este archivo?</h2></div>
      </header>
      <mat-dialog-content>
        <p>El archivo dejará de estar disponible en el expediente. Los demás archivos del mismo tipo no se modificarán.</p>
        <dl>
          <div><dt>Tipo documental</dt><dd>{{ data.documentName }}</dd></div>
          <div><dt>Archivo</dt><dd>{{ data.filename }}</dd></div>
          <div><dt>Versión vigente</dt><dd>{{ data.latestVersion }}</dd></div>
        </dl>
        <p class="audit-note"><mat-icon aria-hidden="true">history</mat-icon>La operación quedará registrada en la auditoría del expediente.</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button class="secondary-button" type="button" mat-dialog-close>Cancelar</button>
        <button class="danger-button" type="button" [mat-dialog-close]="true"><mat-icon>delete</mat-icon>Eliminar archivo</button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .delete-file-dialog { min-width: min(560px, calc(100vw - 32px)); }
    header { display:flex; align-items:flex-start; gap:14px; padding:24px 24px 18px; border-bottom:1px solid var(--piip-border); }
    .warning-icon { display:grid; place-items:center; flex:0 0 42px; width:42px; height:42px; border-radius:50%; color:#a12d2d; background:#fff1f1; }
    .warning-icon mat-icon { width:24px; height:24px; font-size:24px; }
    header p { margin:0; color:#9a3939; font-size:11px; font-weight:700; letter-spacing:.09em; }
    h2 { margin:3px 0 0; color:var(--piip-green-950); font-family:var(--piip-display); font-size:25px; }
    mat-dialog-content { display:grid; gap:18px; padding:22px 24px !important; color:#445047; line-height:1.45; }
    mat-dialog-content > p { margin:0; }
    dl { display:grid; gap:12px; margin:0; padding:15px; border:1px solid #dbe4de; border-radius:6px; background:#f8fbf9; }
    dt { margin-bottom:3px; color:#68756e; font-size:12px; }
    dd { margin:0; color:var(--piip-green-950); font-weight:600; overflow-wrap:anywhere; }
    .audit-note { display:flex; align-items:flex-start; gap:8px; color:#295943; font-size:13px; }
    .audit-note mat-icon { flex:0 0 20px; width:20px; height:20px; font-size:20px; }
    mat-dialog-actions { gap:10px; padding:12px 24px 22px; }
    .danger-button { display:inline-flex; align-items:center; justify-content:center; gap:7px; min-height:42px; padding:0 14px; border:1px solid #a12d2d; border-radius:5px; color:#fff; background:#a12d2d; font-weight:700; cursor:pointer; }
    .danger-button:hover, .danger-button:focus-visible { border-color:#7f2020; background:#7f2020; }
    .danger-button:focus-visible { outline:2px solid #d87878; outline-offset:2px; }
    .danger-button mat-icon { width:19px; height:19px; font-size:19px; }
    @media (max-width:620px) { .delete-file-dialog { min-width:0; width:calc(100vw - 32px); } header, mat-dialog-content { padding-inline:18px !important; } h2 { font-size:21px; } mat-dialog-actions { align-items:stretch; flex-direction:column-reverse; padding-inline:18px; } mat-dialog-actions button { width:100%; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeleteDocumentFileDialogComponent {
  readonly data = inject<DeleteDocumentFileDialogData>(MAT_DIALOG_DATA);
}
