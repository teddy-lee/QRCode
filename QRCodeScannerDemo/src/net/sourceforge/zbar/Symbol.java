package net.sourceforge.zbar;

public class Symbol {
	public static final int NONE = 0;
	public static final int PARTIAL = 1;
	public static final int EAN8 = 8;
	public static final int UPCE = 9;
	public static final int ISBN10 = 10;
	public static final int UPCA = 12;
	public static final int EAN13 = 13;
	public static final int ISBN13 = 14;
	public static final int I25 = 25;
	public static final int DATABAR = 34;
	public static final int DATABAR_EXP = 35;
	public static final int CODABAR = 38;
	public static final int CODE39 = 39;
	public static final int PDF417 = 57;
	public static final int QRCODE = 64;
	public static final int CODE93 = 93;
	public static final int CODE128 = 128;
	private long peer;
	private int type;
	
	static {
		System.loadLibrary("zbarjni");
		init();
	}
	
	private static native void init();
	
	private native int getType(long paramLong);

	public native int getConfigMask();

	public native int getModifierMask();

	public native String getData();

	public native byte[] getDataBytes();

	public native int getQuality();

	public native int getCount();
	
	private native void destroy(long paramLong);

	private native int getLocationSize(long paramLong);

	private native int getLocationX(long paramLong, int paramInt);

	private native int getLocationY(long paramLong, int paramInt);
	
	public native int getOrientation();
	
	private native long getComponents(long paramLong);

	native long next();

	Symbol(long paramLong) {
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

	public int getType() {
		if (this.type == 0)
			this.type = getType(this.peer);
		return this.type;
	}

	public int[] getBounds() {
		int i = getLocationSize(this.peer);
		if (i <= 0)
			return null;
		int[] arrayOfInt = new int[4];
		int j = 2147483647;
		int k = -2147483648;
		int m = 2147483647;
		int n = -2147483648;
		for (int i1 = 0; i1 < i; i1++) {
			int i2 = getLocationX(this.peer, i1);
			if (j > i2)
				j = i2;
			if (k < i2)
				k = i2;
			int i3 = getLocationY(this.peer, i1);
			if (m > i3)
				m = i3;
			if (n < i3)
				n = i3;
		}
		arrayOfInt[0] = j;
		arrayOfInt[1] = m;
		arrayOfInt[2] = (k - j);
		arrayOfInt[3] = (n - m);
		return arrayOfInt;
	}

	public int[] getLocationPoint(int paramInt) {
		int[] arrayOfInt = new int[2];
		arrayOfInt[0] = getLocationX(this.peer, paramInt);
		arrayOfInt[1] = getLocationY(this.peer, paramInt);
		return arrayOfInt;
	}

	

	public SymbolSet getComponents() {
		return new SymbolSet(getComponents(this.peer));
	}

}