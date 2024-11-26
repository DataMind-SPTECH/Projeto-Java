package datamind;

import lombok.Builder;
import lombok.Data;

@Data
@Builder

public class Feedback_POI {
    private Integer Id;
    private String Nome;
    private String Categoria;
    private String Endereco;
    private String Latitude;
    private String Longitude;
    private String Rating_count;
    private String Tempo_Feedback;
    private String Comentario;
    private String Avaliacao;

    // Construtor que aceita apenas Comentario, Avaliacao como número e endereco
    public Feedback_POI(String comentario, Integer avaliacao, String Endereco) {
        this.Comentario = comentario;
        this.Avaliacao = (avaliacao != null) ? String.valueOf(avaliacao) : null;
        this.Endereco = Endereco;
    }

    // Adicione um construtor padrão (opcional) para os outros campos
    public Feedback_POI(Integer id, String nome, String categoria, String endereco, String latitude,
                        String longitude, String rating_count, String tempo_feedback, String comentario,
                        String avaliacao) {
        this.Id = id;
        this.Nome = nome;
        this.Categoria = categoria;
        this.Endereco = endereco;
        this.Latitude = latitude;
        this.Longitude = longitude;
        this.Rating_count = rating_count;
        this.Tempo_Feedback = tempo_feedback;
        this.Comentario = comentario;
        this.Avaliacao = avaliacao;
    }

}

