package datamind;

public class Feedback {
    private Integer idFeedback;
    private String descricao;
    private Integer rating;
    private Empresa empresa;
    private Categoria categoria;

    public Feedback() {
    }

    public Feedback(Integer idFeedback, String descricao, Integer rating, Empresa empresa, Categoria categoria) {
        this.idFeedback = idFeedback;
        this.descricao = descricao;
        this.rating = rating;
        this.empresa = empresa;
        this.categoria = categoria;
    }

    public Integer getIdFeedback() {
        return idFeedback;
    }

    public void setIdFeedback(Integer idFeedback) {
        this.idFeedback = idFeedback;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
}
