import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { TitleService } from '../../core/services/title.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink],
  styleUrl: './topbar.component.scss',
  templateUrl: './topbar.component.html'
})
export class TopbarComponent {
  private readonly router = inject(Router);

  /** O PDV tem a própria busca de produtos; a busca global e o atalho "Novo" ficam de fora. */
  readonly isPdv = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      startWith(null),
      map(() => this.router.url.split(/[?#]/)[0].startsWith('/pdv'))),
    { initialValue: false });

  constructor(public titleSvc: TitleService) {}
}
