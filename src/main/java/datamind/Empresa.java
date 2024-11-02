package datamind;

import java.util.ArrayList;
import java.util.List;


public class Empresa {

    private Integer idEmpresa;
    private String nomeEmpresa;
    private String cnpj;
    private String cep;
    private String rua;
    private String bairro;
    private String complemento;
    private String cidade;
    private String estado;
    private String numero;
    private Empresa matriz;
    private List<Funcionario> funcionarios = new ArrayList<>();
    private List<Feedback> feedbacks = new ArrayList<>();
    private Dataset dataset;

    public Empresa(String s) {
    }

    public Empresa(Integer idEmpresa, String nomeEmpresa, String cnpj, String cep, String rua, String bairro, String complemento, String cidade, String estado, String numero, Empresa matriz, Funcionario funcionarios, Feedback feedbacks, Dataset dataset) {
        this.idEmpresa = idEmpresa;
        this.nomeEmpresa = nomeEmpresa;
        this.cnpj = cnpj;
        this.cep = cep;
        this.rua = rua;
        this.bairro = bairro;
        this.complemento = complemento;
        this.cidade = cidade;
        this.estado = estado;
        this.numero = numero;
        this.matriz = matriz;
        this.funcionarios.add(funcionarios);
        this.feedbacks.add(feedbacks);
        this.dataset = dataset;
    }

    public Integer getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Integer idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getRua() {
        return rua;
    }

    public void setRua(String rua) {
        this.rua = rua;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Empresa getMatriz() {
        return matriz;
    }

    public void setMatriz(Empresa matriz) {
        this.matriz = matriz;
    }

    public List<Funcionario> getFuncionarios() {
        return funcionarios;
    }

    public void setFuncionarios(List<Funcionario> funcionarios) {
        this.funcionarios = funcionarios;
    }

    public List<Feedback> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(List<Feedback> feedbacks) {
        this.feedbacks = feedbacks;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }
}
