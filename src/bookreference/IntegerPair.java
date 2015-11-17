package bookreference;

public class IntegerPair implements Comparable<IntegerPair> {
	Integer _first, _second;

	public IntegerPair(Integer f, Integer s) {
		_first = f;
		_second = s;
	}

	public int compareTo(IntegerPair o) {
		if (!this.first().equals(((IntegerPair) o).first()))
			return this.first() - ((IntegerPair) o).first();
		else
			return this.second() - ((IntegerPair) o).second();
	}

	public Integer first() {
		return _first;
	}

	public Integer second() {
		return _second;
	}

	@Override
	public String toString() {
		return _first + " " + _second;
	}
}
