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
        System.out.println("========== Iniciando o processamento dos dados "+ getCurrentTimestamp() +" ==========");
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

            // Condição 1: Verificar se um caractere específico está presente no comentário
            if (comentario.contains("½ï¿") ) {
                continue;
            }

            // Condição 2: Pegar a primeira posição da string de avaliação e transformar em número
            if (!avaliacao.isEmpty() || (!Categoria.isEmpty() && CategoriasValidas.contains(Categoria))) {
                char firstChar = avaliacao.charAt(0);
                int number = Character.getNumericValue(firstChar);

                if (number >= 0 && number <= 5) {
                    // Criar um novo objeto Feedback_POI com o comentário e a nota
                    Feedback_POI feedbackTratado = new Feedback_POI(
                            comentario, // Comentário mantido
                            number, // Nota obtida
                            Endereco,
                            Categoria// Endereço Obtido
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
                // Verifica se o feedback já existe no banco de dados
                Integer count = connection.queryForObject(
                        "SELECT COUNT(*) FROM feedback WHERE descricao = ? AND rating = ?",
                        Integer.class, comentario, avaliacao
                );

                // Se o feedback não existir (count == 0), ele será inserido
                if (count == null || count == 0) {
                    // Obtém o idFilial baseado no endereço, tratando a ausência de resultado
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
                        // Caso não haja filial, idFilial permanece null
                        System.out.println("Filial não encontrada para o endereço: " + endereco);
                    }

                    // Verifica se idFilial foi encontrado antes de tentar inserir
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
                System.out.println("Erro ao processar o feedback: " + comentario);
                e.printStackTrace();  // Aqui você pode logar a exceção de forma mais detalhada
            }
        }

        System.out.println("========== Inserção de dados concluída " + sdf.format(new Date()) + " ==========");
        System.out.println("Total de registros inseridos no banco: " + registrosInseridos);
        System.out.println("Registros ignorados devido à duplicidade ou erro: " + (totalFeedbacks - registrosInseridos));
    }

    public static void inserirFiliais(List<Feedback_POI> feedbacks) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        System.out.println("========== Iniciando a inserção das filiais no banco " + sdf.format(new Date()) + " ==========");

        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        // Itera sobre cada feedback da lista
        for (Feedback_POI feedback : feedbacks) {
            String NomeEmpresa = feedback.getNome();
            String nomeFilial = feedback.getNome() + feedback.getEndereco(); // Nome da filial
            String enderecoFilial = feedback.getEndereco(); // Endereço da filial
            String latitude = feedback.getLatitude(); // Latitude
            String longitude = feedback.getLongitude(); // Longitude

            // Verifica se a filial já existe no banco
            Integer countFilial = connection.queryForObject(
                    "SELECT COUNT(*) FROM filial WHERE endereco = ? AND fkEmpresa = ?",
                    Integer.class, enderecoFilial, 1
            );

            // Se a filial não existir, insere ela
            if (countFilial == null || countFilial == 0) {

                Integer idEmpresa = null;
                try {
                    // Tenta buscar a idEmpresa a partir do nome da empresa
                    idEmpresa = connection.queryForObject(
                            "SELECT idEmpresa FROM empresa WHERE nomeEmpresa = ?",
                            Integer.class, NomeEmpresa
                    );
                } catch (EmptyResultDataAccessException e) {
                    // Caso não haja empresa, idEmpresa permanece null
                    System.out.println("Empresa não encontrada para o nome: " + NomeEmpresa);
                }

                // Verifica se o idEmpresa foi encontrado antes de tentar inserir a filial
                if (idEmpresa != null) {
                    int rowsAffectedFilial = connection.update(
                            "INSERT INTO filial (Nome, endereco, latitude, longitude, fkEmpresa) VALUES (?, ?, ?, ?, ?)",
                            nomeFilial, enderecoFilial, latitude, longitude, idEmpresa
                    );

                    if (rowsAffectedFilial > 0) {
                        System.out.println("Filial inserida com sucesso: " + nomeFilial + " - " + enderecoFilial);
                    } else {
                        System.out.println("Erro ao tentar inserir a filial: " + nomeFilial + " - " + enderecoFilial);
                    }
                } else {
                    System.out.println("Não foi possível inserir a filial sem uma empresa válida: " + nomeFilial);
                }
            }
        }

        System.out.println("========== Inserção de filiais concluída " + sdf.format(new Date()) + " ==========");
    }

    public static void gerarRecomendacoes() {
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        for (int idCategoria = 1; idCategoria <= 5; idCategoria++) {

            // Constrói o prompt inicial para enviar para a IA com as instruções
            StringBuilder promptBuilder = new StringBuilder("""
        Quero que gere 3 recomendações da melhorias em portugues do brasil, no seguinte formato: 
        1 - [Recomendação aqui]
        2 - [Recomendação aqui]
        3 - [Recomendação aqui]
        
        Suas Recomendações devem ser feitas como bases nesses comentarios:
        """);

            // Consulta ao banco de dados para obter os feedbacks de uma categoria específica
            List<String> feedbacks = connection.query(
                    "SELECT descricao FROM feedback WHERE fkCategoria = ? AND rating <= 2 LIMIT 5;", // Consulta SQL
                    new Object[]{idCategoria}, // Parâmetro da categoria (idCategoria)
                    (rs, rowNum) -> rs.getString("descricao") // Mapeia os resultados para uma lista de Strings com as descrições dos feedbacks
            );

            // Adiciona as descrições dos feedbacks ao prompt
            for (String descricao : feedbacks) {
                promptBuilder.append("\n\n").append(descricao); // Concatenar as descrições no prompt
            }

            // Converte o prompt em uma String e limita seu tamanho a 600 caracteres para evitar erros da IA
            String prompt = promptBuilder.toString();
            if (prompt.length() > 600) {
                prompt = prompt.substring(0, 600); // Se o prompt for maior que 600 caracteres, corta para 600
            }

            // Chama a API Gemini passando o prompt e obtém a resposta da IA
            String respostaIA = GeminiRestApi.obterRespostaDaIA(prompt);

            // Verifica se a resposta contém os códigos de erro 400 ou 503
            if (respostaIA.contains("400") || respostaIA.contains("503")) {
                System.err.println("Erro detectado na resposta da IA para a categoria " + idCategoria + ": " + respostaIA);
                continue; // Pula para a próxima iteração do loop se houver erro
            }

            // Exibe a resposta da IA para visualização
            System.out.println("Categoria: " + idCategoria); // Mostra o número da categoria
            System.out.println("Recomendações da IA:");
            System.out.println(respostaIA + "\n"); // Exibe as recomendações

            // Processa a resposta da IA para separar as recomendações
            // As recomendações são separadas por duas quebras de linha (\n\n)
            String[] recomendacoes = respostaIA.split("\n\n"); // Divide a resposta da IA em várias recomendações

            // Para cada recomendação, remove os números e os asteriscos e insere no banco
            for (String recomendacao : recomendacoes) {
                // Remove os asteriscos ** e os números iniciais da recomendação
                String recomendacaoLimpa = recomendacao.replaceAll("\\*\\*", "").replaceAll("^[0-9]+ - ", "").trim();

                // Obtém a data e hora atuais (quando a recomendação foi gerada)
                LocalDateTime dataCriacao = LocalDateTime.now(); // Armazena a data e hora atual

                // Tenta inserir a recomendação no banco de dados
                try {
                    connection.update(
                            // Insere a recomendação limpa, com a data de criação e a categoria no banco de dados
                            "INSERT INTO recomendacoesIA (descricao, dtCriacao, fkCategoria) VALUES (?, ?, ?)", // SQL para inserção
                            recomendacaoLimpa, dataCriacao, idCategoria // Passa a recomendação, a data e a categoria
                    );

                    // Log de sucesso, caso a inserção seja bem-sucedida
                    System.out.println("Recomendação inserida com sucesso para a categoria " + idCategoria);
                } catch (Exception e) {
                    // Log de erro, caso haja algum problema na inserção
                    System.err.println("Erro ao inserir recomendação para a categoria " + idCategoria + ": " + e.getMessage());
                }
            }
            System.out.println("\n------------------------------------\n");
        }
    }

    public static void gerarPalavrasChavesPositivas() {
        System.out.println("\n----------------- Iniciando Geração de Palavras chaves Possitivas -------------------\n");
        // Cria uma instância para conexão com o banco de dados
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        // Inicializa o prompt que será usado para gerar palavras-chave positivas
        StringBuilder promptBuilderPositivo = new StringBuilder("""
        Quero que gere as 8 principais palavras chaves, no seguinte formato: 
        Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra
        
        Lembre-se que as palavras devem estar em portugues do Brasil.
        As palavras chaves devem ser as principais palavras POSITIVAS (Exemplo: Atendimento bom, otima comida, etc..) que aparecem nesses comentarios:
        
        """);

        // Itera por cada categoria de feedback, representada por idCategoria
        for (int idCategoria = 1; idCategoria <= 5; idCategoria++) {
            // Executa uma consulta para buscar os feedbacks positivos da categoria atual
            List<String> feedbacksPossitivos = connection.query(
                    "SELECT descricao FROM feedback WHERE fkCategoria = ? AND (rating = 4 OR rating = 5) LIMIT 5;",
                    new Object[]{idCategoria},
                    (rs, rowNum) -> rs.getString("descricao") // Converte cada linha em uma String com a descrição
            );

            // Adiciona cada feedback positivo ao prompt
            for (String feedback : feedbacksPossitivos) {
                promptBuilderPositivo.append("\n\n").append(feedback);
            }

            // Converte o prompt acumulado em uma String e limita o tamanho, se necessário
            String promptPositivo = promptBuilderPositivo.toString();
            if (promptPositivo.length() > 500) {
                promptPositivo = promptPositivo.substring(0, 500); // Reduz para os primeiros 500 caracteres
            }

            // Obtém uma resposta da IA com base no prompt gerado
            String respostaIAPositiva = GeminiRestApi.obterRespostaDaIA(promptPositivo);

            // Verifica se houve erro na resposta da IA
            if (respostaIAPositiva.contains("400") || respostaIAPositiva.contains("503") || respostaIAPositiva.contains("429")) {
                System.err.println("Erro detectado na resposta da IA para a categoria " + idCategoria + ": " + respostaIAPositiva);
                continue; // Pula para a próxima categoria em caso de erro
            }

            // Exibe a resposta obtida da IA para fins de depuração
            System.out.println("\n------------------------------------\n");
            System.out.println("Categoria: " + idCategoria);
            System.out.println("Palavras Chaves Positivas da IA:");
            System.out.println(respostaIAPositiva + "\n");
            System.out.println("\n------------------------------------\n");

            try {
                // Insere as palavras-chave geradas no banco de dados, associadas à categoria atual
                connection.update(
                        "INSERT INTO palavrasChave (qualidade, palavras, fkCategoria) VALUES (?, ?, ?)",
                        "Positiva", respostaIAPositiva, idCategoria
                );
                // Log de sucesso após a inserção
                System.out.println("Palavras Chaves positivas inseridas com sucesso para a categoria " + idCategoria);
            } catch (Exception e) {
                // Log de erro em caso de falha na inserção
                System.err.println("Erro ao inserir palavras chaves positivas para a categoria " + idCategoria + ": " + e.getMessage());
            }
        }
        System.out.println("\n----------------- Fim da Geração-------------------\n");
    }

    public static void gerarPalavrasChavesNeutras() {
        System.out.println("\n----------------- Iniciando Geração de Palavras chaves Neutras -------------------\n");
        // Cria uma instância para conexão com o banco de dados
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        // Inicializa o prompt que será usado para gerar palavras-chave neutras
        StringBuilder promptBuilderNeutro = new StringBuilder("""
        Quero que gere as 8 principais palavras chaves, no seguinte formato: 
        Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra
        
        Lembre-se que as palavras devem estar em portugues do Brasil.
        As palavras chaves devem ser as principais palavras NEUTRAS (Exemplo: Atendimento ok, Comida aceitavel, etc..) que aparecem nesses comentarios:
        
        """);

        // Itera por cada categoria de feedback, representada por idCategoria
        for (int idCategoria = 1; idCategoria <= 5; idCategoria++) {
            // Executa uma consulta para buscar os feedbacks neutros da categoria atual
            List<String> feedbacksNeutros = connection.query(
                    "SELECT descricao FROM feedback WHERE fkCategoria = ? AND rating = 3 LIMIT 5;",
                    new Object[]{idCategoria},
                    (rs, rowNum) -> rs.getString("descricao") // Converte cada linha em uma String com a descrição
            );

            // Adiciona cada feedback neutro ao prompt
            for (String feedback : feedbacksNeutros) {
                promptBuilderNeutro.append("\n\n").append(feedback);
            }

            // Converte o prompt acumulado em uma String e limita o tamanho, se necessário
            String promptNeutro = promptBuilderNeutro.toString();
            if (promptNeutro.length() > 500) {
                promptNeutro = promptNeutro.substring(0, 500); // Reduz para os primeiros 600 caracteres
            }

            // Obtém uma resposta da IA com base no prompt gerado
            String respostaIANeutro = GeminiRestApi.obterRespostaDaIA(promptNeutro);

            // Verifica se houve erro na resposta da IA
            if (respostaIANeutro.contains("400") || respostaIANeutro.contains("503") || respostaIANeutro.contains("429")) {
                System.err.println("Erro detectado na resposta da IA para a categoria " + idCategoria + ": " + respostaIANeutro);
                continue; // Pula para a próxima categoria em caso de erro
            }

            // Exibe a resposta obtida da IA para fins de depuração
            System.out.println("\n------------------------------------\n");
            System.out.println("Categoria: " + idCategoria);
            System.out.println("Palavras Chaves Neutras da IA:");
            System.out.println(respostaIANeutro + "\n");
            System.out.println("\n------------------------------------\n");

            try {
                // Insere as palavras-chave geradas no banco de dados, associadas à categoria atual
                connection.update(
                        "INSERT INTO palavrasChave (qualidade, palavras, fkCategoria) VALUES (?, ?, ?)",
                        "Neutra", respostaIANeutro, idCategoria
                );
                // Log de sucesso após a inserção
                System.out.println("Palavras Chaves neutras inseridas com sucesso para a categoria " + idCategoria);
            } catch (Exception e) {
                // Log de erro em caso de falha na inserção
                System.err.println("Erro ao inserir palavras chaves neutras para a categoria " + idCategoria + ": " + e.getMessage());
            }
        }
        System.out.println("\n----------------- Fim da Geração-------------------\n");
    }

    public static void gerarPalavrasChavesNegativas() {
        System.out.println("\n----------------- Iniciando Geração de Palavras chaves Negativas -------------------\n");
        // Cria uma instância para conexão com o banco de dados
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        // Inicializa o prompt que será usado para gerar palavras-chave negativas
        StringBuilder promptBuilderNegativo = new StringBuilder("""
        Quero que gere as 8 principais palavras chaves, no seguinte formato: 
        Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra, Palavra
        
        Lembre-se que as palavras devem estar em portugues do Brasil.
        As palavras chaves devem ser as principais palavras NEGATIVAS (Exemplo: Atendimento ruim, pessima comida, etc..) que aparecem nesses comentarios:
        
        """);

        // Itera por cada categoria de feedback, representada por idCategoria
        for (int idCategoria = 1; idCategoria <= 5; idCategoria++) {
            // Executa uma consulta para buscar os feedbacks negativos da categoria atual
            List<String> feedbacksNegativos = connection.query(
                    "SELECT descricao FROM feedback WHERE fkCategoria = ? AND rating <= 2 LIMIT 5;",
                    new Object[]{idCategoria},
                    (rs, rowNum) -> rs.getString("descricao") // Converte cada linha em uma String com a descrição
            );

            // Adiciona cada feedback negativo ao prompt
            for (String feedback : feedbacksNegativos) {
                promptBuilderNegativo.append("\n\n").append(feedback);
            }

            // Converte o prompt acumulado em uma String e limita o tamanho, se necessário
            String promptNegativo = promptBuilderNegativo.toString();
            if (promptNegativo.length() > 500) {
                promptNegativo = promptNegativo.substring(0, 500); // Reduz para os primeiros 600 caracteres
            }

            // Obtém uma resposta da IA com base no prompt gerado
            String respostaIANegativo = GeminiRestApi.obterRespostaDaIA(promptNegativo);

            // Verifica se houve erro na resposta da IA
            if (respostaIANegativo.contains("400") || respostaIANegativo.contains("503") || respostaIANegativo.contains("429")) {
                System.err.println("Erro detectado na resposta da IA para a categoria " + idCategoria + ": " + respostaIANegativo);
                continue; // Pula para a próxima categoria em caso de erro
            }

            // Exibe a resposta obtida da IA para fins de depuração
            System.out.println("\n------------------------------------\n");
            System.out.println("Categoria: " + idCategoria);
            System.out.println("Palavras Chaves Negativas da IA:");
            System.out.println(respostaIANegativo + "\n");
            System.out.println("\n------------------------------------\n");

            try {
                // Insere as palavras-chave geradas no banco de dados, associadas à categoria atual
                connection.update(
                        "INSERT INTO palavrasChave (qualidade, palavras, fkCategoria) VALUES (?, ?, ?)",
                        "Negativa", respostaIANegativo, idCategoria
                );
                // Log de sucesso após a inserção
                System.out.println("Palavras Chaves negativas inseridas com sucesso para a categoria " + idCategoria);
            } catch (Exception e) {
                // Log de erro em caso de falha na inserção
                System.err.println("Erro ao inserir palavras chaves negativas para a categoria " + idCategoria + ": " + e.getMessage());
            }
        }
        System.out.println("\n----------------- Fim da Geração-------------------\n");
    }


}
