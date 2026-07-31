import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PiipActivityService } from './piip-activity.service';
import { piipLoadingInterceptor } from './piip-loading.interceptor';

describe('piipLoadingInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let activity: PiipActivityService;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([piipLoadingInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    activity = TestBed.inject(PiipActivityService);
  });

  afterEach(() => {
    http.verify({ ignoreCancelled: true });
    vi.useRealTimers();
  });

  it('tracks API requests and releases progress after a response', () => {
    client.get('http://127.0.0.1:4001/api/v1/dashboard').subscribe();
    vi.advanceTimersByTime(120);
    expect(activity.isBusy()).toBe(true);

    http.expectOne('http://127.0.0.1:4001/api/v1/dashboard').flush({});
    expect(activity.isBusy()).toBe(false);
  });

  it('ignores requests outside the PIIP API', () => {
    client.get('https://example.test/asset.json').subscribe();
    vi.advanceTimersByTime(120);

    expect(activity.isBusy()).toBe(false);
    http.expectOne('https://example.test/asset.json').flush({});
  });

  it('releases progress after errors and cancellation', () => {
    client.get('http://127.0.0.1:4001/api/v1/failure').subscribe({ error: () => undefined });
    vi.advanceTimersByTime(120);
    expect(activity.isBusy()).toBe(true);
    http.expectOne('http://127.0.0.1:4001/api/v1/failure').flush({}, { status: 500, statusText: 'Error' });
    expect(activity.isBusy()).toBe(false);

    const subscription = client.get('http://127.0.0.1:4001/api/v1/cancelled').subscribe();
    vi.advanceTimersByTime(120);
    expect(activity.isBusy()).toBe(true);
    subscription.unsubscribe();
    expect(activity.isBusy()).toBe(false);
    expect(http.expectOne('http://127.0.0.1:4001/api/v1/cancelled').cancelled).toBe(true);
  });
});
