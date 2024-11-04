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
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.ResponseTransformer;

public class Main {

    public static void main(String[] args) throws IOException {
        Main app = new Main();

        // Função para conectar no bucket
        app.connectionBucket();

        // Função para gerenciar conexão e criar tabelas
        app.setupDatabase();

        // Função para gerenciar feedbacks
        app.runFeedbackManager();

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
                    cep CHAR(8),
                    rua VARCHAR(45),
                    bairro VARCHAR(45),
                    complemento VARCHAR(45),
                    cidade VARCHAR(20),
                    estado VARCHAR(20),
                    numero VARCHAR(5),
                    fkDataset INT,
                    fkMatriz INT,
                    FOREIGN KEY (fkDataset) REFERENCES dataset(idDataset),
                    FOREIGN KEY (fkMatriz) REFERENCES empresa(idEmpresa)
                );
                """);

        // Inserindo dados da empresa
        connection.update("INSERT IGNORE INTO empresa (idEmpresa, nomeEmpresa, cnpj, cep, rua, bairro, complemento, cidade, estado, numero, fkDataset, fkMatriz) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", 1, "Mc Donalds", "42591651000143" , "06333272", "Avenida Paulista", "Cerqueira Cesar", "Andar 1", "São Paulo", "São Paulo", "1000", 1, null);

        connection.execute("""
                CREATE TABLE IF NOT EXISTS cargo (
                    idCargo INT PRIMARY KEY AUTO_INCREMENT,
                    nomeCargo VARCHAR(45)
                );
                """);

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

        connection.execute("""
                CREATE TABLE IF NOT EXISTS categoria (
                    idCategoria INT PRIMARY KEY AUTO_INCREMENT,
                    descricao VARCHAR(45)
                );
                """);

        //Inserindo categorias
        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 1, "Velocidade do Drive-Thru");
        connection.update("INSERT IGNORE INTO categoria (idCategoria, descricao) VALUES (?, ?);", 2, "Lanche frio");

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
                    descricao VARCHAR(2000),
                    rating INT,
                    fkEmpresa INT,
                    fkCategoria INT,
                    FOREIGN KEY (fkEmpresa) REFERENCES empresa(idEmpresa),
                    FOREIGN KEY (fkCategoria) REFERENCES categoria(idCategoria)
                );
                """);

        // Inserindo cargo de exemplo
        connection.update("INSERT IGNORE INTO cargo (idCargo, nomeCargo) VALUES (?, ?);", 1, "Responsável Legal");
    }
}