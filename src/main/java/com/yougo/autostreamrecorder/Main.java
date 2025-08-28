package com.yougo.autostreamrecorder;

import com.yougo.autostreamrecorder.ui.AddChannelDialog;
import java.util.Optional;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Main extends Application {
    
    private TableView<ChannelEntry> channelTable;
    private ObservableList<ChannelEntry> channelList;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("AutoStreamRecorder");
        
        // Create main layout
        BorderPane root = new BorderPane();
        
        // Create top toolbar
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Create channel table
        channelTable = createChannelTable();
        root.setCenter(channelTable);
        
        // Create scene
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #f0f0f0;");
        
        Button addChannelBtn = new Button("Add Channel");
        Button settingsBtn = new Button("Settings");
        Button removeChannelBtn = new Button("Remove Selected");
        
        // Add button actions (placeholder for now)
        addChannelBtn.setOnAction(e -> showAddChannelDialog());
        settingsBtn.setOnAction(e -> showSettingsDialog());
        removeChannelBtn.setOnAction(e -> removeSelectedChannel());
        
        toolbar.getChildren().addAll(addChannelBtn, removeChannelBtn, new Separator(), settingsBtn);
        return toolbar;
    }
    
    private TableView<ChannelEntry> createChannelTable() {
        TableView<ChannelEntry> table = new TableView<>();
        
        // Create columns
        TableColumn<ChannelEntry, String> platformCol = new TableColumn<>("Platform");
        platformCol.setCellValueFactory(new PropertyValueFactory<>("platform"));
        platformCol.setPrefWidth(80);
        
        TableColumn<ChannelEntry, String> nameCol = new TableColumn<>("Channel Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("channelName"));
        nameCol.setPrefWidth(150);
        
        TableColumn<ChannelEntry, String> urlCol = new TableColumn<>("Channel URL");
        urlCol.setCellValueFactory(new PropertyValueFactory<>("channelUrl"));
        urlCol.setPrefWidth(250);
        
        TableColumn<ChannelEntry, Boolean> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("isActive"));
        activeCol.setPrefWidth(60);
        
        TableColumn<ChannelEntry, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(80);
        
        TableColumn<ChannelEntry, String> qualityCol = new TableColumn<>("Quality");
        qualityCol.setCellValueFactory(new PropertyValueFactory<>("quality"));
        qualityCol.setPrefWidth(80);
        
        // Add columns to table
        table.getColumns().addAll(platformCol, nameCol, urlCol, activeCol, statusCol, qualityCol);
        
        // Initialize data
        channelList = FXCollections.observableArrayList();
        table.setItems(channelList);
        
        // Add some sample data for testing
        addSampleData();
        
        return table;
    }
    
    private void addSampleData() {
        channelList.add(new ChannelEntry("YouTube", "ExampleChannel", "https://youtube.com/@example/live", true, "Offline", "best"));
        channelList.add(new ChannelEntry("Twitch", "TestStreamer", "https://twitch.tv/teststreamer", true, "Online", "720p"));
    }
    
    private void showAddChannelDialog() {
        Optional<ChannelEntry> result = AddChannelDialog.showDialog();
        result.ifPresent(channelEntry -> {
            channelList.add(channelEntry);
        });
    }
    
    private void showSettingsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText("Settings Dialog");
        alert.setContentText("This dialog will be implemented later.");
        alert.showAndWait();
    }
    
    private void removeSelectedChannel() {
        ChannelEntry selected = channelTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            channelList.remove(selected);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No channel selected");
            alert.setContentText("Please select a channel to remove.");
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}