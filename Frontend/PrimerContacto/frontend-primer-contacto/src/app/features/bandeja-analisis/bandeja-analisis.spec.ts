import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BandejaAnalisis } from './bandeja-analisis';

describe('BandejaAnalisis', () => {
  let component: BandejaAnalisis;
  let fixture: ComponentFixture<BandejaAnalisis>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BandejaAnalisis]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BandejaAnalisis);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
