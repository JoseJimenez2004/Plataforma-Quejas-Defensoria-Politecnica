import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DictamenService } from './dictamen.service';

describe('DictamenService', () => {
  let service: DictamenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(DictamenService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
