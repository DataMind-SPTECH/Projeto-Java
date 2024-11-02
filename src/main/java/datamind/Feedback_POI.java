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
    private Integer Latitude;
    private Integer Longitude;
    private String Rating_count;
    private String Tempo_Feedback;
    private String Comentario;
    private String Avaliacao;

    // Construtor que aceita apenas Comentario e Avaliacao como número
    public Feedback_POI(String comentario, Integer avaliacao) {
        this.Comentario = comentario;
        this.Avaliacao = (avaliacao != null) ? String.valueOf(avaliacao) : null;
    }

    // Adicione um construtor padrão (opcional) para os outros campos
    public Feedback_POI(Integer id, String nome, String categoria, String endereco, Integer latitude,
                        Integer longitude, String rating_count, String tempo_feedback, String comentario,
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

