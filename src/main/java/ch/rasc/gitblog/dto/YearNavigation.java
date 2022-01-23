package ch.rasc.gitblog.dto;

public record YearNavigation(int year, boolean current)
		implements Comparable<YearNavigation> {

	@Override
	public int compareTo(YearNavigation o) {
		return this.year() - o.year();
	}

}
