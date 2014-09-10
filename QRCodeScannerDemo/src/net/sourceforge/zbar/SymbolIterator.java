package net.sourceforge.zbar;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SymbolIterator implements Iterator<Symbol> {
	private Symbol current;

	SymbolIterator(Symbol paramSymbol) {
		this.current = paramSymbol;
	}

	public boolean hasNext() {
		return this.current != null;
	}

	public Symbol next() {
		if (this.current == null) {
			throw new NoSuchElementException(
					"access past end of SymbolIterator");
		}
		
		Symbol localSymbol = this.current;
		long l = this.current.next();
		if (l != 0L) {
			this.current = new Symbol(l);
		} else {
			this.current = null;
		}
		return localSymbol;
	}

	public void remove() {
		throw new UnsupportedOperationException("SymbolIterator is immutable");
	}
}