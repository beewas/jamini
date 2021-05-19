import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.List;

public class View extends Application {
    GeminiSession session = new GeminiSession(1965);

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private final double height = screenSize.getHeight() - 100;
    private final double width = screenSize.getWidth() - 50;

    private final double baseFontSize = 20.0;
    private final double subSubHeadingFontSize = baseFontSize * 1.2;
    private final double subHeadingFontSize = subSubHeadingFontSize * 1.4;
    private final double headingFontSize = subHeadingFontSize * 1.4;
    private final double backSize = headingFontSize * 2;

    private final String baseFont = "file:src/main/resources/iosevka-curly-regular.ttf";

    private final String background = "#fbf1c7";
    private final String foreground = "#3c3836";
    private final String headingColor = "#98971a";
    private final String subHeadingColor = "#79740e";
    private final String textFieldColor = "#ebdbb2";
    private final String enteredButton = "#d5c4a1";
    private final String links = "#076678";
    private final String enteredLinks = "#458588";

    private final String mainStyle = "-fx-background-color: " + background + ";-fx-foreground-color:" + foreground;

    public void start(Stage stage) {
        initStage(stage);
    }

    private void initStage(Stage s) {
        s.setTitle("jamini");
        s.setHeight(height);
        s.setWidth(width);

        VBox site = new VBox();
        site.setStyle(mainStyle);

        ScrollPane content = new ScrollPane(site);
        content.setFitToWidth(true);
        content.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        content.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        content.setStyle(mainStyle);

        TextField tf = new TextField();
        tf.setFont(Font.loadFont(baseFont, 25.0));
        tf.setAlignment(Pos.CENTER);
        tf.setStyle("-fx-background-color:" + textFieldColor);
        tf.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getText().equals("\r")) {
                initPane(tf.getText(), site, tf);
                content.setVvalue(0);
            }
        });

        Button backButton = new Button("<");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setMaxHeight(Double.MAX_VALUE);
        backButton.setStyle(mainStyle);
        backButton.setFont(Font.loadFont(baseFont, backSize));
        backButton.setOnAction(e -> initPane(session.getPrevSite(), site, tf));
        backButton.setOnMouseEntered(e -> backButton.setStyle("-fx-background-color: " + enteredButton));
        backButton.setOnMouseExited(e -> backButton.setStyle("-fx-background-color: " + background));

        GridPane gp = new GridPane();
        gp.setStyle(mainStyle);

        ColumnConstraints first = new ColumnConstraints();
        first.setPercentWidth(20);
        ColumnConstraints second = new ColumnConstraints();
        second.setPercentWidth(60);
        ColumnConstraints third = new ColumnConstraints();
        third.setPercentWidth(20);
        gp.getColumnConstraints().addAll(first, second, third);

        RowConstraints top = new RowConstraints();
        top.setPercentHeight(10);
        RowConstraints middle = new RowConstraints();
        middle.setPercentHeight(90);
        gp.getRowConstraints().addAll(top, middle);

        gp.add(tf, 1, 0);
        gp.add(backButton, 0, 0, 1,2);
        gp.add(content, 1, 1);

        GridPane.setVgrow(backButton, Priority.ALWAYS);
        GridPane.setHgrow(backButton, Priority.ALWAYS);
        GridPane.setMargin(backButton,new Insets(0.0, 20.0, 0.0, 0.0));

        initPane("gemini://gemini.circumlunar.space/", site, tf);

        Scene scene = new Scene(gp, Paint.valueOf(background));
        s.setScene(scene);
        s.widthProperty().addListener(e -> reflowContent(site, s.getWidth()));
        s.show();
    }

    private void reflowContent(final VBox box, final double stageWidth){
        for(var t : box.getChildren()) {
            ((Text) t).setWrappingWidth(stageWidth*0.59);
        }
    }

    private void initPane(final String address, final VBox v, final TextField textField) {
        v.getChildren().clear();
        List<String> lines = session.getContent(address);
        textField.setText(session.getCurrentAddress());
        for (String l : lines) {
            if (l.startsWith("# ")) {
                Text t = new Text(l.substring(2));
                t.setFont(Font.loadFont(baseFont, headingFontSize));
                t.setStyle("-fx-fill: " + headingColor);
                t.setTextAlignment(TextAlignment.CENTER);
                v.getChildren().add(t);
            }
            else if (l.startsWith("## ")) {
                Text t = new Text(l.substring(3));
                t.setStyle("-fx-fill: " + subHeadingColor);
                t.setFont(Font.loadFont(baseFont, subHeadingFontSize));
                v.getChildren().add(t);
            }
            else if (l.startsWith("### ")) {
                Text t = new Text(l.substring(4));
                t.setFont(Font.loadFont(baseFont, subSubHeadingFontSize));
                v.getChildren().add(t);
            }
            else if (l.startsWith("=>")) {
                createLink(v, l.substring(2).trim(), textField);
            }
            else {
                Text t = new Text(l);
                t.setWrappingWidth(width*0.59);
                t.setFont(Font.loadFont(baseFont, baseFontSize));
                v.getChildren().add(t);
            }
        }
    }

    private void createLink(final VBox v, final String line, final TextField textField) {
        String[] splittedLine = line.split("\\s+");
        StringBuilder sb = new StringBuilder();
        if (line.startsWith("https://") || line.startsWith("http://") || line.startsWith("gopher://"))
            sb.append("[ANOTHER PROTOCOL!] ");
        for (int i = 1; i < splittedLine.length; i++) {
            sb.append(splittedLine[i]).append(" ");
        }
        Text btn = new Text(sb.toString());

        btn.setOnMouseClicked(e -> initPane(line.split("\\s+")[0], v, textField));
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-fill:" + enteredLinks));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-fill:" + links));
        btn.setFont(Font.loadFont(baseFont, baseFontSize));
        btn.setStyle("-fx-fill: " + links);
        v.getChildren().add(btn);
    }

    public static void main(String[] args) {
        launch();
    }
}
