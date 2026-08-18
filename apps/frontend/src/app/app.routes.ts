import { Routes } from '@angular/router';
import { administratorGuard } from './core/administrator.guard';
import { activeScopeAdministratorGuard } from './core/active-scope-administrator.guard';
import { authenticatedGuard } from './core/authenticated.guard';
import { piipReadyGuard } from './core/piip-ready.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'PIIP | Iniciar sesión',
    loadComponent: () =>
      import('./pages/login/login.component').then((module) => module.LoginComponent),
  },
  {
    path: '',
    loadComponent: () =>
      import('./layout/app-shell.component').then((module) => module.AppShellComponent),
    canActivate: [authenticatedGuard],
    canActivateChild: [piipReadyGuard],
    children: [
      {
        path: 'inicio',
        title: 'PIIP | Inicio',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.component').then(
            (module) => module.DashboardComponent,
          ),
      },
      {
        path: 'iniciativas',
        title: 'PIIP | Iniciativas',
        loadComponent: () =>
          import('./pages/initiatives/initiatives.component').then(
            (module) => module.InitiativesComponent,
          ),
      },
      {
        path: 'iniciativas/nueva',
        title: 'PIIP | Nueva iniciativa',
        canActivate: [activeScopeAdministratorGuard],
        loadComponent: () =>
          import('./pages/initiative-form/initiative-form.component').then(
            (module) => module.InitiativeFormComponent,
          ),
      },
      {
        path: 'iniciativas/:code/documentos',
        title: 'PIIP | Documentos',
        data: { recordType: 'Iniciativa' },
        loadComponent: () =>
          import('./pages/documents/documents.component').then(
            (module) => module.DocumentsComponent,
          ),
      },
      {
        path: 'iniciativas/:code',
        title: 'PIIP | Detalle de iniciativa',
        loadComponent: () =>
          import('./pages/initiative-detail/initiative-detail.component').then(
            (module) => module.InitiativeDetailComponent,
          ),
      },
      {
        path: 'documentos',
        title: 'PIIP | Documentos',
        loadComponent: () =>
          import('./pages/documents-inbox/documents-inbox.component').then(
            (module) => module.DocumentsInboxComponent,
          ),
      },
      {
        path: 'proyectos/nuevo/preexistente',
        title: 'PIIP | Proyecto preexistente',
        canActivate: [activeScopeAdministratorGuard],
        loadComponent: () =>
          import('./pages/preexisting-project-form/preexisting-project-form.component').then(
            (module) => module.PreexistingProjectFormComponent,
          ),
      },
      {
        path: 'proyectos/nuevo/derivado/:initiativeCode',
        title: 'PIIP | Proyecto derivado',
        canActivate: [activeScopeAdministratorGuard],
        loadComponent: () =>
          import('./pages/derived-project-form/derived-project-form.component').then(
            (module) => module.DerivedProjectFormComponent,
          ),
      },
      {
        path: 'proyectos/:code/documentos',
        title: 'PIIP | Documentos',
        data: { recordType: 'Proyecto' },
        loadComponent: () =>
          import('./pages/documents/documents.component').then(
            (module) => module.DocumentsComponent,
          ),
      },
      {
        path: 'proyectos/:code',
        title: 'PIIP | Detalle de proyecto',
        loadComponent: () =>
          import('./pages/project-detail/project-detail.component').then(
            (module) => module.ProjectDetailComponent,
          ),
      },
      {
        path: 'proyectos',
        title: 'PIIP | Proyectos',
        loadComponent: () =>
          import('./pages/projects/projects.component').then((module) => module.ProjectsComponent),
      },
      {
        path: 'auditoria',
        title: 'PIIP | Auditoría',
        canActivate: [administratorGuard],
        loadComponent: () =>
          import('./pages/audit/audit.component').then((module) => module.AuditComponent),
      },
      {
        path: 'administracion/usuarios',
        title: 'PIIP | Administración de usuarios',
        canActivate: [activeScopeAdministratorGuard],
        loadComponent: () =>
          import('./pages/user-administration/user-administration.component').then(
            (module) => module.UserAdministrationComponent,
          ),
      },
      { path: '', redirectTo: 'inicio', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: 'inicio' },
];
