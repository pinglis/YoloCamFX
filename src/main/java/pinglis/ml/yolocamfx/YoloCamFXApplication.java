/*
 * Copyright 2018 pinglis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pinglis.ml.yolocamfx;

import java.io.IOException;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;
import org.deeplearning4j.nn.layers.objdetect.DetectedObject;

/**
 * Main entry point for the YoloCamFX project. 
 * <p>
 * This is JavaFX Application that shows your webcam (if you have one) in a
 * window and then runs either the default Yolo or Tiny Yolo implementation from
 * dl4j and draws what it finds as bounding boxes over the image from your
 * web cam.
 * <p>
 * The application create a basic UI with some controls and then starts two
 * background threads, one to capture the webcam and display it on the screen
 * and another to run the yolo neural network
 * <p>
 * The two background threads run independently of each other. This means that
 * the webcam and yolo net work at different frames per second rates but as the
 * yolo algorithm is generally slower the webcam capture rate that allows you
 * to have smooth playback from the webcam.
 * <p>
 * You can select whether to run either the Yolo or tiny Yolo network. Note,
 * the tiny yolo network is much faster and is the default.
 * <p>
 * On first run, the application will download the model and weights for the
 * neural networks and that can take some time.
 */
public class YoloCamFXApplication
        extends Application
{
    private static final float DEFAULT_THRESHOLD = 0.45f;
    private static final ObservableList<String> MODELS = FXCollections.observableArrayList("Tiny Yolo", "YOLO");
    private Scene scene;
    
    /**
     * Start the application
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) 
            throws IOException
    {
        launch();
    }

    /**
     * Set up the UI and start the webcam and yolo tasks.
     * 
     * @param stage
     * @throws Exception 
     */
    @Override
    public void start(Stage stage) throws Exception
    {   
        // The UI comprises of a borderpane with a central pane displaying the
        // webcam and a bottom pane with some basic controls
        BorderPane root = new BorderPane();
        
        // Create the webcam view and start it running
        WebCamView camView = new WebCamView();
        camView.start();
        
        // Create a canvas for the yolo to draw on
        Canvas canvas = new Canvas();
        canvas.widthProperty().bind(camView.fitWidthProperty());
        canvas.heightProperty().bind(camView.fitHeightProperty());
        
        // Put them both in a stackpane with the canvas on top
        StackPane stack = new StackPane(camView, canvas);
        root.setCenter(stack);
        
        // Create a combo to select the model to run
        ComboBox modelCombo = new ComboBox();
        modelCombo.setItems(MODELS);
        modelCombo.getSelectionModel().select(0);
        
        // Create a slider to allow the user to control the confidence threshold
        Slider sldThreshold = new Slider(0.1f, 1.0f, DEFAULT_THRESHOLD);
        sldThreshold.setShowTickLabels(true);
        sldThreshold.setMajorTickUnit(0.1);
        sldThreshold.setBlockIncrement(0.01);
        sldThreshold.setShowTickMarks(true);
        
        // Create drop down to allow the user to filter duplicates
        ComboBox filterCombo = new ComboBox();
        filterCombo.setItems(FXCollections.observableArrayList(true, false));
        filterCombo.getSelectionModel().select(0);
        
        // Create pause button
        ToggleButton pauseButton = new ToggleButton();
        pauseButton.setText("Pause Camera");
        camView.pausedProperty().bind(pauseButton.selectedProperty());
        
        // Add the controls to the bottom pane
        HBox hbBottom = new HBox(10, 
                new Label("Model:"), modelCombo, 
                new Label("Filter duplicates:"), filterCombo,
                new Label("Confidence Threshold: "), sldThreshold,
                pauseButton
        );
        hbBottom.setAlignment(Pos.CENTER);
        root.setBottom(hbBottom);
        
        // Layout the scene
        scene = new Scene(root);
        stage.setMinWidth(900);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.setTitle("YoloWebCamFX");
        stage.show();
        
        // Start the yolo algorithm running using the default selected model
        YoloTask yolo = new YoloTask();
        yolo.imageBufferProperty().bind(camView.imageBufferProperty());
        yolo.thresholdProperty().bind(sldThreshold.valueProperty());
        yolo.start(modelCombo.getSelectionModel().getSelectedIndex() == 0);
        yolo.filterProperty().bind(filterCombo.valueProperty());
        
        // Monitor the combo and if select changes, change the yolo model running
        modelCombo.getSelectionModel().selectedIndexProperty().addListener((a,b,c)->{
            yolo.close();
            yolo.start(c.intValue()==0);
        });
        
        // Start a animation timer to draw the current bounding boxes on the screen
        AnimationTimer timer = new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {          
                drawBoxes(yolo, canvas);
            }
        };
        timer.start();
    }
    
    /**
     * Draw the current predictions from the yolo application on the given
     * canvas with their scores
     * @param yolo
     * @param canvas 
     */
    private void drawBoxes(YoloTask yolo, Canvas canvas)
    {
        GraphicsContext ctx = canvas.getGraphicsContext2D();
        ctx.setLineWidth(1);
        ctx.setFill(Color.WHITE);
        ctx.setTextAlign(TextAlignment.LEFT);
            
        double h = canvas.getHeight();
        double w = canvas.getWidth();
        List<BoundingBox> detectedBoxes = yolo.getDetectedBoxes();
        
        // First clear the canvas of the last image we wrote to it
        ctx.clearRect(0, 0, w, h);
        
        // If the yolo has found some objects
        if ( detectedBoxes != null )
        {
            // For detected object
            for (BoundingBox box : detectedBoxes)
            {
                int x1 = (int)Math.max(0, Math.round(box.getX1() * w));
                int y1 = (int)Math.max(15, Math.round(box.getY1() * h));
                int x2 = (int)Math.min(w-1, Math.round(box.getX2() * w));
                int y2 = (int)Math.min(h-1, Math.round(box.getY2() * h));
                
                int rectW = x2 - x1;
                int rectH = y2 - y1;
                
                int tx = x1;
                int ty = y1 - 2;
                        
                ctx.setLineWidth(2);
                ctx.setStroke(box.getPaint());
                ctx.strokeRect(x1, y1, rectW, rectH);
                
                ctx.setLineWidth(1);
                String text = String.format("%s [%.2f%%]", box.getLabel(), box.getConfidence());
                ctx.strokeText(text, tx, ty);
                ctx.fillText(text, tx, ty);
            }
        }
    }
}
