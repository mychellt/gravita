import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TitleService } from '../../core/services/title.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink],
  styleUrl: './topbar.component.scss',
  templateUrl: './topbar.component.html'
})
export class TopbarComponent {
  constructor(public titleSvc: TitleService) {}
}
