package app.unocontrol;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Handler;
import android.os.Looper;

/** Runs the quiet GitHub read outside the broadcast time limit. */
public final class WatchJob extends JobService {
    private final Handler main = new Handler(Looper.getMainLooper());
    private Watch.RunToken active;
    private Thread worker;

    @Override public boolean onStartJob(JobParameters params) {
        stopActive();
        final Watch.RunToken run = new Watch.RunToken();
        active = run;
        worker = new Thread(() -> {
            try {
                Watch.run(getApplicationContext(), run);
            } finally {
                main.post(() -> {
                    if (active != run) return;
                    active = null;
                    worker = null;
                    jobFinished(params, false);
                });
            }
        }, "gitteller-watch");
        worker.start();
        return true;
    }

    private void stopActive() {
        Watch.RunToken run = active;
        Thread thread = worker;
        active = null;
        worker = null;
        if (run != null) run.cancel();
        if (thread != null) thread.interrupt();
    }

    @Override public boolean onStopJob(JobParameters params) {
        stopActive();
        return false;
    }

    @Override public void onDestroy() {
        stopActive();
        super.onDestroy();
    }
}
