package com.killercats.playletmobile;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Runs work off the main thread and hands the result back on it. */
public final class Async {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Task<T> {
        T run() throws Exception;
    }

    public interface Done<T> {
        void ok(T result);

        void error(Exception e);
    }

    private Async() {}

    public static <T> void run(final Task<T> task, final Done<T> done) {
        POOL.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final T result = task.run();
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            done.ok(result);
                        }
                    });
                } catch (final Exception e) {
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            done.error(e);
                        }
                    });
                }
            }
        });
    }
}
