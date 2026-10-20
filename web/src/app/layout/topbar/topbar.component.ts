import { Component } from '@angular/core';
import { TitleService } from '../../core/services/title.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  styleUrl: './topbar.component.scss',
  templateUrl: './topbar.component.html'
})
export class TopbarComponent {
  constructor(public titleSvc: TitleService) {}
}
