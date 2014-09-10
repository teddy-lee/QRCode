package net.sourceforge.zbar;

import java.util.AbstractCollection;
import java.util.Iterator;

public class SymbolSet extends AbstractCollection<Symbol> {
	private long peer;
	
	static {
		System.loadLibrary("zbarjni");
		init();
	}
	
	private static native void init();
	private native void destroy(long paramLong);
	public native int size();

	private native long firstSymbol(long paramLong);

	SymbolSet(long paramLong) {
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

	public Iterator<Symbol> iterator() {
		long l = firstSymbol(this.peer);
		if (l == 0L) {
			return new SymbolIterator(null);
		}
		return new SymbolIterator(new Symbol(l));
	}

}