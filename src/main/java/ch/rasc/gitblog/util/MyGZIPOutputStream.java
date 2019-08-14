package ch.rasc.gitblog.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class MyGZIPOutputStream extends GZIPOutputStream {

	public MyGZIPOutputStream(OutputStream out) throws IOException {
		super(out);
	}

	public void setLevel(int level) {
		this.def.setLevel(level);
	}
}