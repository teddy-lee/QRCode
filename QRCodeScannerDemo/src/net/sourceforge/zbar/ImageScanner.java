package net.sourceforge.zbar;

public class ImageScanner {
	
	static {
		System.loadLibrary("zbarjni");
		init();
	}
	
	private long peer = create();

	private static native void init();

	private native long create();
	
	private native void destroy(long paramLong);

	public native void setConfig(int paramInt1, int paramInt2, int paramInt3)
			throws IllegalArgumentException;

	public native void parseConfig(String paramString);

	public native void enableCache(boolean paramBoolean);
	
	private native long getResults(long paramLong);

	public native int scanImage(Image paramImage);

	protected void finalize() {
		destroy();
	}

	public synchronized void destroy() {
		if (this.peer != 0L) {
			destroy(this.peer);
			this.peer = 0L;
		}
	}

	public SymbolSet getResults() {
		return new SymbolSet(getResults(this.peer));
	}
	
}