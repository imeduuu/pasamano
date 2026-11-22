import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiClient } from '../../core/http/api-client';

interface Category {
  id: string;
  name: string;
  slug: string;
  sortOrder: number;
  children: Category[];
}

/** Panel mínimo para CU-18, RF-CAT-02 y RF-ADM-05. */
@Component({
  selector: 'app-category-admin-page',
  imports: [ReactiveFormsModule],
  templateUrl: './category-admin-page.html',
})
export class CategoryAdminPage {
  private readonly api = inject(ApiClient);
  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly categories = signal<Category[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly createForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    parentId: [''],
  });
  protected readonly edits = new Map<string, { name: string; sortOrder: number; reason: string }>();
  protected readonly deactivationReasons = new Map<string, string>();

  constructor() {
    this.load();
  }

  protected edit(category: Category) {
    return (
      this.edits.get(category.id) ?? {
        name: category.name,
        sortOrder: category.sortOrder,
        reason: '',
      }
    );
  }

  protected setEdit(category: Category, key: 'name' | 'sortOrder' | 'reason', value: string) {
    const edit = { ...this.edit(category), [key]: key === 'sortOrder' ? Number(value) : value };
    this.edits.set(category.id, edit);
  }

  protected reason(id: string): string {
    return this.deactivationReasons.get(id) ?? '';
  }
  protected setReason(id: string, value: string) {
    this.deactivationReasons.set(id, value);
  }

  protected create(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    const parentId = this.createForm.controls.parentId.value || null;
    const siblings = parentId
      ? (this.categories().find((item) => item.id === parentId)?.children ?? [])
      : this.categories();
    this.api
      .post<Category>('/admin/categories', {
        name: this.createForm.controls.name.value.trim(),
        sortOrder: siblings.length + 1,
        parentId,
        reason: null,
      })
      .subscribe({
        next: () => {
          this.createForm.reset();
          this.load();
        },
        error: () => this.error.set('No se pudo crear la categoría.'),
      });
  }

  protected save(category: Category): void {
    const edit = this.edit(category);
    this.api
      .patch<Category>(`/admin/categories/${category.id}`, {
        name: edit.name.trim(),
        sortOrder: edit.sortOrder,
        reason: edit.reason || null,
      })
      .subscribe({
        next: () => this.load(),
        error: () => this.error.set('No se pudo guardar la categoría.'),
      });
  }

  protected deactivate(category: Category): void {
    const reason = this.reason(category.id).trim();
    if (!reason) {
      this.error.set('Indica el motivo para desactivar la categoría.');
      return;
    }
    this.api.post<void>(`/admin/categories/${category.id}/deactivate`, { reason }).subscribe({
      next: () => this.load(),
      error: () => this.error.set('No se pudo desactivar la categoría.'),
    });
  }

  private load(): void {
    this.api.get<Category[]>('/categories').subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.error.set(null);
      },
      error: () => this.error.set('No se pudieron cargar las categorías.'),
    });
  }
}
