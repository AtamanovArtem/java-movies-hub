package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ErrorHandler {

	public static void validationError(HttpExchange exchange, String errorMessage, String[] details) throws
			IOException {

		ErrorResponse errorResponse = new ErrorResponse(errorMessage, details);
		Gson gson = new Gson();
		String jsonResponse = gson.toJson(errorResponse);
		exchange.sendResponseHeaders(422, -1);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void requestHeaderError(HttpExchange exchange, String errorMessage, String[] details) throws
			IOException {

		ErrorResponse errorResponse = new ErrorResponse(errorMessage, details);
		GsonBuilder gsonBuilder = new GsonBuilder()
				.serializeNulls();

		Gson gson = gsonBuilder.create();
		String jsonResponse = gson.toJson(errorResponse);
		System.out.println(jsonResponse);
		System.out.println(jsonResponse.length());
		exchange.sendResponseHeaders(415, -1);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void notFoundParameterError(HttpExchange exchange, String errorMessage, String[] details)
			throws IOException {

		ErrorResponse errorResponse = new ErrorResponse(errorMessage, details);
		Gson gson = new Gson();
		String jsonResponse = gson.toJson(errorResponse);

		System.out.println("Передача ответа от сервера: 404");
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		exchange.sendResponseHeaders(404, -1);


		System.out.println("Передача ответа от сервера произошла");
		System.out.println("Начало записи в выходной поток");
		System.out.println("JSON response: " + jsonResponse);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("запись в выходной поток произошла");
	}

	public static void movieIdNot_Number (HttpExchange exchange, String errorMessage, String[] details) throws
			IOException {
		ErrorResponse errorResponse = new ErrorResponse(errorMessage, details);
		Gson gson = new Gson();
		String jsonResponse = gson.toJson(errorResponse);
		exchange.sendResponseHeaders(400, -1);
		details[4] = "Некорректный параметр";
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
