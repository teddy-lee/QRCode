package net.sourceforge.zbar;

public class Image {
	private long peer;
	private Object data;
	
	static {
		System.loadLibrary("zbarjni");
		init();
	}

	private static native void init();
	private native long create();
	private native void destroy(long paramLong);
	private native long convert(long paramLong, String paramString);

	public native String getFormat();

	public native void setFormat(String paramString);

	public native int getSequence();

	public native void setSequence(int paramInt);

	public native int getWidth();

	public native int getHeight();

	public native int[] getSize();

	public native void setSize(int paramInt1, int paramInt2);

	public native void setSize(int[] paramArrayOfInt);

	public native int[] getCrop();

	public native void setCrop(int paramInt1, int paramInt2, int paramInt3,
			int paramInt4);

	public native void setCrop(int[] paramArrayOfInt);

	public native byte[] getData();

	public native void setData(byte[] paramArrayOfByte);

	public native void setData(int[] paramArrayOfInt);
	
	private native long getSymbols(long paramLong);

	public Image() {
		this.peer = create();
	}

	public Image(int paramInt1, int paramInt2) {
		this();
		setSize(paramInt1, paramInt2);
	}

	public Image(int paramInt1, int paramInt2, String paramString) {
		this();
		setSize(paramInt1, paramInt2);
		setFormat(paramString);
	}

	public Image(String paramString) {
		this();
		setFormat(paramString);
	}

	Image(long paramLong) {
		this.peer = paramLong;
	}

	

	protected void finalize() {
		destroy();
	}

	public synchronized void destroy() {
		if (this.peer != 0L) {
			destroy(this.peer);
			this.peer = 0L;
		}
	}

	public Image convert(String paramString) {
		long l = convert(this.peer, paramString);
		if (l == 0L)
			return null;
		return new Image(l);
	}

	public SymbolSet getSymbols() {
		return new SymbolSet(getSymbols(this.peer));
	}

}