import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { HeaderComponent } from '../../../components/header/header.component';
import { FooterComponent } from '../../../components/footer/footer.component';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [RouterLink, LucideAngularModule, HeaderComponent, FooterComponent],
  templateUrl: './registro.component.html',
})
export class RegistroComponent {
  currentYear = new Date().getFullYear();
}
