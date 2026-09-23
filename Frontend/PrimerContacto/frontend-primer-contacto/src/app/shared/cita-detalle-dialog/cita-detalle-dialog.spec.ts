import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CitaDetalleDialog } from './cita-detalle-dialog';

describe('CitaDetalleDialog', () => {
  let component: CitaDetalleDialog;
  let fixture: ComponentFixture<CitaDetalleDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CitaDetalleDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CitaDetalleDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
