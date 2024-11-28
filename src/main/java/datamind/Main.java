package datamind;

//import org.springframework.jdbc.core.JdbcTemplate;
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

        // Função para conectar no bucket
        //app.connectionBucket();

        // Função para gerenciar conexão e criar tabelas
        app.setupDatabase();

        // Função para gerenciar feedbacks
        app.runFeedbackManager();

        // Envio de mensagem do slack
       GerenciadorFeedbacks gerenciador = new GerenciadorFeedbacks();
        List<Feedback_POI> feedbacks = gerenciador.criar(); // Carrega os feedbacks do XLSX

        // Verifica e notifica eventos ao Slack
        gerenciador.verificarENotificar(feedbacks);

    }

    public String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return sdf.format(new Date());
    }

    private void connectionBucket(){
        S3Client s3Client = new S3Provider().getS3Client();
        String bucketName = System.getenv("NAME_BUCKET");

        // Listando objetos do bucket
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

            File file = new File("src\\main\\resources\\Feedbacks McDonalds (50).xlsx");
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

    private void runFeedbackManager() throws IOException {
        GerenciadorFeedbacks gerenciadorFeedbacks = new GerenciadorFeedbacks();
        List<Feedback_POI> feedbacks = gerenciadorFeedbacks.criar();
        TratacaoDeDados TratadorDeDados = new TratacaoDeDados();
        TratacaoDeDados.inserirFiliais(feedbacks);
        List<Feedback_POI> dadosTratados = TratadorDeDados.processarDados(feedbacks);
        TratacaoDeDados.inserindoDadosNoBanco(dadosTratados);
    }

    private void setupDatabase() {
        // Realizando conexão
        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        // Criando tabelas no banco de dados
        connection.execute("""
                CREATE TABLE IF NOT EXISTS dataset (
                    idDataset INT PRIMARY KEY AUTO_INCREMENT,
                    url VARCHAR(400),
                    nome VARCHAR(45),
                    descricao VARCHAR(50)
                );
                """);
        connection.update("INSERT IGNORE INTO dataset(idDataset, url, nome, descricao) VALUES (?, ?, ?, ?)",1, "datasetmac.com", "Mc Donald's dados", "50 feedbacks");

        connection.execute("""
                CREATE TABLE IF NOT EXISTS empresa (
                    idEmpresa INT PRIMARY KEY AUTO_INCREMENT,
                    nomeEmpresa VARCHAR(45),
                    cnpj CHAR(14),
                    urlFoto VARCHAR(264),
                    fkDataset INT,
                    FOREIGN KEY (fkDataset) REFERENCES dataset(idDataset)
                );
                """);

        // Inserindo dados da empresa
        connection.update("INSERT IGNORE INTO empresa (idEmpresa, nomeEmpresa, cnpj, urlfoto, fkDataset) VALUES (?, ?, ?, ?, ?);", 1, "McDonald's", "42591651000143" , null, 1);


        connection.execute("""
                CREATE TABLE IF NOT EXISTS filial (
                    idFilial INT PRIMARY KEY AUTO_INCREMENT,
                    nome VARCHAR (200),
                    endereco VARCHAR(150),
                    latitude VARCHAR(45),
                    longitude VARCHAR(45),
                    fkEmpresa INT,
                    FOREIGN KEY (fkEmpresa) REFERENCES Empresa(idEmpresa)
                );
                """);

        connection.execute("""
                CREATE TABLE IF NOT EXISTS cargo (
                    idCargo INT PRIMARY KEY AUTO_INCREMENT,
                    cargo VARCHAR(45)
                );
                """);
        connection.update("INSERT IGNORE INTO cargo (idCargo, cargo) VALUES (?, ?);", 1, "Responsável Legal");

        connection.execute("""
                CREATE TABLE IF NOT EXISTS funcionario (
                    idFuncionario INT PRIMARY KEY AUTO_INCREMENT,
                    nome VARCHAR(45),
                    email VARCHAR(45),
                    senha VARCHAR(20),
                    telefone CHAR(11),
                    cpf CHAR(11),
                    fkEmpresa INT,
                    fkCargo INT,
                    FOREIGN KEY (fkEmpresa) REFERENCES empresa(idEmpresa),
                    FOREIGN KEY (fkCargo) REFERENCES cargo(idCargo)
                );
                """);
        connection.update("INSERT IGNORE INTO funcionario (idFuncionario, nome, email, senha, telefone, cpf, fkEmpresa, fkCargo) VALUES (?, ?, ?, ?, ?, ?, ?, ?);", 1, "Henrique", "henrique@gmail.com", "12345678", null, "12345678901", 1, 1);

        connection.execute("""
                CREATE TABLE IF NOT EXISTS categoria (
                    idCategoria INT PRIMARY KEY AUTO_INCREMENT,
                    descricao VARCHAR(45)
                );
                """);

        //Inserindo categorias
        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 1, "Qualidade do produto");
        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 2, "Atendimento");
        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 3, "Tempo de espera");
        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 4, "Experiência do drive thru");
        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 5, "Experiência geral");

        connection.execute("""
                CREATE TABLE IF NOT EXISTS recomendacoesIA (
                    idRecomendacao INT PRIMARY KEY AUTO_INCREMENT,
                    descricao VARCHAR(500),
                  	dtCriacao DATE,
                    fkCategoria INT,
                  	FOREIGN KEY (fkCategoria) REFERENCES categoria(idCategoria)
                );
                """);

        //Inserindo recomendação
        connection.update("INSERT IGNORE INTO recomendacoesIA (idRecomendacao, descricao, dtCriacao, fkCategoria) VALUES (?, ?, ?, ?);", 1, "Você poderia redistribuir os funcionários conforme a demanda do Drive-Thru", "2024-10-29", 1);
        connection.update("INSERT IGNORE INTO recomendacoesIA (idRecomendacao, descricao, dtCriacao, fkCategoria) VALUES (?, ?, ?, ?);", 2, "Você poderia acelerar a montagem do lanche para que ele não esfrie", "2024-10-30", 2);


        connection.execute("""
                CREATE TABLE IF NOT EXISTS feedback (
                    idFeedback INT PRIMARY KEY AUTO_INCREMENT,
                    descricao VARCHAR(10000),
                    rating INT,
                    fkFilial INT,
                    fkCategoria INT,
                    FOREIGN KEY (fkFilial) REFERENCES filial(idfilial),
                    FOREIGN KEY (fkCategoria) REFERENCES categoria(idCategoria)
                );
                """);
    }
}