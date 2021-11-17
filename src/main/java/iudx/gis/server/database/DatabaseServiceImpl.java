package iudx.gis.server.database;

import static iudx.gis.server.database.util.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

import java.util.Optional;

public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
  private PostgresClient pgSQLClient;
  public static final String SELECT_GIS_SERVER_URL =
      "SELECT * FROM gis WHERE iudx_resource_id='$1'";
 
  public DatabaseServiceImpl(PostgresClient pgClient) {
    // TODO Auto-generated constructor stub
    this.pgSQLClient = pgClient;
  }

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Future<JsonObject> getGISURL = getURLInDb(request.getString("id"));
    getGISURL.onComplete(getUserApiKeyHandler -> {
      if (getUserApiKeyHandler.succeeded()) {
        LOGGER.info("DATABASE_READ_SUCCESS");
        handler.handle(Future.succeededFuture(getUserApiKeyHandler.result()));
      } else {
        LOGGER.info("DATABASE_READ_FAILURE");
      }
    });
    return this;
  }

  @Override
  public DatabaseService insertIntoDb(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    handler.handle(Future.succeededFuture(new JsonObject().put("a", "b")));
    return this;
  }

  @Override
  public DatabaseService insertAdminDetails(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String resourceId = request.getString(ID);
    String serverUrl = request.getString(SERVER_URL);
    Long serverPort = request.getLong(SERVER_PORT);
    Boolean isSecure = request.getBoolean(SECURE);

    Optional<JsonObject> accessInfo = Optional.ofNullable(request.getJsonObject(ACCESS_INFO));

    String query = INSERT_ADMIN_DETAILS_QUERY.replace("$1", resourceId)
        .replace("$2", serverUrl)
        .replace("$3", serverPort.toString())
        .replace("$4", isSecure.toString());

    if (accessInfo.isPresent() && !accessInfo.get().isEmpty()) {
      JsonObject accessObject = accessInfo.get();
      String username = accessObject.getString(USERNAME);
      String password = accessObject.getString(PASSWORD);
      query = query.replace("$5", username).replace("$6", password);
    } else {
      query = query.replace("$5", "").replace("$6", "");
    }

    pgSQLClient.executeAsync(query)
        .onSuccess(ar -> {
          LOGGER.debug("Insert admin details operation successful!");
          handler.handle(Future.succeededFuture(new JsonObject().put(TYPE, SUCCESS)));
        })
        .onFailure(ar -> {
          LOGGER.error("Insert admin operation failed due to: {}", ar.toString());
          handler.handle(Future.failedFuture(ar));
        });
    return this;
  }

  @Override
  public DatabaseService updateAdminDetails(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String resourceId = request.getString(ID);
    String serverUrl = request.getString(SERVER_URL);
    Long serverPort = request.getLong(SERVER_PORT);
    Boolean isSecure = request.getBoolean(SECURE);

    Optional<JsonObject> accessInfo = Optional.ofNullable(request.getJsonObject(ACCESS_INFO));

    String query = UPDATE_ADMIN_DETAILS_QUERY.replace("$1", serverUrl)
        .replace("$2", serverPort.toString())
        .replace("$3", isSecure.toString())
        .replace("$6", resourceId);

    if (accessInfo.isPresent() && !accessInfo.get().isEmpty()) {
      JsonObject accessObject = accessInfo.get();
      String username = accessObject.getString(USERNAME);
      String password = accessObject.getString(PASSWORD);
      query = query.replace("$4", username).replace("$5", password);
    } else {
      query = query.replace("$4", "").replace("$5", "");
    }

    pgSQLClient.executeAsync(query)
        .onSuccess(ar -> {
          LOGGER.debug("Update admin details operation successful!");
          handler.handle(Future.succeededFuture(new JsonObject().put(TYPE, SUCCESS)));
        })
        .onFailure(ar -> {
          LOGGER.error("Update admin operation failed due to: {}", ar.toString());
          handler.handle(Future.failedFuture(ar));
        });

    return this;
  }

  @Override
  public DatabaseService deleteAdminDetails(String resourceId, Handler<AsyncResult<JsonObject>> handler) {
    String query = DELETE_ADMIN_DETAILS_QUERY.replace("$1", resourceId);

    pgSQLClient.executeAsync(query)
        .onSuccess(ar -> {
          LOGGER.debug("Delete admin details operation successful!");
          handler.handle(Future.succeededFuture(new JsonObject().put(TYPE, SUCCESS)));
        })
        .onFailure(ar -> {
          LOGGER.error("Delete admin operation failed due to: {}", ar.toString());
          handler.handle(Future.failedFuture(ar));
        });

    return this;
  }

  Future<JsonObject> getURLInDb(String id) {

    LOGGER.debug("Info : PSQLClient#getUserInDb() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    String query = SELECT_GIS_SERVER_URL.replace("$1", id);
    LOGGER.debug("Info : " + query);
    // Check in DB, get username and password
    pgSQLClient.executeAsync(query).onComplete(db -> {
      LOGGER.debug("Info : PSQLClient#getUserInDb()executeAsync completed");
      if (db.succeeded()) {
        LOGGER.debug("Info : PSQLClient#getUserInDb()executeAsync success");
        String url = null;
        // Get the apiKey
        RowSet<Row> result = db.result();
        if (db.result().size() > 0) {
          for (Row row : result) {
            url = row.getString(2);
          }
        }
        response.put("type", "success");
        response.put("title", "Successfully fetched the GIS server URL");
        response.put("results", new JsonArray().add(new JsonObject().put("URL", url)));
        promise.complete(response);
      } else {
        LOGGER.fatal("Fail : PSQLClient#getUserInDb()executeAsync failed");
        promise.fail("Error : Get ID from database failed");
      }
    });
    return promise.future();
  }


}
