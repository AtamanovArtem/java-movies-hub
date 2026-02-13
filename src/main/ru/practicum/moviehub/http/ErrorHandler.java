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
		byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(422, -1);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(responseBytes);
		}
	}

	public static void requestHeaderError(HttpExchange exchange, String errorMessage, String[] details) throws
			IOException {

		ErrorResponse errorResponse = new ErrorResponse(errorMessage, details);
		GsonBuilder gsonBuilder = new GsonBuilder()
				.serializeNulls();

		Gson gson = gsonBuilder.create();
		String jsonResponse = gson.toJson(errorResponse);
		byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(415, -1);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(responseBytes);
		}
	}

	public static void notFoundParameterError(HttpExchange exchange, String errorMessage, String[] details)
			throws IOException {

		ErrorResponse errorResponse = new ErrorResponse(errorMessage, details);
		Gson gson = new Gson();
		String jsonResponse = gson.toJson(errorResponse);
		byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
		exchange.sendResponseHeaders(404, -1);

		try (OutputStream os = exchange.getResponseBody()) {
			os.write(responseBytes);
		}
	}

	public static void movieIdNotNumber(HttpExchange exchange, String errorMessage, String[] details) throws
			IOException { //Поправленный
		ErrorResponse errorResponse = new ErrorResponse(errorMessage, details);
		Gson gson = new Gson();
		String jsonResponse = gson.toJson(errorResponse);
		byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(400, -1);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(responseBytes);
		}
	}
}
