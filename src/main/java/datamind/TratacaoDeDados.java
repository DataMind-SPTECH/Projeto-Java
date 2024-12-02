package datamind;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;


import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class TratacaoDeDados {

    public List<Feedback_POI> processarDados(List<Feedback_POI> feedbacks) {
        System.out.println("\n========== Iniciando o processamento dos dados "+ getCurrentTimestamp() +" ==========");
        List<Feedback_POI> dadosTratados = new ArrayList<>();
        System.out.println("Processando...\n");

        for (Feedback_POI feedback : feedbacks) {
            String comentario = feedback.getComentario();
            String avaliacao = feedback.getAvaliacao();
            String Endereco = feedback.getEndereco();
            String Categoria = feedback.getCategoria_feedback();

            List<String> CategoriasValidas = new ArrayList<>(List.of(
                    "Qualidade do Produto",
                    "Atendimento",
                    "Tempo de espera",
                    "Experiência no Drive-thru",
                    "Experiência Geral"
            ));

            if (comentario.contains("½ï¿") ) {
                continue;
            }

            if (!avaliacao.isEmpty() || (!Categoria.isEmpty() && CategoriasValidas.contains(Categoria))) {
                char firstChar = avaliacao.charAt(0);
                int number = Character.getNumericValue(firstChar);

                if (number >= 0 && number <= 5) {
                    Feedback_POI feedbackTratado = new Feedback_POI(
                            comentario,
                            number,
                            Endereco,
                            Categoria
                    );
                    dadosTratados.add(feedbackTratado);
                } else {
                    System.out.println("\nPrimeiro caractere da avaliação não é um número válido.");
                }
            } else {
                System.out.println("\nAvaliação está vazia, ignorando feedback.");
            }

        }
        System.out.println("========== Processamento dos dados concluído " + getCurrentTimestamp() + " ==========");
        System.out.println("Total de feedbacks tratados: " + feedbacks.size());
        System.out.println("Feedbacks incorretos: " + (feedbacks.size() - dadosTratados.size()));
        System.out.println("Feedbacks corretos: " + dadosTratados.size() + "\n");
        return dadosTratados;
    }

    public String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return sdf.format(new Date());
    }

    public static void inserindoDadosNoBanco(List<Feedback_POI> feedbacks) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        System.out.println("========== Iniciando a inserção de dados no banco " + sdf.format(new Date()) + " ==========");

        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        int totalFeedbacks = feedbacks.size();
        int registrosInseridos = 0;

        System.out.println("Inserindo...\n");

        for (Feedback_POI feedback : feedbacks) {
            String comentario = feedback.getComentario();
            String avaliacao = feedback.getAvaliacao();
            String endereco = feedback.getEndereco();
            String categoria = feedback.getCategoria_feedback();

            try {
                Integer count = connection.queryForObject(
                        "SELECT COUNT(*) FROM feedback WHERE descricao = ? AND rating = ?",
                        Integer.class, comentario, avaliacao
                );

                if (count == null || count == 0) {
                    Integer idFilial = null;
                    Integer IdCategoria = null;

                    switch (categoria) {
                        case "Qualidade do produto":
                            IdCategoria = 1;
                            break;
                        case "Atendimento":
                            IdCategoria = 2;
                            break;
                        case "Tempo de espera":
                            IdCategoria = 3;
                            break;
                        case "Experiência do drive thru":
                            IdCategoria = 4;
                            break;
                        case "Experiência geral":
                            IdCategoria = 5;
                            break;
                    }

                    try {
                        idFilial = connection.queryForObject(
                                "SELECT idFilial FROM filial WHERE endereco = ?",
                                Integer.class, endereco
                        );
                    } catch (EmptyResultDataAccessException e) {
                        System.out.println("Filial não encontrada para o endereço: " + endereco);
                    }

                    if (idFilial != null) {
                        int rowsAffected = connection.update(
                                "INSERT INTO feedback (descricao, rating , fkFilial, fkCategoria) VALUES (?, ?, ?, ?);",
                                comentario, avaliacao, idFilial, IdCategoria
                        );

                        if (rowsAffected > 0) {
                            registrosInseridos++;
                        } else {
                            System.out.println("Erro ao inserir o feedback com descrição: " + comentario);
                        }
                    }
                }

            } catch (Exception e) {
            }
        }

        System.out.println("========== Inserção de dados concluída " + sdf.format(new Date()) + " ==========");
        System.out.println("Total de registros inseridos no banco: " + registrosInseridos);
        System.out.println("Registros ignorados devido à duplicidade ou erro: " + (totalFeedbacks - registrosInseridos));
    }

    public static void inserirFiliais(List<Feedback_POI> feedbacks) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        System.out.println("\n========== Iniciando a inserção das filiais no banco " + sdf.format(new Date()) + " ==========");

        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();
        System.out.println("Processando...");

        for (Feedback_POI feedback : feedbacks) {
            String NomeEmpresa = feedback.getNome();
            String nomeFilial = feedback.getNome() + feedback.getEndereco();
            String enderecoFilial = feedback.getEndereco();
            String latitude = feedback.getLatitude();
            String longitude = feedback.getLongitude();

            Integer countFilial = connection.queryForObject(
                    "SELECT COUNT(*) FROM filial WHERE endereco = ? AND fkEmpresa = ?",
                    Integer.class, enderecoFilial, 1
            );

            if (countFilial == null || countFilial == 0) {

                Integer idEmpresa = null;
                try {
                    idEmpresa = connection.queryForObject(
                            "SELECT idEmpresa FROM empresa WHERE nomeEmpresa = ?",
                            Integer.class, NomeEmpresa
                    );
                } catch (EmptyResultDataAccessException e) {
                    System.out.println("Empresa não encontrada para o nome: " + NomeEmpresa);
                }

                if (idEmpresa != null) {
                    int rowsAffectedFilial = connection.update(
                            "INSERT INTO filial (Nome, endereco, latitude, longitude, fkEmpresa) VALUES (?, ?, ?, ?, ?)",
                            nomeFilial, enderecoFilial, latitude, longitude, idEmpresa
                    );

                    if (rowsAffectedFilial > 0) {
                    } else {
                        System.out.println("Erro ao tentar inserir a filial: " + nomeFilial + " - " + enderecoFilial);
                    }
                } else {
                    System.out.println("Não foi possível inserir a filial sem uma empresa válida: " + nomeFilial);
                }
            }
        }

        System.out.println("\n========== Inserção de filiais concluída "+ sdf.format(new Date()) +" ==========");
    }

    public static void gerarRecomendacoes() throws InterruptedException {
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        System.out.println("\n========== Iniciando geração de recomendações "+ sdf.format(new Date()) +" ==========");
        connection.update("TRUNCATE TABLE recomendacoesIA");
        for (int idCategoria = 1; idCategoria <= 5; idCategoria++) {

            StringBuilder promptBuilder = new StringBuilder("""
            Quero que gere 3 recomendações da melhorias em portugues do brasil, no seguinte formato: 
            1 - [Recomendação aqui]
            2 - [Recomendação aqui]
            3 - [Recomendação aqui]
        
            Suas Recomendações devem ser feitas como bases nesses comentarios:
            """);

            List<String> feedbacks = connection.query(
                    "SELECT descricao FROM feedback WHERE fkCategoria = ? AND rating <= 2 LIMIT 5;",
                    new Object[]{idCategoria},
                    (rs, rowNum) -> rs.getString("descricao")
            );

            for (String descricao : feedbacks) {
                promptBuilder.append("\n\n").append(descricao);
            }

            String prompt = promptBuilder.toString();
            if (prompt.length() > 600) {
                prompt = prompt.substring(0, 600);
            }

            String respostaIA = GeminiRestApi.obterRespostaDaIA(prompt);

            if (respostaIA.contains("400") || respostaIA.contains("503")) {
                System.err.println("Erro detectado na resposta da IA para a categoria " + idCategoria + ": " + respostaIA);
                continue;
            }

            String[] recomendacoes = respostaIA.split("\n\n");

            for (String recomendacao : recomendacoes) {
                String recomendacaoLimpa = recomendacao.replaceAll("\\*\\*", "").replaceAll("^[0-9]+ - ", "").trim();

                LocalDateTime dataCriacao = LocalDateTime.now();

                try {
                    connection.update(
                            "INSERT INTO recomendacoesIA (descricao, dtCriacao, fkCategoria) VALUES (?, ?, ?)",
                            recomendacaoLimpa, dataCriacao, idCategoria
                    );

                    System.out.println("Recomendação inserida com sucesso para a categoria " + idCategoria);
                } catch (Exception e) {
                    System.err.println("Erro ao inserir recomendação para a categoria " + idCategoria + ": " + e.getMessage());
                }
            }
            System.out.println("\n------------------------------------\n");
        }

        System.out.println("\n========== Geração de recomendações finalizada "+ sdf.format(new Date()) +" ==========");
    }

    public static void gerarPalavrasChavesPositivas() throws InterruptedException {

        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();
        connection.update("TRUNCATE TABLE palavrasChave");

        StringBuilder promptBuilderPositivo = new StringBuilder("""
        Quero que gere as 8 principais palavras chaves, no seguinte formato: 
        Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra
        
        Lembre-se que as palavras devem estar em portugues do Brasil.
        As palavras chaves devem ser as principais palavras POSITIVAS (Exemplo: Atendimento bom, otima comida, etc..) que aparecem nesses comentarios:
        
        """);

        for (int idCategoria = 1; idCategoria <= 5; idCategoria++) {
            List<String> feedbacksPossitivos = connection.query(
                    "SELECT descricao FROM feedback WHERE fkCategoria = ? AND (rating = 4 OR rating = 5) LIMIT 5;",
                    new Object[]{idCategoria},
                    (rs, rowNum) -> rs.getString("descricao")
            );

            for (String feedback : feedbacksPossitivos) {
                promptBuilderPositivo.append("\n\n").append(feedback);
            }

            String promptPositivo = promptBuilderPositivo.toString();
            if (promptPositivo.length() > 500) {
                promptPositivo = promptPositivo.substring(0, 500);
            }

            String respostaIAPositiva = GeminiRestApi.obterRespostaDaIA(promptPositivo);

            if (respostaIAPositiva.contains("400") || respostaIAPositiva.contains("503") || respostaIAPositiva.contains("429")) {
                System.err.println("Erro detectado na resposta da IA para a categoria " + idCategoria + ": " + respostaIAPositiva);
                continue;
            }

            try {
                connection.update(
                        "INSERT INTO palavrasChave (qualidade, palavras, fkCategoria) VALUES (?, ?, ?)",
                        "Positiva", respostaIAPositiva, idCategoria
                );
                System.out.println("Palavras Chaves positivas inseridas com sucesso para a categoria " + idCategoria);
            } catch (Exception e) {
                System.err.println("Erro ao inserir palavras chaves positivas para a categoria " + idCategoria + ": " + e.getMessage());
            }
            System.out.println("\n------------------------------------\n");
        }
    }

    public static void gerarPalavrasChavesNeutras() throws InterruptedException {
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        StringBuilder promptBuilderNeutro = new StringBuilder("""
        Quero que gere as 8 principais palavras chaves, no seguinte formato: 
        Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra
        
        Lembre-se que as palavras devem estar em portugues do Brasil.
        As palavras chaves devem ser as principais palavras NEUTRAS (Exemplo: Atendimento ok, Comida aceitavel, etc..) que aparecem nesses comentarios:
        
        """);

        for (int idCategoria = 1; idCategoria <= 5; idCategoria++) {
            List<String> feedbacksNeutros = connection.query(
                    "SELECT descricao FROM feedback WHERE fkCategoria = ? AND rating = 3 LIMIT 5;",
                    new Object[]{idCategoria},
                    (rs, rowNum) -> rs.getString("descricao")
            );

            for (String feedback : feedbacksNeutros) {
                promptBuilderNeutro.append("\n\n").append(feedback);
            }

            String promptNeutro = promptBuilderNeutro.toString();
            if (promptNeutro.length() > 500) {
                promptNeutro = promptNeutro.substring(0, 500);
            }

            String respostaIANeutro = GeminiRestApi.obterRespostaDaIA(promptNeutro);

            if (respostaIANeutro.contains("400") || respostaIANeutro.contains("503") || respostaIANeutro.contains("429")) {
                System.err.println("Erro detectado na resposta da IA para a categoria " + idCategoria + ": " + respostaIANeutro);
                continue;
            }


            try {
                connection.update(
                        "INSERT INTO palavrasChave (qualidade, palavras, fkCategoria) VALUES (?, ?, ?)",
                        "Neutra", respostaIANeutro, idCategoria
                );
                System.out.println("Palavras Chaves neutras inseridas com sucesso para a categoria " + idCategoria);
            } catch (Exception e) {
                System.err.println("Erro ao inserir palavras chaves neutras para a categoria " + idCategoria + ": " + e.getMessage());
            }
            System.out.println("\n------------------------------------\n");
        }
    }

    public static void gerarPalavrasChavesNegativas() throws InterruptedException {
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        StringBuilder promptBuilderNegativo = new StringBuilder("""
        Quero que gere as 8 principais palavras chaves, no seguinte formato: 
        Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra
        
        Lembre-se que as palavras devem estar em portugues do Brasil.
        As palavras chaves devem ser as principais palavras NEGATIVAS (Exemplo: Atendimento ruim, pessima comida, etc..) que aparecem nesses comentarios:
        
        """);

        for (int idCategoria = 1; idCategoria <= 5; idCategoria++) {
            List<String> feedbacksNegativos = connection.query(
                    "SELECT descricao FROM feedback WHERE fkCategoria = ? AND rating <= 2 LIMIT 5;",
                    new Object[]{idCategoria},
                    (rs, rowNum) -> rs.getString("descricao")
            );

            for (String feedback : feedbacksNegativos) {
                promptBuilderNegativo.append("\n\n").append(feedback);
            }

            String promptNegativo = promptBuilderNegativo.toString();
            if (promptNegativo.length() > 500) {
                promptNegativo = promptNegativo.substring(0, 500);
            }

            String respostaIANegativo = GeminiRestApi.obterRespostaDaIA(promptNegativo);

            if (respostaIANegativo.contains("400") || respostaIANegativo.contains("503") || respostaIANegativo.contains("429")) {
                System.err.println("Erro detectado na resposta da IA para a categoria " + idCategoria + ": " + respostaIANegativo);
                continue;
            }

            try {
                connection.update(
                        "INSERT INTO palavrasChave (qualidade, palavras, fkCategoria) VALUES (?, ?, ?)",
                        "Negativa", respostaIANegativo, idCategoria
                );
                System.out.println("Palavras Chaves negativas inseridas com sucesso para a categoria " + idCategoria);
            } catch (Exception e) {
                System.err.println("Erro ao inserir palavras chaves negativas para a categoria " + idCategoria + ": " + e.getMessage());
            }
            System.out.println("\n------------------------------------\n");
        }
    }


}
