import { Routes } from '@angular/router';
import { MainLayout } from './layouts/main-layout/main-layout';

export const routes: Routes = [
  {
    path: '',
    component: MainLayout,
    children: [
      {
        path: '',
        title: 'Trato · Compra y vende entre personas',
        loadComponent: () => import('./features/home/home-page').then((m) => m.HomePage),
      },
      {
        path: '**',
        title: 'Página no encontrada · Trato',
        loadComponent: () =>
          import('./features/not-found/not-found-page').then((m) => m.NotFoundPage),
      },
    ],
  },
];
