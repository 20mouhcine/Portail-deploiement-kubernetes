import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ActionHistory } from '../models/action-history.models';
import { ActionHistoryService } from './action-history.service';

describe('ActionHistoryService', () => {
  let service: ActionHistoryService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ActionHistoryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads visible history from the backend', () => {
    const entries: ActionHistory[] = [{
      id: 'history-1', action: 'CREATE', details: 'Projet créé',
      createdAt: '2026-08-12T10:00:00Z', ipAddress: '127.0.0.1',
      username: 'developer', targetType: 'PROJECT', targetName: 'Portal',
    }];

    service.load();
    expect(service.loading()).toBe(true);

    const request = http.expectOne('/api/action-history');
    expect(request.request.method).toBe('GET');
    request.flush(entries);

    expect(service.entries()).toEqual(entries);
    expect(service.loading()).toBe(false);
    expect(service.error()).toBeNull();
  });

  it('clears stale entries and exposes an error when loading fails', () => {
    service.load();
    http.expectOne('/api/action-history').flush([], { status: 500, statusText: 'Server Error' });

    expect(service.entries()).toEqual([]);
    expect(service.loading()).toBe(false);
    expect(service.error()).not.toBeNull();
  });
});
