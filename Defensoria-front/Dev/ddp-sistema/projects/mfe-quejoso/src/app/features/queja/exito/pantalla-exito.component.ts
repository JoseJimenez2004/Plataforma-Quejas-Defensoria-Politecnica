import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { HeaderComponent } from '../../../components/header/header.component';
import { FooterComponent } from '../../../components/footer/footer.component';

@Component({
  selector: 'app-pantalla-exito',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule, HeaderComponent, FooterComponent],
  templateUrl: './pantalla-exito.component.html',
})
export class PantallaExitoComponent implements OnInit {
  private router = inject(Router);

  folio = '';
  correo = '';
  currentYear = new Date().getFullYear();

  ngOnInit(): void {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state ?? history.state;
    this.folio = state?.['folio'] ?? '';
    this.correo = state?.['correo'] ?? '';

    if (!this.folio) {
      this.router.navigate(['/']);
    }
  }

  crearCuenta(): void {
    this.router.navigate(['/queja/configurar-cuenta'], {
      state: { folio: this.folio, correo: this.correo },
    });
  }
}
