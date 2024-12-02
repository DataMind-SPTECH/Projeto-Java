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

        System.out.println("Abrindo arquivo Excel...");
        @Cleanup FileInputStream file = new FileInputStream("DataSet-McDonalds.xlsx");

        Workbook workbook = new XSSFWorkbook(file);

        System.out.println("Carregando primeira aba da planilha...");
        Sheet sheet = workbook.getSheetAt(0);

        System.out.println("Lendo linhas da planilha...\n");
        List<Row> rows = (List<Row>) toList(sheet.iterator());

        rows.remove(0);

        rows.forEach(row -> {

            List<Cell> cells = (List<Cell>) toList(row.cellIterator());

            if (cells.size() < 11) {
                return;
            }

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
            return "";
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return String.valueOf(cell.getNumericCellValue());
            case BLANK:
                return "";
            default:
                throw new IllegalStateException("Tipo de célula inesperado: " + cell.getCellType());
        }
    }

    public void verificarENotificar(List<Feedback_POI> feedbacks) throws IOException, InterruptedException {
        Slack slack = new Slack(System.getenv("LINK_SLACK"));

        System.out.println("\n========== Iniciando envio de notificação "+ getCurrentTimestamp() +" ==========");
        Integer totalCincoEstrelas = 0;
        Integer totalAvaliacoesNegativas = 0;
        Integer totalAvaliacoesNeutras = 0;

        for (Feedback_POI feedback : feedbacks) {
            String avaliacao = feedback.getAvaliacao();

            if (avaliacao.equals("5")) {
                totalCincoEstrelas++;
            }

            if (avaliacao.equals("2") || avaliacao.equals("1")) {
                totalAvaliacoesNegativas++;
            }

            if (avaliacao.equals("3")) {
                totalAvaliacoesNeutras++;
            }
        }

        if (totalCincoEstrelas > 0) {
            String alertaPositivo = "🎉 Sucesso: Recebemos " + totalCincoEstrelas + " avaliação(s) de 5 estrelas!";
            slack.sendMessage(alertaPositivo);
        }

        if (totalAvaliacoesNegativas > 0) {
            String alertaNegativo = "🚨 Alerta: Recebemos " + totalAvaliacoesNegativas + " avaliação(ões) negativa(s)!";
            slack.sendMessage(alertaNegativo);
        }

        if (totalAvaliacoesNeutras > 0) {
            String alertaNeutra = "🤔 Neutro: Recebemos " + totalAvaliacoesNeutras + " avaliação(ões) neutra(s)!";
            slack.sendMessage(alertaNeutra);
        }

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

        System.out.println("\n========== Envio de notificação finalizada "+ getCurrentTimestamp() +" ==========");
    }



    public String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return sdf.format(new Date());
    }

    public List<?> toList(Iterator<?> iterator) {
        return IteratorUtils.toList(iterator);
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
