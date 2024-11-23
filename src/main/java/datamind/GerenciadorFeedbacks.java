package datamind;

import lombok.Cleanup;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
        System.out.println("Abrindo arquivo Excel...");
        @Cleanup FileInputStream file = new FileInputStream("DataSet-McDonalds.xlsx");
        /*
        Para Facilitar teste, não excluir
        @Cleanup FileInputStream file = new FileInputStream("C:\\Users\\User\\Documents\\SPTech\\2 Semestre\\PI\\Projeto-Java\\src\\main\\resources\\Feedbacks McDonalds (50).xlsx");
         */
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

            // Atribui os valores para a classe Feedback_POI
            Feedback_POI feedback = Feedback_POI.builder()
                    .Id((int) cells.get(0).getNumericCellValue())
                    .Nome(cells.get(1).getStringCellValue())
                    .Categoria(cells.get(2).getStringCellValue())
                    .Endereco(cells.get(3).getStringCellValue())
                    .Latitude((int) cells.get(4).getNumericCellValue())
                    .Longitude((int) cells.get(5).getNumericCellValue())
                    .Rating_count(String.valueOf(cells.get(6).getNumericCellValue()))
                    .Tempo_Feedback(cells.get(7).getStringCellValue())
                    .Comentario(cells.get(8).getStringCellValue())
                    .Avaliacao(cells.get(9).getStringCellValue())
                    .build();

            feedbacks.add(feedback);
        });

        System.out.println("========== Criação de feedbacks concluída "+ getCurrentTimestamp() + " ==========");
        System.out.println("Total de feedbacks criados: " + feedbacks.size() + "\n");
        return feedbacks;
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
