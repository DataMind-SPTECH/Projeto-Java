package datamind;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import datamind.Feedback_POI;
import datamind.GerenciadorFeedbacks;
import datamind.TratacaoDeDados;
import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.ResponseTransformer;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException  {
        Main app = new Main();

        app.connectionBucket();

        app.setupDatabase();

        app.runFeedbackManager();

    }

    public String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return sdf.format(new Date());
    }

    private void connectionBucket(){
        S3Client s3Client = new S3Provider().getS3Client();
        String bucketName = System.getenv("NAME_BUCKET");

        String date = getCurrentTimestamp();
        System.out.println("========== Iniciando Conexão com o bucket " + date + " ==========");
        List<S3Object> objects = null;
        try {
            ListObjectsRequest listObjects = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .build();

            objects = s3Client.listObjects(listObjects).contents();

            if (objects.isEmpty()) {
                System.out.println("O bucket está vazio. Realizando upload de um novo arquivo...");
                uploadFile(s3Client, bucketName);
                objects = s3Client.listObjects(listObjects).contents();
                downloadFiles(s3Client, bucketName, objects);
            } else {
                System.out.println("Objetos no bucket " + bucketName + ":");
                for (S3Object object : objects) {
                    System.out.println("- " + object.key());
                }
                downloadFiles(s3Client, bucketName, objects);
            }
        } catch (S3Exception e) {
            System.err.println("Erro ao listar objetos no bucket: " + e.getMessage());
        }
    }

    private void uploadFile(S3Client s3Client, String bucketName) {
        try {
            String uniqueFileName = "DataSet-McDonalds.xlsx";
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .build();

            File file = new File("src\\main\\feedbacks_categorizados.xlsx");
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));

            System.out.println("Arquivo '" + file.getName() + "' enviado com sucesso com o nome: " + uniqueFileName);
        } catch (S3Exception e) {
            System.err.println("Erro ao fazer upload do arquivo ou arquivo já existente: " + e.getMessage());
        }
    }

    private void downloadFiles(S3Client s3Client, String bucketName, List<S3Object> objects) {
        try {
            for (S3Object object : objects) {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(object.key())
                        .build();

                InputStream inputStream = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());
                Files.copy(inputStream, new File(object.key()).toPath());
                System.out.println("Arquivo baixado: " + object.key());
            }
        } catch (IOException | S3Exception e) {
            System.err.println("Erro ao fazer download dos arquivos: " + e.getMessage());
        }
    }

    private void runFeedbackManager() throws IOException, InterruptedException {
        GerenciadorFeedbacks gerenciadorFeedbacks = new GerenciadorFeedbacks();
        List<Feedback_POI> feedbacks = gerenciadorFeedbacks.criar();
        TratacaoDeDados TratadorDeDados = new TratacaoDeDados();
        TratacaoDeDados.inserirFiliais(feedbacks);
        List<Feedback_POI> dadosTratados = TratadorDeDados.processarDados(feedbacks);
        TratacaoDeDados.inserindoDadosNoBanco(dadosTratados);
      
        gerenciadorFeedbacks.verificarENotificar(dadosTratados);

        TratacaoDeDados.gerarRecomendacoes();

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        System.out.println("\n========== Iniciando geração de palavras chaves "+ sdf.format(new Date()) +" ==========");
        TratacaoDeDados.gerarPalavrasChavesPositivas();
        Thread.sleep(5000);
        TratacaoDeDados.gerarPalavrasChavesNeutras();
        Thread.sleep(5000);
        TratacaoDeDados.gerarPalavrasChavesNegativas();
        Thread.sleep(5000);
        System.out.println("\n========== Geração de palavras chaves finalizada "+ sdf.format(new Date()) +" ==========");
    }

    private void setupDatabase() {
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        connection.update("INSERT IGNORE INTO dataset(idDataset, url, nome, descricao) VALUES (?, ?, ?, ?)",1, "http://dataset1.com", "Mc Donald's dados", "dados do Mc Donald's");

        connection.update("INSERT IGNORE INTO empresa (idEmpresa, nomeEmpresa, cnpj, fkDataset) VALUES (?, ?, ?, ?, ?);", 1, "McDonald's", "12345678000195", 1);

        connection.update("INSERT IGNORE INTO cargo (idCargo, cargo) VALUES (?, ?);", 1, "Gerente");
        connection.update("INSERT IGNORE INTO cargo (idCargo, cargo) VALUES (?, ?);", 2, "Analista");
        connection.update("INSERT IGNORE INTO cargo (idCargo, cargo) VALUES (?, ?);", 3, "Desenvolvedor");

        connection.update("INSERT IGNORE INTO funcionario (idFuncionario, nome, email, senha, telefone, cpf, fkEmpresa, fkCargo) VALUES (?, ?, ?, ?, ?, ?, ?, ?);", 1, "Henrique", "henrique@gmail.com", "12345678", null, "12345678901", 1, 1);

        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 1, "Qualidade do produto");
        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 2, "Atendimento");
        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 3, "Tempo de espera");
        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 4, "Experiência do drive thru");
        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 5, "Experiência geral");

    }
}