package datamind;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class DBConnectionProvider {

    private final DataSource dataSource;

    public DBConnectionProvider() {
        String DB_HOST = System.getenv("DB_HOST");
        String DB_PORT = System.getenv("DB_PORT");
        String DB_DATABASE = System.getenv("DB_DATABASE");
        String DB_USER = System.getenv("DB_USER");
        String DB_PASSWORD = System.getenv("DB_PASSWORD");

        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl("jdbc:mysql://"+ DB_HOST +":"+ DB_PORT +"/"+ DB_DATABASE);
        basicDataSource.setUsername(DB_USER);
        basicDataSource.setPassword(DB_PASSWORD);
        basicDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");


        //Para facilitar teste, não excluir

        /*BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl("jdbc:mysql://localhost:3306/datamind");
        basicDataSource.setUsername("root"); // Nome de usuário correto
        basicDataSource.setPassword("arr161820"); // Senha correta
        basicDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");*/

        this.dataSource = basicDataSource;
    }

    public JdbcTemplate getConnection() {
        return new JdbcTemplate(dataSource);
    }
}
