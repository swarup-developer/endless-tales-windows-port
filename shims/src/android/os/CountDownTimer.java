package android.os;

/** Desktop CountDownTimer driven by {@link MainThread}. */
public abstract class CountDownTimer {
    private final long millisInFuture, interval;
    private long stopAt;
    private volatile boolean cancelled;
    private int generation;

    public CountDownTimer(long millisInFuture, long countDownInterval) {
        this.millisInFuture = millisInFuture;
        this.interval = Math.max(1, countDownInterval);
    }

    public abstract void onTick(long millisUntilFinished);
    public abstract void onFinish();

    public final synchronized void cancel() { cancelled = true; generation++; }

    public final synchronized CountDownTimer start() {
        cancelled = false;
        int gen = ++generation;
        if (millisInFuture <= 0) { MainThread.post(this::onFinish, 0); return this; }
        stopAt = SystemClock.elapsedRealtime() + millisInFuture;
        tick(gen);
        return this;
    }

    private void tick(int gen) {
        synchronized (this) { if (cancelled || gen != generation) return; }
        long left = stopAt - SystemClock.elapsedRealtime();
        if (left <= 0) { onFinish(); return; }
        onTick(left);
        MainThread.post(() -> tick(gen), Math.min(interval, left));
    }
}
