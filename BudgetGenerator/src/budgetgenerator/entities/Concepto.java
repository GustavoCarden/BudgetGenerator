package budgetgenerator.entities;

/**
 *
 * @author Gustavo
 */
public class Concepto {

    private String descripcionConcepto;
    private Integer cantidad;
    private Double costoUnitario;
    private Double costoTotal;

    public Concepto() {
    }

    public Concepto(String descripcionConcepto, Integer cantidad, Double costoUnitario, Double costoTotal) {
        this.descripcionConcepto = descripcionConcepto;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
        this.costoTotal = costoTotal;
    }

    public String getDescripcionConcepto() {
        return descripcionConcepto;
    }

    public void setDescripcionConcepto(String descripcionConcepto) {
        this.descripcionConcepto = descripcionConcepto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Double getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(Double costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public Double getCostoTotal() {
        return costoTotal;
    }

    public void setCostoTotal(Double costoTotal) {
        this.costoTotal = costoTotal;
    }

}
