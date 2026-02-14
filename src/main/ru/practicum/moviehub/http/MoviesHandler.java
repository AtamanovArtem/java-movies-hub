package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class MoviesHandler extends BaseHttpHandler {
	protected MoviesStore moviesStore;
	protected Gson gson;
	protected int nextId = 1;

	public MoviesHandler(MoviesStore moviesStore) {
		this.moviesStore = moviesStore;
		this.gson = new GsonBuilder().serializeNulls().create();
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String method = ex.getRequestMethod();
		String contentType = ex.getRequestHeaders().getFirst("Content-Type");
		String[] details = {"название не должно быть пустым", "год должен быть между 1888 и 2026", "Неподдерживаемый " +
				"тип содержимого. Ожидаемый тип: application/json"};
		String path = ex.getRequestURI().getPath();

		if (method.equalsIgnoreCase("GET")) {

			if (path.equals("/movies")) {
				String query = ex.getRequestURI().getQuery();
				if (query != null && query.startsWith("year=")) {
					try {
						String yearStr = query.split("=")[1];
						int year = Integer.parseInt(yearStr);
						List<Movie> moviesByYear = moviesStore.getMoviesByYear(year);

						if (year < 1888 || year > 2026) {
							sendJson(ex, 200, "[]");
							return;
						}
						sendJson(ex, 200, convertMoviesToJson(moviesByYear));
						return;
					} catch (NumberFormatException e) {
						ErrorHandler.movieIdNotNumber(ex, "Некорректный параметр запроса 'year'", details);
						return;
					}
				}
				List<Movie> movies = moviesStore.getAllMovies();
				sendJson(ex, 200, convertMoviesToJson(movies));

			} else if (path.matches("/movies/\\d+")) {

				String idStr = path.substring(path.indexOf("/movies/") + "/movies/".length());
				try {
					int id = Integer.parseInt(idStr);
					Movie movie = moviesStore.findMovieById(id);
					if (movie == null) {
						ErrorHandler.notFoundParameterError(ex, "Фильм не найден", details);
					} else {
						String movieJson = gson.toJson(movie);
						sendJson(ex, 200, movieJson);
					}
				} catch (NumberFormatException e) {
					ErrorHandler.movieIdNotNumber(ex, "Некорректный формат ID", details);
					return;
				}
			} else if (path.startsWith("/movies/")) {
				ErrorHandler.movieIdNotNumber(ex, "Некорректный формат ID", details);
				return;
			} else {
				ErrorHandler.notFoundParameterError(ex, "Эндпоинт не найден", details);
				return;
			}

		} else if (method.equalsIgnoreCase("POST")) {

			if (!isValidContentType(contentType, details)) {
				ErrorHandler.requestHeaderError(ex, "Получен запрос с неправильным значением Content-Type",
						details);
				return;
			}
			String requestBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			if (!isValidJson(requestBody)) {
				ErrorHandler.validationError(ex, "Некорректный формат JSON", details);
				return;
			}
			Movie newMovie = gson.fromJson(requestBody, Movie.class);
			if (!validateMovie(newMovie, details)) {
				ErrorHandler.validationError(ex, "Ошибка валидации", details);
				return;
			}
			newMovie.setId(generateUniqueId());
			moviesStore.addMovie(newMovie);
			String responseJson = gson.toJson(newMovie);
			sendJson(ex, 201, responseJson);

		} else if (method.equalsIgnoreCase("DELETE")) {
			if (path.contains("/movies/")) {
				String idStr = path.substring(path.lastIndexOf('/') + 1);
				try {
					int id = Integer.parseInt(idStr);
					Movie movie = moviesStore.findMovieById(id);
					if (movie == null) {
						ErrorHandler.notFoundParameterError(ex, "Фильм по указанному ID не найден", details);
						return;
					} else {
						moviesStore.removeMovieById(id);
						ex.sendResponseHeaders(204, -1);
						ex.close();
					}
				} catch (NumberFormatException e) {
					ErrorHandler.movieIdNotNumber(ex, "Некорректный формат ID", details);
					return;
				}
			}
		}
	}


	private int generateUniqueId() {
		return nextId++;
	}

	private String convertMoviesToJson(List<Movie> movies) {

		return gson.toJson(movies);
	}

	private boolean validateMovie(Movie movie, String[] details) {
		boolean isValid = true;

		if (movie.getTitle() == null || movie.getTitle().isEmpty() || movie.getTitle().length() > 100) {
			isValid = false;
			details[0] = "Фильм с указанным названием не найден";
		} else if (movie.getYear() < 1888 || movie.getYear() > 2026) {
			isValid = false;
			details[0] = "Указан некорректный год фильма";
		}
		return isValid;
	}

	private boolean isValidContentType(String contentType, String[] details) {

		if (contentType == null || contentType.isEmpty()) {
			details[0] = "Отсутствует заголовок Content-Type";
			return false;
		}
		if (!"application/json; charset=UTF-8".equalsIgnoreCase(contentType)) {
			details[0] = "Неподдерживаемый тип содержимого";
			return false;
		}
		return true;
	}

	public boolean isValidJson(String json) {
		try {
			gson.fromJson(json, Object.class);
			return true;
		} catch (JsonSyntaxException e) {
			return false;
		}
	}
}





