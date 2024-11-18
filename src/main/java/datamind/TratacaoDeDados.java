package datamind;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

            // Condição 1: Verificar se um caractere específico está presente no comentário
            if (comentario.contains("½ï¿")) {
                continue;
            }

            // Condição 2: Pegar a primeira posição da string de avaliação e transformar em número
            if (!avaliacao.isEmpty()) {
                char firstChar = avaliacao.charAt(0);
                int number = Character.getNumericValue(firstChar);

                if (number >= 0 && number <= 5) {
                    // Criar um novo objeto Feedback_POI com o comentário e a nota
                    Feedback_POI feedbackTratado = new Feedback_POI(
                            comentario, // Comentário mantido
                            number, // Nota obtida
                            Endereco // Endereço Obtido
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
                                comentario, avaliacao, idFilial, 1
                        );

                        if (rowsAffected > 0) {
                            registrosInseridos++;
                        } else {
                            System.out.println("Erro ao inserir o feedback com descrição: " + comentario);
                        }
                    }
                } else {
                    System.out.println("Feedback duplicado detectado: " + comentario + " - " + avaliacao);
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
            double latitude = feedback.getLatitude(); // Latitude
            double longitude = feedback.getLongitude(); // Longitude

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
            } else {
                System.out.println("Filial já existe no banco: " + nomeFilial + " - " + enderecoFilial);
            }
        }

        System.out.println("========== Inserção de filiais concluída " + sdf.format(new Date()) + " ==========");
    }


}
