package datamind;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public class TratacaoDeDados {



    public List<Feedback_POI> processarDados(List<Feedback_POI> feedbacks) {
        System.out.println("========== Iniciando o processamento dos dados "+ getCurrentTimestamp() +" ==========");
        List<Feedback_POI> dadosTratados = new ArrayList<>();
        System.out.println("Processando...\n");
        for (Feedback_POI feedback : feedbacks) {
            String comentario = feedback.getComentario();
            String avaliacao = feedback.getAvaliacao();

            // Condição 1: Verificar se um caractere específico está presente no comentário
            if (comentario.contains("½ï¿")) {
                continue;
            }

            // Condição 2: Pegar a primeira posição da string de avaliação e transformar em número
            if (!avaliacao.isEmpty()) {
                char firstChar = avaliacao.charAt(0);
                int number = Character.getNumericValue(firstChar);

                if (number >= 0) {
                    // Criar um novo objeto Feedback_POI com o comentário e a nota
                    Feedback_POI feedbackTratado = new Feedback_POI(
                            comentario, // Comentário mantido
                            number // Nota obtida
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

    public static void inserindoDadosNoBanco(List<Feedback_POI> feedbacks){
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        System.out.println("========== Iniciando a inserção de dados no banco " + sdf.format(new Date()) +" ==========");

        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        int totalFeedbacks = feedbacks.size();
        int registrosInseridos = 0;

        System.out.println("Inserindo...\n");
        for (Feedback_POI feedback : feedbacks) {
            String comentario = feedback.getComentario();
            String avaliacao = feedback.getAvaliacao();

            // Verifica se o feedback já existe no banco de dados
            Integer count = connection.queryForObject(
                    "SELECT COUNT(*) FROM feedback WHERE descricao = ? AND rating = ?",
                    Integer.class, comentario, avaliacao
            );

            // Se o feedback não existir (count == 0), ele será inserido
            if (count == null || count == 0) {
                int rowsAffected = connection.update(
                        "INSERT INTO feedback (descricao, rating , fkEmpresa, fkCategoria) VALUES (?, ?, ?, ?);",
                        comentario, avaliacao, 1, 1
                );

                if (rowsAffected > 0) {
                    registrosInseridos++;
                }
            }
        }

        System.out.println("========== Inserção de dados concluída " + sdf.format(new Date()) +" ==========");
        System.out.println("Total de registros inseridos no banco: " + registrosInseridos);
        System.out.println("Registros ignorados devido à duplicidade ou erro: " + (totalFeedbacks - registrosInseridos));

    }
}
