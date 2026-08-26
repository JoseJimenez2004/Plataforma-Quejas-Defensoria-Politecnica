import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Dictamen } from './dictamen';

describe('Dictamen', () => {
  let component: Dictamen;
  let fixture: ComponentFixture<Dictamen>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dictamen]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Dictamen);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
