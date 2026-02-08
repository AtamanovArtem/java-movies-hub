package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.List;


public class MoviesHandler extends BaseHttpHandler {
	public MoviesStore moviesStore;
	public Gson gson;
	private int nextId = 1;

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



		if (!isValidContentType(contentType, details)) {
			ErrorHandler.requestHeaderError(ex, "Получен запрос с неправильным значением Content-Type", details);
			return;
		}
		String requestBody = new String(ex.getRequestBody().readAllBytes());
		System.out.println("Полученное тело запроса: " + requestBody);
		System.out.println("Начало проверки isValidJson");
		if (!isValidJson(requestBody)) {
			ErrorHandler.validationError(ex, "Некорректный формат JSON", details);
		}
		System.out.println("Проверка isValidJson пройдена");
		if (method.equalsIgnoreCase("GET")) {
			System.out.println("Перешли в логику GET");
			System.out.println("Полученный путь: " + path);
			if (path.equals("/movies")) {
				System.out.println("Обрабатываем запрос для /movies");
				List<Movie> movies = moviesStore.getAllMovies();
				System.out.println("Список фильмов после получения: " + movies);
				System.out.println("Список фильмов перед конвертацией в JSON: " + movies.toString());
				String jsonMovies = convertMoviesToJson(movies);
				System.out.println(jsonMovies);

				sendJson(ex, 200, jsonMovies);

			} else if (path.contains("/movies/")) {
				List<Movie> movies = moviesStore.getAllMovies();
				System.out.println("Список фильмов после получения: " + movies);
				System.out.println("Обрабатываем запрос для /movies/");
				String idStr = path.substring(path.indexOf("/movies/") + "/movies/".length());
				System.out.println("Разобрали посимвольно /movies и переходим к try");
				try {
					System.out.println("Парсим id");
					int id = Integer.parseInt(idStr);
					System.out.println("Идентификатор: " + id);
					System.out.println("Список фильмов " + moviesStore.getAllMovies());
					System.out.println("Использование findMovieById");
					Movie movie = moviesStore.findMovieById(id);
					System.out.println("Найденый фильм: " + movie);

					if (movie == null) {
						System.out.println("Проверка на null");
						ErrorHandler.notFoundParameterError(ex, "Фильм не найден", details);
						System.out.println("Прошла обработка null");
						return;
					} else {
						String movieJson = gson.toJson(movie);
						sendJson(ex, 200, movieJson);
					}
				} catch (NumberFormatException e) {
					ErrorHandler.movieIdNot_Number(ex, "Некорректный формат ID", details);
				}
			} else if (path.contains("/movies/?year=")) {


				String query = ex.getRequestURI().getQuery();
				if (query != null && query.contains("year=")) {
					String yearStr = query.split("=")[1];
					try {
						int year = Integer.parseInt(yearStr);
						List<Movie> moviesByYear = moviesStore.getMoviesByYear(year);
						if (moviesByYear.isEmpty()) {
							ErrorHandler.notFoundParameterError(ex, "Фильмы за указанный год не найдены", details);
						} else {
							String movieByYearToJson = convertMoviesToJson(moviesByYear);
							sendJson(ex, 200, movieByYearToJson);
						}
					} catch (NumberFormatException e) {
						ErrorHandler.movieIdNot_Number(ex, "Некорректный параметр запроса - 'year'", details);
					}
				} else {

				}
			}
		} else if (method.equalsIgnoreCase("POST")) {
			System.out.println("Метод запроса - POST");

			Movie newMovie = gson.fromJson(requestBody, Movie.class);
			System.out.println("newMovie после десериализации: " + newMovie);
			System.out.println("Проверка validateMovie");
			if (!validateMovie(newMovie, details)) {
				ErrorHandler.validationError(ex, "Ошибка валидации", details);
				return;
			}
			System.out.println("Проверка validateMovie прошла");
			newMovie.setId(generateUniqueId());
			moviesStore.addMovie(newMovie);
			String responseJson = gson.toJson(newMovie);
			System.out.println("Проверка sendJson");
			sendJson(ex, 201, responseJson);


		} else if (method.equalsIgnoreCase("DELETE")) {
			if (path.contains("/movies/")) {
				String idStr = path.substring(path.lastIndexOf('/') + 1);
				try {
					int id = Integer.parseInt(idStr);
					Movie movie = moviesStore.findMovieById(id);
					if (movie == null) {
						ErrorHandler.notFoundParameterError(ex, "Ошибка при поиске фильма по ID", details);
					} else {
						moviesStore.removeMovieById(id);
						sendJson(ex, 204, "");
					}
				} catch (NumberFormatException e) {
					ErrorHandler.movieIdNot_Number(ex, "Некорректный формат ID", details);
				}
			}
		}
	}



	private int generateUniqueId() {
		return nextId++;
	}

	private String convertMoviesToJson(List<Movie> movies) {
		System.out.println("Лист movies при входе в метод convertMoviesToJson: " + movies.toString());
		Gson gson = new GsonBuilder()
				.serializeNulls()
				.create();
		System.out.println("В методе convertMoviesToJson перед отправлением: " + gson.toJson(movies));
		return gson.toJson(movies);
	}

	private boolean validateMovie(Movie movie, String[] details) {
		boolean isValid = true;

		if (movie.title == null || movie.title.isEmpty() || movie.title.length() > 100) {
			isValid = false;
		} else if (movie.year < 1888 || movie.year > 2027) {
			isValid = false;
		}
		return isValid;
	}

	private boolean isValidContentType(String contentType, String[] details) {
		System.out.println("Проверка содержимого заголовка в методе isValidContentType началась");
		if (contentType == null || contentType.isEmpty()) {
			details[2] = "Отсутствует заголовок Content-Type. Ожидаемый тип: application/json";
			System.out.println("Заголовок Content-Type пуст или отсутствует. Возвращается false");
			return false;
		}
		if (!"application/json; charset=UTF-8".equalsIgnoreCase(contentType)) {
			details[2] = "Неподдерживаемый тип содержимого. Ожидаемый тип: application/json";
			System.out.println("Проверка не прошла. Возвращается false");
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





