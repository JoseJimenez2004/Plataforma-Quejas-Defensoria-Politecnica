import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ExpedienteService } from './expediente.service';

describe('ExpedienteService', () => {
  let service: ExpedienteService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ExpedienteService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
