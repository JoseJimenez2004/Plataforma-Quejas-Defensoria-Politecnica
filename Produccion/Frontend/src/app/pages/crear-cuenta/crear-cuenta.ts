import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Pantalla puramente informativa: solo ofrece las 2 rutas para llegar a tener cuenta
 * (registrar queja nueva o activar con folio ya existente) -- no tiene formulario propio. */
@Component({
  selector: 'app-crear-cuenta',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './crear-cuenta.html',
  styleUrl: './crear-cuenta.scss',
})
export class CrearCuenta {}
