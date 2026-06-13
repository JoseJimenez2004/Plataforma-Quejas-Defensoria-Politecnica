import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-recuperar-password',
  standalone: true,
  templateUrl: './recuperar-password.html', 
  styleUrls: ['./recuperar-password.css']   
})
export class RecuperarPasswordComponent {

  constructor(private router: Router) {}

  volverLogin() {
    this.router.navigate(['/login']);
  }

  enviarEnlace() {
    alert("Enlace enviado al correo (Simulación)");
    
  }
}