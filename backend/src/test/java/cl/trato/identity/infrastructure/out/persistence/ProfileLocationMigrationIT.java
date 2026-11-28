package cl.trato.identity.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import cl.trato.support.IntegrationTest;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** Migración V12: los nombres no reconocidos se descartan sin inventar códigos CUT. */
@IntegrationTest
class ProfileLocationMigrationIT {

  @Autowired JdbcTemplate jdbc;

  @Test
  void maps_normalized_names_and_leaves_unmatched_values_null() {
    String schema = "profile_location_" + UUID.randomUUID().toString().replace("-", "");
    jdbc.execute(
        (ConnectionCallback<Void>)
            connection -> {
              try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA " + schema);
                connection.setSchema(schema);
                statement.execute(
                    "CREATE TABLE regions (code varchar(2) primary key, name varchar(60) not null)");
                statement.execute(
                    """
                    CREATE TABLE communes (
                        code varchar(5) primary key,
                        name varchar(60) not null,
                        region_code varchar(2) not null)
                    """);
                statement.execute(
                    "CREATE TABLE users (id integer primary key, region varchar(60), commune varchar(60))");
                statement.execute(
                    "INSERT INTO regions VALUES ('05', 'Valparaíso'), ('13', 'Metropolitana de Santiago')");
                statement.execute(
                    "INSERT INTO communes VALUES ('05109', 'Viña del Mar', '05'), ('13101', 'Santiago', '13')");
                statement.execute(
                    """
                    INSERT INTO users VALUES
                        (1, '  VALPARAISO ', 'viña   del mar'),
                        (2, 'Región inventada', 'Comuna inventada'),
                        (3, 'Metropolitana de Santiago', 'No existe')
                    """);
              }

              ScriptUtils.executeSqlScript(
                  connection,
                  new ClassPathResource(
                      "db/migration/V12__use_cut_codes_for_user_profile_location.sql"));

              try (Statement statement = connection.createStatement();
                  ResultSet result =
                      statement.executeQuery(
                          "SELECT id, region_code, commune_code FROM users ORDER BY id")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("region_code")).isEqualTo("05");
                assertThat(result.getString("commune_code")).isEqualTo("05109");
                assertThat(result.next()).isTrue();
                assertThat(result.getString("region_code")).isNull();
                assertThat(result.getString("commune_code")).isNull();
                assertThat(result.next()).isTrue();
                assertThat(result.getString("region_code")).isNull();
                assertThat(result.getString("commune_code")).isNull();
              }
              connection.setSchema("public");
              try (Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
              }
              return null;
            });
  }
}
