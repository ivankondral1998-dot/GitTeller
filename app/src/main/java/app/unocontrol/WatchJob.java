package app.unocontrol;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Runs the quiet GitHub read outside the broadcast time limit. */
public final class WatchJob extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try {
                Watch.run(getApplicationContext());
            } finally {
                jobFinished(params, false);
            }
        }, "gitteller-watch").start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        return false;
    }
}
