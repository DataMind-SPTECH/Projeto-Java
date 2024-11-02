package datamind;

import java.util.ArrayList;
import java.util.List;

public class Cargo {

    private Integer idCargo;
    private String cargo;
    private List<Funcionario> funcionarios = new ArrayList<>();


    public Cargo() {
    }

    public Cargo(Integer idCargo, String cargo, Funcionario funcionarios) {
        this.idCargo = idCargo;
        this.cargo = cargo;
        this.funcionarios.add(funcionarios);
    }

    public Integer getIdCargo() {
        return idCargo;
    }

    public void setIdCargo(Integer idCargo) {
        this.idCargo = idCargo;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public List<Funcionario> getFuncionarios() {
        return funcionarios;
    }

    public void setFuncionarios(List<Funcionario> funcionarios) {
        this.funcionarios = funcionarios;
    }
}
