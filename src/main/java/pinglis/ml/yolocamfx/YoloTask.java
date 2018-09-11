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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2RGB;
import org.datavec.image.loader.Java2DNativeImageLoader;
import org.datavec.image.transform.ColorConversionTransform;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.layers.objdetect.DetectedObject;
import org.deeplearning4j.nn.layers.objdetect.Yolo2OutputLayer;
import org.deeplearning4j.zoo.model.TinyYOLO;
import org.deeplearning4j.zoo.model.YOLO2;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;

/**
 * 
 */
public class YoloTask
{
    public final int INPUT_WIDTH = 416;
    public final int INPUT_HEIGHT = 416;
    public final int INPUT_CHANNELS = 3;
    public final int GRID_W = 13;
    public final int GRID_H = 13;
    
    private static final String[] TINY_CLASSES = {
        "Aeroplane", "Bicycle", "Bird", 
        "Boat", "Bottle", "Bus", 
        "Car", "Cat", "Chair", 
        "Cow", "Diningtable", "Dog", 
        "Horse", "Motorbike", "Person", 
        "Pottedplant", "Sheep", "Sofa",
        "Train", "TV" 
    };
    
    private final String[] CLASSES =
    {
        "Person", "Bicycle", "Car", 
        "Motorbike", "Aeroplane", "Bus", 
        "Train", "Truck", "Boat", 
        "Traffic light", "Fire hydrant", "Stop sign", 
        "Parking meter", "Bench", "Bird", 
        "Cat", "Dog", "Horse", 
        "Sheep", "Cow", "Elephant", 
        "Bear", "Zebra", "Giraffe", 
        "Backpack", "Umbrella", "Handbag",
        "Tie", "Suitcase", "Frisbee", 
        "Skis", "Snowboard", "Sports ball", 
        "Kite", "Baseball bat", "Baseball glove",
        "Skateboard", "Surfboard", "Tennis racket", 
        "Bottle", "Wine glass", "Cup", 
        "Fork", "Knife", "Spoon", 
        "Bowl", "Banana", "Apple", 
        "Sandwich", "Orange", "Broccoli", 
        "Carrot", "Hot dog", "Pizza", 
        "Donut", "Cake", "Chair",
        "Sofa", "Potted plant", "Bed", 
        "Dining Table", "Toilet", "TV", 
        "Laptop", "Mouse", "Remote", 
        "Keyboard", "Mobile phone", "Microwave", 
        "Oven", "Toaster", "Sink", 
        "Refrigerator", "Book", "Clock", 
        "Vase", "Scissors", "Teddy bear", 
        "Hair drier", "Toothbrush"
    };
    
    private final ObjectProperty<BufferedImage> imageBufferProperty = new SimpleObjectProperty<>();
    private final DoubleProperty thresholdProperty = new SimpleDoubleProperty();
    private String[] classes;
    private Map<String, Paint> colors = new HashMap<>();
    private ComputationGraph graph;
    private Java2DNativeImageLoader bufferLoader;
    private List<DetectedObject> detectedObjects;
    private ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(0, 1);
    private boolean stop;
    
    public ObjectProperty<BufferedImage> imageBufferProperty()
    {
        return imageBufferProperty;
    }
    
    public DoubleProperty thresholdProperty()
    {
        return this.thresholdProperty;
    }
    
    public List<DetectedObject> getDetectedObjects()
    {
        return detectedObjects;
    }
    
    public String getClass(int forPrediction)
    {
        return classes[forPrediction];
    }
    
    public Paint getColor(String forClass)
    {
        return colors.get(forClass);
    }
    
    public void close()
    {
        stop = true;
    }
    
    public void start(boolean isTiny)
    {
        Task<Void> task = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                stop = false;
                try
                {
                    if (isTiny)
                    {
                        graph = (ComputationGraph) TinyYOLO.builder().build().initPretrained();                      
                        bufferLoader = new Java2DNativeImageLoader(INPUT_WIDTH, INPUT_HEIGHT, INPUT_CHANNELS, new ColorConversionTransform(COLOR_BGR2RGB));
                        classes = TINY_CLASSES;
                    }
                    else
                    {
                        graph = (ComputationGraph) YOLO2.builder().build().initPretrained();
                        bufferLoader = new Java2DNativeImageLoader(INPUT_WIDTH, INPUT_HEIGHT, INPUT_CHANNELS, new ColorConversionTransform(COLOR_BGR2RGB));
                        classes = CLASSES;
                    }
                }
                catch (IOException e)
                {
                    throw new Error("Not able to init the model", e);
                }

                for (int i = 0; i < classes.length; i++)
                {
                    colors.put(classes[i], Color.hsb((i + 1) * 20, 0.5, 1.0));
                }
                while (!stop)
                {
                    BufferedImage imageBuffer = imageBufferProperty.get();
                    double threshold = thresholdProperty.get();
                    
                    if ( imageBuffer != null )
                    {
                        detectedObjects = detect(imageBuffer, threshold);
                    }
                    else
                    {
                        detectedObjects = null;
                    }
                }
                return null;
            }
        };

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private List<DetectedObject> detect(BufferedImage buffer, double threshold)
    {
        List<DetectedObject> predictions = null;
        
        try
        {
            INDArray img = this.bufferLoader.asMatrix(buffer);
            
            scaler.transform(img);
            
            INDArray output = graph.outputSingle(img);
            Yolo2OutputLayer outputLayer = (Yolo2OutputLayer) graph.getOutputLayer(0);
        
            predictions = filterDuplicates(outputLayer.getPredictedObjects(output, threshold));
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
        }
        
        return predictions;
    }
    
    /**
     * Remove the duplicates from the predictions. Taken from: 
     * 
     * https://github.com/klevis/AutonomousDriving
     * 
     * @param predictedObjects
     * @return List of DetectedObject
     */
    private static List<DetectedObject> filterDuplicates(List<DetectedObject> predictedObjects) 
    {
        if (predictedObjects == null) 
        {
            return new ArrayList<>();
        }
        List<DetectedObject> detectedObjects = new ArrayList<>();

        while (!predictedObjects.isEmpty()) 
        {
            Optional<DetectedObject> max = predictedObjects.stream().max((o1, o2) -> ((Double) o1.getConfidence()).compareTo(o2.getConfidence()));
            
            if (max.isPresent()) 
            {
                DetectedObject maxObjectDetect = max.get();
                removeObjectsIntersectingWithMax(predictedObjects, maxObjectDetect);
                detectedObjects.add(maxObjectDetect);
            }
        }
        return detectedObjects;
    }

    private static void removeObjectsIntersectingWithMax(List<DetectedObject> predictedObjects, DetectedObject maxObjectDetect) 
    {
        double[] bottomRightXY1 = maxObjectDetect.getBottomRightXY();
        double[] topLeftXY1 = maxObjectDetect.getTopLeftXY();
        List<DetectedObject> removeIntersectingObjects = new ArrayList<>();
        
        for (DetectedObject detectedObject : predictedObjects)
        {
            double[] topLeftXY = detectedObject.getTopLeftXY();
            double[] bottomRightXY = detectedObject.getBottomRightXY();
            double iox1 = Math.max(topLeftXY[0], topLeftXY1[0]);
            double ioy1 = Math.max(topLeftXY[1], topLeftXY1[1]);

            double iox2 = Math.min(bottomRightXY[0], bottomRightXY1[0]);
            double ioy2 = Math.min(bottomRightXY[1], bottomRightXY1[1]);

            double inter_area = (ioy2 - ioy1) * (iox2 - iox1);

            double box1_area = (bottomRightXY1[1] - topLeftXY1[1]) * (bottomRightXY1[0] - topLeftXY1[0]);
            double box2_area = (bottomRightXY[1] - topLeftXY[1]) * (bottomRightXY[0] - topLeftXY[0]);

            double union_area = box1_area + box2_area - inter_area;
            double iou = inter_area / union_area;

            if (iou > 0.5) 
            {
                removeIntersectingObjects.add(detectedObject);
            }
        }
        predictedObjects.removeAll(removeIntersectingObjects);
    }
}
