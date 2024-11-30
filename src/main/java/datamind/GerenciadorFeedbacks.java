package datamind;

import lombok.Cleanup;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class GerenciadorFeedbacks {

    public List<Feedback_POI> criar() throws IOException {
        System.out.println("\n========== Iniciando criação de feedbacks "+ getCurrentTimestamp() +" ==========");

        List<Feedback_POI> feedbacks = new ArrayList<>();

        // Recuperando o arquivo
        //System.out.println("Abrindo arquivo Excel...");
        //@Cleanup FileInputStream file = new FileInputStream("DataSet-McDonalds.xlsx");

        //Para Facilitar teste, não excluir
        @Cleanup FileInputStream file = new FileInputStream("src/main/feedbacks_categorizados.xlsx");

        Workbook workbook = new XSSFWorkbook(file);

        // Setando a aba
        System.out.println("Carregando primeira aba da planilha...");
        Sheet sheet = workbook.getSheetAt(0);

        // Setando as linhas
        System.out.println("Lendo linhas da planilha...\n");
        List<Row> rows = (List<Row>) toList(sheet.iterator());

        // Removendo os títulos
        rows.remove(0);

        // Processando cada linha
        rows.forEach(row -> {

            // Setando as células
            List<Cell> cells = (List<Cell>) toList(row.cellIterator());

            if (cells.size() < 11) {
                //System.err.println("Linha ignorada devido ao número insuficiente de células: " + cells.size());
                return; // Ignora linhas inválidas
            }

            // Atribui os valores para a classe Feedback_POI
            Feedback_POI feedback = Feedback_POI.builder()
                    .Id((int) cells.get(0).getNumericCellValue())
                    .Nome(cells.get(1).getStringCellValue())
                    .Categoria(cells.get(2).getStringCellValue())
                    .Endereco(cells.get(3).getStringCellValue())
                    .Latitude(String.valueOf(cells.get(4).getNumericCellValue()))
                    .Longitude(getCellValueAsString(cells.get(5)))
                    .Rating_count(getCellValueAsString(cells.get(6)))
                    .Tempo_Feedback(cells.get(7).getStringCellValue())
                    .Comentario(cells.get(8).getStringCellValue())
                    .Avaliacao(cells.get(9).getStringCellValue())
                    .categoria_feedback(cells.get(10).getStringCellValue())
                    .build();

            feedbacks.add(feedback);
        });

        System.out.println("========== Criação de feedbacks concluída "+ getCurrentTimestamp() + " ==========");
        System.out.println("Total de feedbacks criados: " + feedbacks.size() + "\n");
        return feedbacks;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return ""; // Retorna uma string vazia se a célula for nula
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Avalia a fórmula e retorna o resultado
                return String.valueOf(cell.getNumericCellValue());
            case BLANK:
                return ""; // Retorna uma string vazia para células em branco
            default:
                throw new IllegalStateException("Tipo de célula inesperado: " + cell.getCellType());
        }
    }

    public void verificarENotificar(List<Feedback_POI> feedbacks) throws IOException, InterruptedException {
        Slack slack = new Slack("https://hooks.slack.com/services/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");

        // Inicializando contadores como Integer
        Integer totalCincoEstrelas = 0;
        Integer totalAvaliacoesNegativas = 0;
        Integer totalAvaliacoesNeutras = 0;

        // Percorrendo cada feedback e verificando as avaliações individuais
        for (Feedback_POI feedback : feedbacks) {
            String avaliacao = feedback.getAvaliacao();

            // Verifica se a avaliação é 5 estrelas
            if (avaliacao.equals("5")) {
                totalCincoEstrelas++;
            }

            // Verifica se a avaliação é 2 estrelas ou menos (avaliação negativa)
            if (avaliacao.equals("2") || avaliacao.equals("1")) {
                totalAvaliacoesNegativas++;
            }

            // Verifica se a avaliação é 3 estrelas (avaliação neutra)
            if (avaliacao.equals("3")) {
                totalAvaliacoesNeutras++;
            }
        }

        // Envia a mensagem se houver avaliações de 5 estrelas
        if (totalCincoEstrelas > 0) {
            String alertaPositivo = "🎉 Sucesso: Recebemos " + totalCincoEstrelas + " avaliação(s) de 5 estrelas!";
            slack.sendMessage(alertaPositivo);
        }

        // Envia a mensagem se houver avaliações negativas (1 ou 2 estrelas)
        if (totalAvaliacoesNegativas > 0) {
            String alertaNegativo = "🚨 Alerta: Recebemos " + totalAvaliacoesNegativas + " avaliação(ões) negativa(s)!";
            slack.sendMessage(alertaNegativo);
        }

        // Envia a mensagem se houver avaliações neutras (3 estrelas)
        if (totalAvaliacoesNeutras > 0) {
            String alertaNeutra = "🤔 Neutro: Recebemos " + totalAvaliacoesNeutras + " avaliação(ões) neutra(s)!";
            slack.sendMessage(alertaNeutra);
        }

        // Exibe a mensagem final com base nas comparações de quantidade
        if (totalAvaliacoesNegativas > totalAvaliacoesNeutras && totalAvaliacoesNegativas > totalCincoEstrelas) {
            String mensagemFinal =
                    "⚠️ Cuidado, temos muitas avaliações negativas por aqui, que tal acessar a Dashboard para algumas recomendações de melhoria?";
            slack.sendMessage(mensagemFinal);
        } else if (totalAvaliacoesNeutras > totalCincoEstrelas && totalAvaliacoesNeutras > totalAvaliacoesNegativas) {
            String mensagemFinal = "👌 Tudo parcialmente bem até o momento, que tal acessar a Dashboards para algumas recomendações de melhoria e alcançar maiores notas?";
            slack.sendMessage(mensagemFinal);
        } else if (totalCincoEstrelas > totalAvaliacoesNegativas && totalCincoEstrelas > totalAvaliacoesNeutras) {
            String mensagemFinal = "🎉 Uau, seu negócio tem muitas avaliações boas, continue assim para que ainda tenha ótimas impressões!";
            slack.sendMessage(mensagemFinal);
        }
    }



    public String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return sdf.format(new Date());
    }

    public List<?> toList(Iterator<?> iterator) {
        return IteratorUtils.toList(iterator);
    }

    public void imprimirRecomendacao(List<Categoria> categorias){
        categorias.forEach(System.out::println);
    }

    public void imprimirPorIndice(List<Feedback_POI> feedbacks, int indice) {
        System.out.println("\n========== Imprimindo feedback por índice ==========\n");
        if (indice >= 0 && indice < feedbacks.size()) {
            System.out.println(feedbacks.get(indice));
        } else {
            System.out.println("Índice inválido.");
        }
        System.out.println("\n========== Fim da impressão por índice ==========");
    }
}
