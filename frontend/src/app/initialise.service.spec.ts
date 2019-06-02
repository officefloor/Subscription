import { TestBed } from '@angular/core/testing';

import { InitialiseService } from './initialise.service';

describe('InitialiseService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: InitialiseService = TestBed.get(InitialiseService);
    expect(service).toBeTruthy();
  });
});
