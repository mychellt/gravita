import { Injectable, signal } from '@angular/core';

/**
 * Avisa as telas do estoque que o servidor mudou (ajuste, contagem aprovada, reserva liberada): quem está montado
 * observa `version` e busca de novo, em vez de manter uma cópia local do saldo.
 */
@Injectable({ providedIn: 'root' })
export class InventoryChangesService {
  private readonly counter = signal(0);
  readonly version = this.counter.asReadonly();

  stockChanged(): void {
    this.counter.update(n => n + 1);
  }
}
