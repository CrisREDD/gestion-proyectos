module uptc {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens uptc to javafx.fxml;
    opens uptc.ui to javafx.fxml;
    exports uptc;
    exports uptc.ui;
}
