package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class MoviesApiTest {
	private static final String BASE = "http://localhost:8080";
	private static MoviesServer server;
	private static MoviesStore moviesStore;
	private static HttpClient client;
	private static boolean serverStarted = false;

	public MoviesApiTest() throws IOException, InterruptedException {
	}

	@BeforeAll
	static void beforeAll() {
		moviesStore = new MoviesStore();
		server = new MoviesServer(moviesStore, 8080);
		server.start();
		serverStarted = true;
		client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(2))
				.build();
	}

	@AfterAll
	static void afterAll() {
		if (serverStarted) {
			server.stop();
			serverStarted = false;
		}
	}

	@BeforeEach
	void beforeEach() {
		moviesStore.clear();
	}

	@Test
	void getMoviesWhenEmptyReturnsEmptyArray() throws Exception {
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.GET()
				.header("Content-Type", "application/json; charset=UTF-8")
				.build();

		HttpResponse<String> resp =
				client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

		assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

		String contentTypeHeaderValue =
				resp.headers().firstValue("Content-Type").orElse("");
		assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
				"Content-Type должен содержать формат данных и кодировку");

		System.out.println("Body before trim: " + resp.body());
		String body = resp.body().trim();
		System.out.println("Body after trim: " + body);
		assertEquals("[]", body, "Ответ должен быть пустым массивом JSON");
	}


	@Test
	void getMoviesIdErrorMovieNotFound() throws Exception {

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/2"))
				.GET()
				.build();
		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(404, resp.statusCode(), "GET /movies/id должен вернуть 404 если фильм не найден");
	}

	@Test
	void getMoviesIdMovieFound() throws IOException, InterruptedException {

		Movie movie1 = new Movie("Название фильма 1", 2020, 1);
		Movie movie2 = new Movie("Название фильма 2", 2021, 2);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);

		Gson gson = new GsonBuilder()
				.serializeNulls()
				.create();
		String moviesArrayJson = gson.toJson(moviesStore.movies);
		List<Movie> movies = gson.fromJson(moviesArrayJson, new ListOfMoviesTypeToken().getType());

		assertNotNull(moviesStore.findMovieById(2), "Фильм с id = 2 должен быть найден в списке");
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/2"))
				.GET()
				.build();

		HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(200, response.statusCode(), "GET /movies/id должен вернуть 200 если фильм найден");
	}


	@Test
	void getMoviesArray() throws Exception {
		Movie movie1 = new Movie("Название фильма 1", 2020, 1);
		Movie movie2 = new Movie("Название фильма 2", 2021, 2);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.GET()
				.header("Content-Type", "application/json; charset=UTF-8")
				.build();

		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

		assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
		Gson gson = new GsonBuilder()
				.serializeNulls()
				.create();
		String moviesArrayJson = gson.toJson(moviesStore.movies);
		List<Movie> movies = gson.fromJson(moviesArrayJson, new ListOfMoviesTypeToken().getType());

		assertEquals(2, movies.size(), "Список фильмов должен содержать 2 элемента");

		for (int i = 0; i < movies.size(); i++) {
			Movie movie = movies.get(i);

			if (i == 0) {
				assertEquals("Название фильма 1", movie.getTitle(), "Первый фильм должен иметь " +
						"правильное название");
				assertEquals(1, movie.getId(), "Первый фильм должен иметь правильный ID");
			} else if (i == 1) {
				assertEquals("Название фильма 2", movie.getTitle(), "Второй фильм должен иметь " +
						"правильное название");
				assertEquals(2, movie.getId(), "Второй фильм должен иметь правильный ID");
			}
		}
	}

	@Test
	void getMoviesIdErrorWhenIdIsNotNumber() throws Exception {

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/abc"))
				.GET()
				.build();
		HttpResponse<String> response = client.send(
				request,
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
		);

		assertEquals(400, response.statusCode(),
				"GET /movies/{id} должен вернуть 400, если id не является числом");

	}


	@Test
	void postMovies_whenSuccessfullyAdded() throws IOException, InterruptedException {
		Gson gson = new Gson();
		String json = gson.toJson(new Movie("Название фильма", 2020, 1));
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.header("Content-Type", "application/json; charset=UTF-8")
				.build();

		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

		assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
	}

	@Test
	void postMovies_whenTitleIsNull() throws IOException, InterruptedException {
		String json = "{\"title\": \"\", \"year\": 2023, \"id\": 123}";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.header("Content-Type", "application/json; charset=UTF-8")
				.build();

		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

		assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
	}

	@Test
	void postMovies_whenTitleIsVeryLength() throws IOException, InterruptedException {
		String json = "{\"title\": \"11111111111111111111111111111111111111111111111111111111111111111111111111111111" +
				"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
				"\", \"year\": 2023, \"id\": 123}";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.header("Content-Type", "application/json; charset=UTF-8")
				.build();

		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

		assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
	}

	@Test
	void postMovies_whenYearIsNotValid() throws IOException, InterruptedException {
		String json = "{\"title\": \"new Movie\", \"year\": 1887, \"id\": 123}";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.header("Content-Type", "application/json; charset=UTF-8")
				.build();

		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

		assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
	}

	@Test
	void postMovies_whenContentTypeIsValid() throws IOException, InterruptedException {
		String json = "{\"title\": \"new Movie\", \"year\": 1887, \"id\": 123}";
		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies"))
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.header("Content-Type", "")
				.build();

		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

		assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");
	}

	@Test
	void deleteMoviesId() throws IOException, InterruptedException {
		Movie movie = new Movie("Название фильма", 2020, 1);
		moviesStore.addMovie(movie);

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/" + movie.getId()))
				.DELETE()
				.build();

		HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(204, response.statusCode(), "DELETE /movies/id должен вернуть 204 если фильм удалён");

		assertNull(moviesStore.findMovieById(movie.getId()), "Фильм должен быть удалён из хранилища");
	}

	@Test
	void deleteNonExistentMovie() throws IOException, InterruptedException {
		long nonExistentId = 154; // заведомо несуществующий ID

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/" + nonExistentId))
				.DELETE()
				.build();

		HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(404, response.statusCode(), "DELETE /movies/id должен вернуть 404 если фильм не найден");
	}

	@Test
	void deleteWithInvalidId() throws IOException, InterruptedException {
		String invalidId = "abc";

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies/" + invalidId))
				.DELETE()
				.build();

		HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

		assertEquals(400, response.statusCode(), "DELETE /movies/id должен вернуть 400 если id не является числом");
	}

	@Test
	void getMoviesByYearExistingYear() throws IOException, InterruptedException {
		Movie movie1 = new Movie("Название фильма 1", 2020, 1);
		Movie movie2 = new Movie("Название фильма 2", 2021, 2);
		moviesStore.addMovie(movie1);
		moviesStore.addMovie(movie2);
		int existingYear = 2020;

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies?year=" + existingYear))
				.GET()
				.build();

		HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(200, response.statusCode(), "GET /movies?year должен вернуть 200 для существующего года");
		assertTrue(response.body().contains("["));
	}

	@Test
	void getMoviesByYearNonExistentYear() throws IOException, InterruptedException {
		int nonExistentYear = 9999;

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies?year=" + nonExistentYear))
				.GET()
				.build();

		HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(200, response.statusCode(), "GET /movies?year должен вернуть 200 даже для несуществующего года");
		assertTrue(response.body().contains("[]"));
	}

	@Test
	void getMoviesByYearInvalidYear() throws IOException, InterruptedException {
		String invalidYear = "abc";

		HttpRequest req = HttpRequest.newBuilder()
				.uri(URI.create(BASE + "/movies?year=" + invalidYear))
				.GET()
				.build();

		HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(400, response.statusCode(), "GET /movies?year должен вернуть 400 для некорректного значения года");
	}

}








