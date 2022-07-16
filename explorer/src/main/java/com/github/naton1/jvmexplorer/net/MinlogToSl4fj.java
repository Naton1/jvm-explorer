package com.github.naton1.jvmexplorer.net;

import com.esotericsoftware.minlog.Log;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MinlogToSl4fj extends Log.Logger {

	public void log(int level, String category, String message, Throwable ex) {
		final String line = category != null ? (category + " - " + message) : message;

		switch (level) {
		case Log.LEVEL_ERROR:
			log.error(line, ex);
			break;
		case Log.LEVEL_WARN:
			log.warn(line, ex);
			break;
		case Log.LEVEL_INFO:
			log.info(line, ex);
			break;
		case Log.LEVEL_DEBUG:
			log.debug(line, ex);
			break;
		case Log.LEVEL_TRACE:
			log.trace(line, ex);
			break;
		}
	}

}
