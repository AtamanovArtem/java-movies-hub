package ru.practicum.moviehub.store;

import org.junit.jupiter.api.BeforeEach;
import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class MoviesStore {

	public List<Movie> movies;

	public MoviesStore() {
		movies = new ArrayList<>();
	}

	// Добавление фильма
	public void addMovie(Movie movie) {
		movies.add(movie);
	}

	// Поиск фильма по идентификатору
	public Movie findMovieById(int id) {
		for (Movie movie : movies) {
			if (movie.getId() == id) {
				return movie;
			}
		}
		return null;
	}


	public boolean removeMovieById(int id) {
		Movie movieToRemove = findMovieById(id);
		if (movieToRemove != null) {
			movies.remove(movieToRemove);
			return true;
		}
		return false;
	}

	public List<Movie> getAllMovies() {
		return movies;
	}
	
	public void clear() {
		movies.clear();
	}

	public List<Movie> getMoviesByYear(int year) {
		List<Movie> moviesByYears = new ArrayList<>();
		for (Movie movie : movies) {
			if (movie.getYear() == year) {
				moviesByYears.add(movie);
			}
		}
		return moviesByYears;
	}
}