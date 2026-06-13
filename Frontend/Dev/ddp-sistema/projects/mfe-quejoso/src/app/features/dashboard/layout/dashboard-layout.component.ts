import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { HeaderComponent } from '../../../components/header/header.component';
import { FooterComponent } from '../../../components/footer/footer.component';
import { NotificacionesService } from '../../../core/services/notificaciones.service';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    LucideAngularModule,
    HeaderComponent,
    FooterComponent,
  ],
  templateUrl: './dashboard-layout.component.html',
})
export class DashboardLayoutComponent implements OnInit {
  private notifService = inject(NotificacionesService);
  private cdr = inject(ChangeDetectorRef);

  notifNoLeidas = 0;
  currentYear = new Date().getFullYear();

  ngOnInit(): void {
    this.cargarNotificaciones();
  }

  private cargarNotificaciones(): void {
    this.notifService.listar().subscribe({
      next: (notifs) => {
        this.notifNoLeidas = notifs.filter((n) => !n.leida).length;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }
}
