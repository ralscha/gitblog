package ch.rasc.gitblog.dto;

public class YearNavigation {
	private final int year;

	private final boolean current;

	public YearNavigation(int year, boolean current) {
		this.year = year;
		this.current = current;
	}

	public int getYear() {
		return this.year;
	}

	public boolean isCurrent() {
		return this.current;
	}

}
