import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './registro.component.html',
})
export class RegistroComponent {
  currentYear = new Date().getFullYear();
}
