import { PiipActivityService } from './piip-activity.service';

describe('PiipActivityService', () => {
  let activity: PiipActivityService;

  beforeEach(() => {
    vi.useFakeTimers();
    activity = new PiipActivityService();
  });

  afterEach(() => vi.useRealTimers());

  it('keeps concurrent activity visible until the last operation finishes', () => {
    const finishFirst = activity.beginRequest();
    const finishSecond = activity.beginRequest();

    expect(activity.isBusy()).toBe(false);
    vi.advanceTimersByTime(120);
    expect(activity.isBusy()).toBe(true);

    finishFirst();
    expect(activity.isBusy()).toBe(true);
    finishSecond();
    expect(activity.isBusy()).toBe(false);
  });

  it('does not show progress when an operation completes before the delay', () => {
    const finish = activity.beginNavigation();
    finish();
    vi.advanceTimersByTime(120);

    expect(activity.isBusy()).toBe(false);
  });

  it('keeps nested blocking messages and always releases them', async () => {
    let releaseOuter!: () => void;
    const outerGate = new Promise<void>((resolve) => { releaseOuter = resolve; });
    const outer = activity.runBlocking('Operación externa', () => outerGate);

    expect(activity.isBlocking()).toBe(true);
    expect(activity.blockingMessage()).toBe('Operación externa');

    await expect(activity.runBlocking('Operación interna', async () => {
      expect(activity.blockingMessage()).toBe('Operación interna');
      throw new Error('falló');
    })).rejects.toThrow('falló');

    expect(activity.isBlocking()).toBe(true);
    expect(activity.blockingMessage()).toBe('Operación externa');
    releaseOuter();
    await outer;
    expect(activity.isBlocking()).toBe(false);
  });
});
