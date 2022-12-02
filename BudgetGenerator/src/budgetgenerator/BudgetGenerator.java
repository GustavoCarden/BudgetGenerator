package budgetgenerator;

import budgetgenerator.entities.Concepto;
import budgetgenerator.entities.Data;
import budgetgenerator.pdf.BIUtils;
import budgetgenerator.util.SceneUtils;
import budgetgenerator.util.XMLUtils;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Gustavo
 */
public class BudgetGenerator extends Application implements EventHandler<Event> {

    private String pathFile = null;
    private TableView<Concepto> table = null;
    private TextField marcaVehiculo = null,
            anioVehiculo = null,
            concepto = null,
            cantidad = null,
            costoUnitario = null,
            subtotal = null,
            iva = null,
            total = null;

    private TextArea descripcion = null,
            observacion = null;

    boolean update = true;

    @Override
    public void start(Stage primaryStage) throws FileNotFoundException, IOException {
        //Creating a Grid Pane 

        GridPane gridPane = new GridPane();
        //Setting size for the pane 
        gridPane.setMinSize(600, 600);

        //Setting the padding  
        gridPane.setPadding(new Insets(10, 10, 10, 10));

        //Setting the vertical and horizontal gaps between the columns 
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        //Setting the Grid alignment 
        gridPane.setAlignment(Pos.BASELINE_LEFT);
        gridPane.add(SceneUtils.getLabel("Marca Vehiculo", Pos.BASELINE_LEFT), 0, 0);
        marcaVehiculo = new TextField();
        gridPane.add(marcaVehiculo, 1, 0);

        gridPane.add(SceneUtils.getLabel("Año Vehiculo", Pos.BASELINE_LEFT), 0, 1);
        anioVehiculo = SceneUtils.getNumberTextField();
        gridPane.add(anioVehiculo, 1, 1);

        gridPane.add(SceneUtils.getLabel("Descripción del procedimiento", Pos.BASELINE_LEFT), 0, 2);
        descripcion = new TextArea();
        descripcion.setPrefRowCount(2);
        gridPane.add(descripcion, 1, 2, 5, 1);

        gridPane.add(SceneUtils.getLabel("Observaciones Adicionales", Pos.BASELINE_LEFT), 0, 3);
        observacion = new TextArea();
        observacion.setPrefRowCount(2);
        gridPane.add(observacion, 1, 3, 5, 1);

        gridPane.add(SceneUtils.getLabel("Agregar Concepto:", Pos.BASELINE_LEFT), 0, 4);

        concepto = new TextField();
        concepto.setPromptText("Concepto:");
        gridPane.add(concepto, 0, 5);

        cantidad = SceneUtils.getNumberTextField();
        cantidad.setPromptText("Cantidad:");
        gridPane.add(cantidad, 1, 5);

        costoUnitario = SceneUtils.getDecimalTextField();
        costoUnitario.setPromptText("Costo Unitario:");
        gridPane.add(costoUnitario, 2, 5);

        gridPane.add(SceneUtils.getButton("Agregar", "Agregar", this, Pos.BASELINE_RIGHT), 4, 5, 1, 1);

        PropertyValueFactory<Concepto, String>[] values = new PropertyValueFactory[4];
        values[0] = new PropertyValueFactory("descripcionConcepto");
        values[1] = new PropertyValueFactory("cantidad");
        values[2] = new PropertyValueFactory("costoUnitario");
        values[3] = new PropertyValueFactory("costoTotal");
        table = SceneUtils.getTableView(new String[]{"Concepto", "Cantidad", "Costo Unitario", "Costo Total"}, values);
        gridPane.add(table, 0, 6, 5, 1);
        gridPane.add(SceneUtils.getButton("Borrar", "Borrar", this, Pos.BASELINE_RIGHT), 0, 7, 5, 1);

        gridPane.add(SceneUtils.getLabel("Subtotal:", Pos.BASELINE_RIGHT), 0, 8, 5, 1);
        subtotal = SceneUtils.getDecimalTextField();
        gridPane.add(subtotal, 5, 8, 1, 1);
        gridPane.add(SceneUtils.getLabel("Iva:", Pos.BASELINE_RIGHT), 0, 9, 5, 1);
        iva = SceneUtils.getDecimalTextField();
        iva.textProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != "") {
                update = false;
                calculate(table);
            }
        });
        gridPane.add(iva, 5, 9, 1, 1);
        gridPane.add(SceneUtils.getLabel("Total:", Pos.BASELINE_RIGHT), 0, 10, 5, 1);
        total = SceneUtils.getDecimalTextField();
        gridPane.add(total, 5, 10, 1, 1);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(gridPane);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);

        gridPane.add(SceneUtils.getButton("Generar", "Generar Presupuesto", this, Pos.BASELINE_RIGHT), 0, 11, 6, 1);
        Scene scene = new Scene(scrollPane, 1200, 600);
        primaryStage.setTitle("Generar Presupuesto");
        primaryStage.getIcons().add(new Image(BudgetGenerator.class.getResourceAsStream("images/icon.png")));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        launch(args);
    }

    @Override
    public void handle(Event event) {
        Node node = (Node) event.getSource();
        update = true;
        Concepto concepto;
        switch (node.getId()) {
            case "Agregar":
                if (this.concepto.getText() != null && !this.concepto.getText().isEmpty()
                        && this.cantidad.getText() != null && !this.cantidad.getText().isEmpty()
                        && this.costoUnitario.getText() != null && !this.costoUnitario.getText().isEmpty()) {
                    Integer cantidad = Integer.valueOf(this.cantidad.getText());
                    Double costoUnitario = Double.valueOf(this.costoUnitario.getText());
                    Double costoTotal = costoUnitario * cantidad.doubleValue();
                    concepto = new Concepto(this.concepto.getText(), cantidad, costoUnitario, costoTotal);
                    table.getItems().add(concepto);

                    calculate(table);

                    this.concepto.setText("");
                    this.cantidad.setText("");
                    this.costoUnitario.setText("");
                } else {
                    Alert alert = new Alert(AlertType.WARNING);
                    alert.setTitle("Advertencia");
                    alert.setHeaderText("Información incompleta.");
                    alert.setContentText("Para agregar un concepto es necesario capturar todos los campos.");
                    alert.showAndWait();
                }
                break;
            case "Borrar":
                concepto = table.getSelectionModel().getSelectedItem();
                if (concepto != null) {
                    Alert alert = new Alert(AlertType.CONFIRMATION);
                    alert.setTitle("Borrar Datos");
                    alert.setContentText("¿Estas seguro que deseas eliminar el concepto " + concepto.getDescripcionConcepto() + "?");

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() == ButtonType.OK) {
                        table.getItems().remove(concepto);
                        calculate(table);
                    } else {
                        alert.close();
                    }
                } else {
                    Alert alert = new Alert(AlertType.WARNING);
                    alert.setTitle("Advertencia");
                    alert.setContentText("Para poder borrar algun dato es necesario seleccionarlo primero.");
                    alert.showAndWait();
                }
                break;
            case "Generar":
                boolean exception = false;
                Exception e = null;
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Generando");
                alert.setContentText("El presupuesto se esta generando...");
                alert.show();
                
                try {
                    Data data = new Data();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                    data.setFechaElaboracion(dateFormat.format(new Date()));
                    data.setMarcaVehiculo(marcaVehiculo.getText());
                    data.setAnioVehiculo(anioVehiculo.getText());
                    data.setDescripcion(descripcion.getText());
                    data.setObservaciones(observacion.getText());
                    data.setConceptos(table.getItems());
                    data.setSubtotal(Double.valueOf(subtotal.getText()));
                    data.setIva(Double.valueOf(iva.getText()));
                    data.setTotal(Double.valueOf(total.getText()));

                    InputStream isRtf = BudgetGenerator.class.getResourceAsStream("templates/Plantilla.rtf");

                    String folderDirectory = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "Presupuestos";
                    File theDir = new File(folderDirectory);
                    if (!theDir.exists()) {
                        theDir.mkdirs();
                    }

                    byte[] pdfBytes = BIUtils.createPDF(XMLUtils.mapObjectInXML(data), isRtf);
                    pathFile = folderDirectory + File.separator + "Presupuesto_" + marcaVehiculo.getText().toUpperCase() + "_" + dateFormat.format(new Date()).replace("/", "-") + ".pdf";
                    File document = new File(pathFile);
                    try ( FileOutputStream out = new FileOutputStream(document)) {
                        IOUtils.copy(new ByteArrayInputStream(pdfBytes), out);
                        createMargin(pdfBytes);
                    } catch (IOException ex) {
                        exception = true;
                        e = ex;
                    }
                } catch (Exception ex) {
                    exception = true;
                    e = ex;
                }

                alert.close();
                
                alert = new Alert(exception ? AlertType.ERROR : AlertType.INFORMATION);
                alert.setTitle(exception ? "Error" : "Información");
                alert.setContentText(exception ? e.getMessage() : "El presupuesto se ha creado con exito en la carpeta Escritorio.");
                alert.showAndWait();
        }
    }

    private void calculate(TableView<Concepto> table) {
        Double subtotal = 0.0,
                iva = 0.0,
                total = 0.0;
        for (Concepto cp : table.getItems()) {
            subtotal += cp.getCostoTotal();
        }
        if (update) {
            iva = subtotal * 0.16;
        } else {
            if (this.iva.getText() != null && !this.iva.getText().isEmpty()) {
                iva = Double.valueOf(this.iva.getText());
            } else {
                iva = 0.0;
            }
        }
        total = subtotal + iva;

        this.subtotal.setText(subtotal.toString());
        this.iva.setText(iva.toString());
        this.total.setText(total.toString());
    }

    private void createMargin(byte[] fileBytes) throws FileNotFoundException, DocumentException, IOException {
        PdfReader reader = new PdfReader(fileBytes);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(pathFile));
        //loop on pages (1-based)
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {

            // get object for writing over the existing content;
            // you can also use getUnderContent for writing in the bottom layer
            PdfContentByte over = stamper.getOverContent(i);

            over.setRGBColorStroke(0x00, 0x00, 0x00);
            over.rectangle(30, 30, reader.getPageSize(i).getWidth() - 60, reader.getPageSize(i).getHeight() - 60);
            over.setLineWidth(0.5f);
            over.stroke();
        }

        stamper.close();
    }
}
