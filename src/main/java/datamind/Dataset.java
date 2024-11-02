package datamind;

import java.util.ArrayList;
import java.util.List;

public class Dataset {

    private Integer idDataset;
    private String url;
    private String nome;
    private String descricao;
    private List<Empresa> empresas = new ArrayList<>();

    public Dataset() {
    }

    public Dataset(Integer idDataset, String url, String nome, String descricao, Empresa empresas) {
        this.idDataset = idDataset;
        this.url = url;
        this.nome = nome;
        this.descricao = descricao;
        this.empresas.add(empresas);
    }

    public Integer getIdDataset() {
        return idDataset;
    }

    public void setIdDataset(Integer idDataset) {
        this.idDataset = idDataset;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public List<Empresa> getEmpresas() {
        return empresas;
    }

    public void setEmpresas(List<Empresa> empresas) {
        this.empresas = empresas;
    }
}
