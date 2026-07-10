import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { RemisionService } from './remision.service';

describe('RemisionService', () => {
  let service: RemisionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(RemisionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
