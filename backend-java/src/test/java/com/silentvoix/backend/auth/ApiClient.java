package com.silentvoix.backend.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.jayway.jsonpath.JsonPath;
import com.silentvoix.backend.database.TestDatabase;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Calls the API the way the app does, for tests against a real database. */
public final class ApiClient {

    public static final String PASSWORD = "mat-khau-dai";

    private final MockMvc mvc;
    private final TestDatabase.Database database;

    public ApiClient(MockMvc mvc, TestDatabase.Database database) {
        this.mvc = mvc;
        this.database = database;
    }

    public ResultActions register(String email, String password, String displayName) throws Exception {
        String name = displayName == null ? "null" : "\"" + displayName + "\"";
        return post("/api/v1/auth/register", null, """
                {"email": "%s", "password": "%s", "displayName": %s, "platform": "android", "appVersion": "0.1.0"}"""
                .formatted(email, password, name));
    }

    public ResultActions login(String email, String password) throws Exception {
        return post("/api/v1/auth/login", null, """
                {"email": "%s", "password": "%s", "platform": "android", "appVersion": "0.1.0"}"""
                .formatted(email, password));
    }

    /** Registers [email] and returns its session token. */
    public String signUp(String email) throws Exception {
        return token(register(email, PASSWORD, "Lan"));
    }

    /** Registers [email], makes it an admin in the database, and returns its session token. */
    public String signUpAdmin(String email) throws Exception {
        String token = signUp(email);
        sql("""
                UPDATE app_user SET role = 'admin'
                WHERE id = (SELECT user_id FROM user_identity WHERE provider = 'email' AND subject = ?)""", email);
        return token;
    }

    public ResultActions get(String path, String token) throws Exception {
        return send(MockMvcRequestBuilders.get(path), token);
    }

    public ResultActions post(String path, String token, String json) throws Exception {
        return send(MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    public ResultActions patch(String path, String token, String json) throws Exception {
        return send(MockMvcRequestBuilders.patch(path).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String token) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request);
    }

    public String userId(String token) throws Exception {
        return JsonPath.read(body(get("/api/v1/auth/me", token)), "$.id");
    }

    public static String token(ResultActions result) throws Exception {
        return JsonPath.read(body(result), "$.token");
    }

    public static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    public void sql(String statement, Object... params) throws SQLException {
        try (HikariDataSource db = database.dataSource();
             Connection connection = db.getConnection();
             PreparedStatement prepared = bind(connection.prepareStatement(statement), params)) {
            prepared.execute();
        }
    }

    public Object scalar(String query, Object... params) throws SQLException {
        try (HikariDataSource db = database.dataSource();
             Connection connection = db.getConnection();
             PreparedStatement prepared = bind(connection.prepareStatement(query), params);
             ResultSet rows = prepared.executeQuery()) {
            return rows.next() ? rows.getObject(1) : null;
        }
    }

    private static PreparedStatement bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
        return statement;
    }
}
