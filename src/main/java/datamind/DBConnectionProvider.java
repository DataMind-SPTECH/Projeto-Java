package datamind;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class DBConnectionProvider {

    private final DataSource dataSource;

    public DBConnectionProvider() {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl("jdbc:mysql://"+System.getenv("DB_HOST")+":3306/datamind");
        basicDataSource.setUsername(System.getenv("DB_USER"));
        basicDataSource.setPassword(System.getenv("DB_PASSWORD"));
        basicDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
/* 
Para facilitar teste, não excluir

        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl("jdbc:mysql://localhost:3306/datamind");
        basicDataSource.setUsername("root"); // Nome de usuário correto
        basicDataSource.setPassword("Naomelembro2024@"); // Senha correta
        basicDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
*/
        this.dataSource = basicDataSource;
    }

    public JdbcTemplate getConnection() {
        return new JdbcTemplate(dataSource);
    }
}
