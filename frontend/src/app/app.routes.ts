import { Routes } from '@angular/router';
import { adminGuard, authGuard, guestGuard } from './core/auth/auth.guards';
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
        path: 'admin/categorias',
        title: 'Administrar categorías · Trato',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/category-admin-page').then((m) => m.CategoryAdminPage),
      },
      {
        path: 'registro',
        title: 'Crear cuenta · Trato',
        canActivate: [guestGuard],
        loadComponent: () => import('./features/auth/register-page').then((m) => m.RegisterPage),
      },
      {
        path: 'verificar-correo',
        title: 'Verificar correo · Trato',
        loadComponent: () =>
          import('./features/auth/verify-email-page').then((m) => m.VerifyEmailPage),
      },
      {
        path: 'ingresar',
        title: 'Ingresar · Trato',
        canActivate: [guestGuard],
        loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
      },
      {
        path: 'recuperar-contrasena',
        title: 'Recuperar contraseña · Trato',
        loadComponent: () =>
          import('./features/auth/forgot-password-page').then((m) => m.ForgotPasswordPage),
      },
      {
        path: 'restablecer-contrasena',
        title: 'Nueva contraseña · Trato',
        loadComponent: () =>
          import('./features/auth/reset-password-page').then((m) => m.ResetPasswordPage),
      },
      {
        path: 'perfil',
        title: 'Mi perfil · Trato',
        canActivate: [authGuard],
        loadComponent: () => import('./features/profile/profile-page').then((m) => m.ProfilePage),
      },
      {
        path: 'activar-perfil-vendedor',
        title: 'Activar perfil de vendedor · Trato',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/seller/activate-seller-page').then((m) => m.ActivateSellerPage),
      },
      {
        path: 'publicar-producto',
        title: 'Publicar producto · Trato',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/seller/publish-product-page').then((m) => m.PublishProductPage),
      },
      {
        path: 'mis-publicaciones',
        title: 'Mis publicaciones · Trato',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/seller/my-listings-page').then((m) => m.MyListingsPage),
      },
      {
        path: 'terminos',
        title: 'Términos y condiciones · Trato',
        loadComponent: () => import('./features/legal/legal-pages').then((m) => m.TermsPage),
      },
      {
        path: 'privacidad',
        title: 'Política de privacidad · Trato',
        loadComponent: () => import('./features/legal/legal-pages').then((m) => m.PrivacyPage),
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
