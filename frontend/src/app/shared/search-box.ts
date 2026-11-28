import { Component, input } from '@angular/core';
import { Router } from '@angular/router';
import { inject } from '@angular/core';

@Component({
  selector: 'app-search-box',
  template: `
    <form role="search" aria-label="Buscar publicaciones" (submit)="submit($event, field.value)">
      <label class="visually-hidden" [for]="id()">Buscar publicaciones</label>
      <input #field [id]="id()" type="search" name="q" placeholder="¿Qué estás buscando?" />
      <button type="submit">Buscar</button>
    </form>
  `,
  styles: `
    form {
      display: flex;
      gap: 0.5rem;
      width: min(100%, 34rem);
    }
    input {
      min-width: 0;
      flex: 1;
      padding: 0.7rem 0.85rem;
      border: 1px solid var(--color-border);
      border-radius: 8px;
      font: inherit;
    }
    button {
      padding: 0.7rem 1rem;
      border: 0;
      border-radius: 8px;
      background: var(--color-accent);
      color: var(--color-on-accent);
      font: inherit;
      font-weight: 700;
      cursor: pointer;
    }
    .visually-hidden {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }
  `,
})
export class SearchBox {
  readonly id = input.required<string>();
  private readonly router = inject(Router);

  submit(event: Event, value: string): void {
    event.preventDefault();
    void this.router.navigate(['/buscar'], { queryParams: { q: value.trim() || null } });
  }
}
