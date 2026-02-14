package ru.practicum.moviehub.model;

public class Movie {
	protected String title;
	protected int year;
	protected int id;

	public void setId(int id) {
		this.id = id;
	}

	public Movie(String title, int year, int id) {
		this.title = title;
		this.year = year;
		this.id = id;
	}

	public int getId() {
		return id;
	}


	public int getYear() {
		return year;
	}

	public String getTitle() {
		return title;
	}
}