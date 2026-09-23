import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Remision } from './remision';

describe('Remision', () => {
  let component: Remision;
  let fixture: ComponentFixture<Remision>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Remision]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Remision);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
