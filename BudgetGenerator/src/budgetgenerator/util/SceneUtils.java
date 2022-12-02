package budgetgenerator.util;

import budgetgenerator.entities.Concepto;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

/**
 *
 * @author Gustavo
 */
public class SceneUtils {

    public static HBox getButton(String id, String label, EventHandler listener, Pos aligment_rule) {
        Button button = new Button();
        button.setId(id);
        button.setText(label);
        button.setOnAction(listener);
        HBox hb = new HBox();
        hb.getChildren().addAll(button);
        hb.setSpacing(10);
        hb.setAlignment(aligment_rule);
        return hb;
    }

    public static HBox getLabel(String text, Pos aligment_rule) {
        Label label1 = new Label(text);
        //TextField textField = new TextField();
        HBox hb = new HBox();
        hb.getChildren().addAll(label1);
        hb.setSpacing(10);
        hb.setAlignment(aligment_rule);
        return hb;
    }

    public static HBox getTextFieldInHbox(Pos aligment_rule) {
        //Label label1 = new Label(text);
        TextField textField = new TextField();
        HBox hb = new HBox();
        hb.getChildren().addAll(textField);
        hb.setSpacing(10);
        hb.setAlignment(aligment_rule);
        return hb;
    }

    public static TextField getNumberTextField() {
        //Label label1 = new Label(text);
        TextField textField = new TextField();
        textField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                    String newValue) {
                if (!newValue.matches("\\d*")) {
                    textField.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });
        return textField;
    }

    public static TextField getDecimalTextField() {
        //Label label1 = new Label(text);
        TextField textField = new TextField();
        textField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d*(\\.\\d*)?")) {
                    textField.setText(oldValue);
                }
            }
        });
        return textField;
    }

    public static HBox getTextAreaInHbox(Pos aligment_rule) {
        //Creating a pagination
        TextArea area = new TextArea();
        //Setting number of pages
        area.setPrefColumnCount(50);
        area.setPrefHeight(120);
        //Creating a hbox to hold the pagination
        HBox hbox = new HBox();
        hbox.setSpacing(10);
        hbox.setAlignment(aligment_rule);
        hbox.getChildren().addAll(area);
        return hbox;
    }

    public static TableView getTableView(String[] columnNames, PropertyValueFactory<Concepto, String>[] values) {
        TableView tableView = new TableView();
        tableView.setMinSize(900, 400);
        TableColumn tableColumn;
        int i = 0;
        for (String columnName : columnNames) {
            tableColumn = new TableColumn(columnName);
            if (columnName.equals("Concepto")) {
                tableColumn.setMinWidth(450);
            } else {
                tableColumn.setMinWidth(150);
            }
            tableColumn.setCellValueFactory(values[i]);
            i++;
            tableView.getColumns().add(tableColumn);
        }

        return tableView;
    }
}
