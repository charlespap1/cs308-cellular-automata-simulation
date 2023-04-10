package cellsociety;

import cellsociety.backend.gridstructures.GridStructure;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.HashMap;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * SimulationMenu is used to run the entire application. It has a start-up display and tracks the time for the
 * simulations. It is used to SimulationUI classes, which hold each simulation (including concurrent ones). The
 * application generates simulations by creating new SimulationUI classes.
 */
public class SimulationMenu extends Application {

    public static final int FRAMES_PER_SECOND = 60;
    public static final int MILLISECOND_DELAY = 1000 / FRAMES_PER_SECOND;

    public static final String RESOURCE_FOLDER = "/resources/"; // [hard coded because not locale-specific]
    public static final String STYLESHEET = "default.css";
    public static final String XML_FOLDER = "./data/";
    public static final Color DISPLAY_COLOR = Color.color(0.9, 0.9, 1.0);
    public static final Color FONT_COLOR = Color.color(0.0, 0.0, 0.4);
    public static final int FONT_SIZE = 16;
    public static final int PADDING = 5;
    public static final int V_GAP = 10;
    public static final int H_GAP = 50;
    public static final int BOX_WIDTH = 100;
    public static final int TOTAL_WIDTH = 1000;
    public static final int TOTAL_HEIGHT = 500;
    private static final double DEFAULT_NODE_SPACING = 12;
    private static final int BUTTON_SPACING = 4;

    private String shape = "SQUARE";
    private boolean isTorus = false;

    private Simulation currSimulation;
    private Stage stage;
    private Scene display;
    private GridPane topLevelGrid;

    private XMLParser fileParser;
    private String XMLFilename;
    private TextField filenameField;
    private StackPane myInfoBox;
    private ToggleGroup myShapeButtons;
    private ToggleGroup myTorusButtons;
    private ChoiceBox<String> neighborhoodBox;
    private int neighborhoodType = 8;

    HashMap<SimulationUI, Stage> runningSims;

    Locale locale = Locale.ENGLISH;
    ResourceBundle textElements = ResourceBundle.getBundle("resources.TextElements", locale);

    enum SimulationType {
        LIFE, FIRE, PERCOLATION, SEGREGATION, PRED_PREY, RPS
    }


    /**
     * The start method is used to kick-off the animation by setting up key variables and calling
     * initializeUI(), which sets up all of the UI Controls within the display.
     * It sets up the Timeline, too, which calls the step() function at the end of each frame.
     * @param stage
     */
    @Override
    public void start (Stage stage) {
        runningSims = new HashMap<>();
        fileParser = new XMLParser();
        this.stage = stage;

        initializeUI();

        Text title = new Text(textElements.getString("title"));
        title.setId("title-text");

        topLevelGrid.add(title, 1, 0);
        this.stage.setScene(display);
        this.stage.setTitle(textElements.getString("title"));
        this.stage.show();

        KeyFrame frame = new KeyFrame(Duration.millis(MILLISECOND_DELAY), e -> step());
        Timeline animation = new Timeline();
        animation.setCycleCount(Timeline.INDEFINITE);
        animation.getKeyFrames().add(frame);
        animation.play();
    }

    private void initializeUI() {
        Group root = new Group();
        topLevelGrid = initializePane(); // topLevelGrid is the highest level grid

        topLevelGrid.add(setupFilenameField(), 1, 1);
        topLevelGrid.add(createInfoBox(), 2, 1);
        topLevelGrid.add(setupShapeButtons(), 1, 2);
        topLevelGrid.add(setupNeighborhoodBox(), 2, 2);
        topLevelGrid.add(setupTorusButtons(), 2, 3);

        root.getChildren().add(topLevelGrid);
        display = new Scene(root, TOTAL_WIDTH, TOTAL_HEIGHT, DISPLAY_COLOR);

        String stylesheet = String.format("%s%s", RESOURCE_FOLDER, STYLESHEET);
        System.out.println(stylesheet);
        display.getStylesheets().add(getClass().getResource(stylesheet).toExternalForm());
        myInfoBox.getChildren().add(new Label("FILLER"));
        addMessage(myInfoBox, textElements.getString("defaultInfoboxMessage"));
    }

    private GridPane initializePane() {
        GridPane pane = new GridPane();
        pane.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));
        pane.setVgap(V_GAP);
        pane.setHgap(H_GAP);
        return pane;
    }

    private VBox setupFilenameField() {
        VBox holder = new VBox(DEFAULT_NODE_SPACING);
        Text text = createPrompt(textElements.getString("filenamePrompt"));

        filenameField = new TextField();
        Button loadButton = new Button("Load XML File");
        loadButton.setOnAction(event -> loadButton());

        holder.getChildren().addAll(text, filenameField, loadButton);
        return holder;
    }

    private VBox setupNeighborhoodBox() {
        VBox holder = new VBox(DEFAULT_NODE_SPACING);
        Text text = createPrompt(textElements.getString("neighborhoodPrompt"));

        neighborhoodBox = new ChoiceBox<>();
        neighborhoodBox.getItems().addAll("3", "4", "6", "8", "9", "12");
        neighborhoodBox.getSelectionModel().select(0);
        holder.getChildren().addAll(text, neighborhoodBox);
        return holder;
    }

    private VBox setupShapeButtons() {
        VBox holder = new VBox(BUTTON_SPACING);
        Text text = createPrompt(textElements.getString("shapePrompt"));
        holder.getChildren().add(text);

        ToggleGroup tg = new ToggleGroup();
        myShapeButtons = tg;

        String[] shapeArray = new String[]{textElements.getString("square"),
                textElements.getString("diamond"), textElements.getString("triangle"),
                textElements.getString("hexagon"), textElements.getString("circle")};
        for (String shape: shapeArray){
            RadioButton button = new RadioButton(shape);
            button.setUserData(shape);
            button.setToggleGroup(tg);
            button.setOnAction(event -> shapeButton());
            holder.getChildren().add(button);
        }
        return holder;
    }

    private VBox setupTorusButtons() {
        VBox holder = new VBox(BUTTON_SPACING);
        Text text = createPrompt(textElements.getString("torusPrompt"));
        holder.getChildren().add(text);

        ToggleGroup tg = new ToggleGroup();
        myTorusButtons = tg;

        String[] torusArray = new String[]{textElements.getString("torus"),
                textElements.getString("noTorus")};
        for (String option: torusArray){
            RadioButton button = new RadioButton(option);
            button.setUserData(option);
            button.setToggleGroup(tg);
            button.setOnAction(event -> torusButton());
            holder.getChildren().add(button);
        }
        return holder;
    }

    private StackPane createInfoBox() {
        StackPane infoBox = createMessageBox(BOX_WIDTH);
        myInfoBox = infoBox;
        return infoBox;
    }

    private StackPane createMessageBox(int size) {
        StackPane sp = new StackPane();
        Rectangle background = new Rectangle(size*2, size, Color.WHITE);
        background.setArcWidth(BOX_WIDTH/10);
        background.setArcHeight(BOX_WIDTH/10);
        sp.getChildren().add(background);
        return sp;
    }

    private Text createPrompt(String prompt) {
        Text text = new Text(prompt);
        text.setFont(new Font(textElements.getString("font"), FONT_SIZE));
        text.setFill(FONT_COLOR);
        return text;
    }

    private void shapeButton() {
        shape = myShapeButtons.getSelectedToggle().getUserData().toString();
    }

    private void torusButton() {
        isTorus = (myTorusButtons.getSelectedToggle().getUserData().toString()
                == textElements.getString("torus"));
    }

    private void loadButton() {
        String val = neighborhoodBox.getValue();
        neighborhoodType = Integer.parseInt(val);
        if (isValidNeighborhood()) {
            try {
                XMLFilename = String.format("%s%s", XML_FOLDER, filenameField.getText());
                generateSimulation();
                addMessage(myInfoBox, textElements.getString("defaultInfoboxMessage"));
            } catch (Exception e) {
                addMessage(myInfoBox, textElements.getString("fileErrorMessage"));
            }
        } else {
            addMessage(myInfoBox, textElements.getString("neighborhoodErrorMessage"));
        }
    }

    private void generateSimulation() {
        GridStructure gs = fileParser.generateGrid(XMLFilename, isTorus, neighborhoodType);
        Stage stageForNewSim = new Stage();
        currSimulation = new Simulation(gs, shape);
        SimulationUI sim = new SimulationUI(currSimulation,stageForNewSim);
        runningSims.put(sim,stageForNewSim);
    }

    private void addMessage (Pane messageBox, String message) {
        Label l = new Label(message);
        l.setId("message-box");
        l.setWrapText(true);
        l.setMaxWidth(BOX_WIDTH*1.5);
        if (messageBox.getChildren().size() > 0) {
            messageBox.getChildren().remove(1);
        }
        messageBox.getChildren().add(l);
    }

    private boolean isValidNeighborhood() {
        if (shape.equals("SQUARE")) {
            return (neighborhoodType==4 || neighborhoodType==8);
        } else if (shape.equals("DIAMOND")) {
            return (neighborhoodType==4 || neighborhoodType==8);
        } else if (shape.equals("HEXAGON")) {
            return (neighborhoodType==3 || neighborhoodType==6);
        } else if (shape.equals("TRIANGLE")) {
            return (neighborhoodType==3 || neighborhoodType==9 || neighborhoodType==12);
        } else {
            // "CIRCLE" allows the user to test any of the neighborhood types and see what happens ...
            return true;
        }
    }

    private void step() {
        for (SimulationUI currSim : runningSims.keySet()){
            currSim.step();
        }
    }

    /**
     * Main is used to run the application.
     * @param args
     */
    public static void main(String[] args){
        launch(args);
    }
}
