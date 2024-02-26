package org.acme.web;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.acme.ultils.TestContainerResource;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;

import static io.restassured.RestAssured.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(TestContainerResource.class)
class CartResourceTest {

    private static final String INSERT_WRONG_CART_IN_DB =
            "insert into carts values (999, current_timestamp, current_timestamp,'NEW', 3)";

    private static final String DELETE_WRONG_CART_IN_DB = "delete from carts where id = 999";

    @Inject
    DataSource dataSource;

    @Test
    void findAll() {
        get("/carts")
        .then()
        .statusCode(OK.getStatusCode())
        .body("size()", greaterThan(0));
    }

    @Test
    void testFindAllActiveCarts() {
        get("/carts/active").then()
                .statusCode(OK.getStatusCode());
    }

    @Test
    void testGetActiveCartForCustomer() {
        get("/carts/customer/3").then()
                .contentType(ContentType.JSON)
                .statusCode(OK.getStatusCode())
                .body(containsString("Peter"));
    }

    @Test
    void testFindById() {
        get("/carts/3").then()
                .statusCode(OK.getStatusCode())
                .body(containsString("status"))
                .body(containsString("NEW"));
        get("/carts/100").then()
                .statusCode(NO_CONTENT.getStatusCode());
    }

    @Test
    void testDelete() {
        get("/carts/active").then()
                .statusCode(OK.getStatusCode())
                .body(containsString("Jason"))
                .body(containsString("NEW"));
        delete("/carts/1").then()
                .statusCode(NO_CONTENT.getStatusCode());
        get("/carts/1").then()
                .statusCode(OK.getStatusCode())
                .body(containsString("Jason"))
                .body(containsString("CANCELED"));
    }

    @Test
    void testGetActiveCartForCustomerWhenThereAreTwoCartsInDB() {
        executeSql(INSERT_WRONG_CART_IN_DB);

        get("/carts/customer/3").then()
                .statusCode(INTERNAL_SERVER_ERROR.getStatusCode())
                .log().all()
                .body(containsString("Many active carts detected !!!"));
        executeSql(DELETE_WRONG_CART_IN_DB);
    }

    private void executeSql(String query) {
        try (var connection = dataSource.getConnection()) {
            var statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (SQLException e) {
            throw new IllegalStateException("Error has occurred while trying to execute SQL Query: " + e.getMessage());
        }
    }
}