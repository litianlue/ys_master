package com.yeespec.microscope.master.service.system;

import android.util.Log;

public class AliveCheckThread extends Thread {

	private final static String TAG = AliveCheckThread.class.getSimpleName();

	private final Process proc;
	private final ShutdownThread shutdownThread;

	public AliveCheckThread(Process proc, ShutdownThread shutdownThread) {
		this.proc = proc;
		this.shutdownThread = shutdownThread;
	}

	@Override
	public void run() {
		try {
			sleep(15000); // wait 15s, because Superuser also has 10s timeout
		} catch (InterruptedException e) {
			e.printStackTrace();
			Log.i(TAG, "Interrupted.");
			return;
		}
		Log.w(TAG, "Still alive after 15 sec...");
		Utils.dumpProcessOutput(proc);
		proc.destroy();
		shutdownThread.interrupt();
		Log.w(TAG, "Interrupted and destroyed.");

		Utils.killMyProcess();
	}
}