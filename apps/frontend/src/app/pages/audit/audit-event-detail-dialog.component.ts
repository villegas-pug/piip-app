import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { PresentedAuditEvent } from './audit-event.presenter';

@Component({
  selector: 'app-audit-event-detail-dialog',
  imports: [MatDialogModule, MatIconModule],
  template: `
    <div class="audit-detail-dialog">
      <header><div><p>EVENTO DE AUDITORÍA</p><h2>{{ event.eventLabel }}</h2></div><button class="icon-action" type="button" mat-dialog-close aria-label="Cerrar detalle"><mat-icon>close</mat-icon></button></header>
      <mat-dialog-content>
        <dl><div><dt>Fecha y hora</dt><dd>{{ event.source.timestamp }}</dd></div><div><dt>Expediente</dt><dd>{{ event.source.recordCode ?? '—' }}</dd></div><div><dt>Usuario</dt><dd>{{ event.source.user }}@if (event.source.email) {<small>{{ event.source.email }}</small>}</dd></div><div><dt>Observación</dt><dd>{{ event.observation }}</dd></div>@if (event.source.documentName) {<div><dt>Documento</dt><dd>{{ event.source.documentName }}</dd></div>}</dl>
        @if (event.detailFields.length) {<section class="event-fields"><h3>Datos del evento</h3><dl>@for (field of event.detailFields; track field.label) {<div><dt>{{ field.label }}</dt><dd>{{ field.value }}</dd></div>}</dl></section>}
        <details><summary>Datos técnicos</summary><p>Subject: <code>{{ event.source.actorSubject ?? 'No registrado' }}</code></p><pre>{{ event.technicalDetail }}</pre></details>
      </mat-dialog-content>
      <mat-dialog-actions align="end"><button class="secondary-button" type="button" mat-dialog-close>Cerrar</button></mat-dialog-actions>
    </div>
  `,
  styles: [`
    .audit-detail-dialog { min-width: min(620px, calc(100vw - 32px)); } header { display:flex; align-items:flex-start; gap:16px; padding:22px 24px 17px; border-bottom:1px solid var(--piip-border); } header p { margin:0; color:var(--piip-green-700); font-size:11px; font-weight:700; letter-spacing:.09em; } h2 { margin:3px 0 0; color:var(--piip-green-950); font-family:var(--piip-display); font-size:25px; } .icon-action { margin-left:auto; } mat-dialog-content { display:grid; gap:20px; padding:22px 24px !important; } dl { display:grid; grid-template-columns:repeat(2, minmax(0, 1fr)); gap:16px; margin:0; } dt { margin-bottom:4px; color:#68756e; font-size:12px; } dd { margin:0; overflow-wrap:anywhere; } dd small { display:block; margin-top:3px; color:#68756e; } .event-fields { padding-top:18px; border-top:1px solid var(--piip-border); } .event-fields h3 { margin:0 0 13px; font-family:var(--piip-display); } details { padding-top:17px; border-top:1px solid var(--piip-border); } summary { cursor:pointer; color:var(--piip-green-800); font-weight:700; } details p { margin:14px 0 8px; } pre { max-height:220px; overflow:auto; margin:0; padding:12px; border-radius:5px; background:#f3f6f4; font-size:12px; white-space:pre-wrap; overflow-wrap:anywhere; } mat-dialog-actions { padding:12px 24px 20px; } @media (max-width:680px) { .audit-detail-dialog { min-width:0; width:calc(100vw - 32px); } header, mat-dialog-content { padding-inline:18px !important; } h2 { font-size:21px; } dl { grid-template-columns:1fr; gap:13px; } mat-dialog-actions { padding-inline:18px; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditEventDetailDialogComponent {
  readonly event = inject<PresentedAuditEvent>(MAT_DIALOG_DATA);
}
