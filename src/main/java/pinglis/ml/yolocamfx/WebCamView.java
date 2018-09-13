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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Extension of a standard ImageView class where the image is set by your 
 * webcam.
 */
public class WebCamView 
        extends ImageView
{
    private final ObjectProperty<BufferedImage> imageBufferProperty = new SimpleObjectProperty<>();
    private final BooleanProperty pausedProperty = new SimpleBooleanProperty();
    private WebCamTask task;
 
    public WebCamView()
    {
        this.imageBufferProperty.addListener((a,b,c)->{
            setImage(SwingFXUtils.toFXImage(c,null));
        });
    }
    
    /**
     * A property containing a copy of the current Image object but in 
     * BufferedImage format
     * @return ObjectProperty
     */
    public ObjectProperty<BufferedImage> imageBufferProperty()
    {
        return imageBufferProperty;
    }
    
    /**
     * If true, the image will not be updated
     * @return BooleanProperty
     */
    public BooleanProperty pausedProperty()
    {
        return this.pausedProperty;
    }
    
    /**
     * The the background thread which grabs the input from your webcam
     * and updates the view
     */
    public void start()
    {
        task = new WebCamTask(this);
        task.start();
    }
    
    /**
     * Close the stream from the webcam
     */
    public void close()
    {
        if (task !=null)
        {
            task.close();
            task=null;
        }
    }
    
    @Override
    public double minWidth(double height)
    {
        return 40;
    }

    @Override
    public double prefWidth(double height)
    {
        Image I=getImage();
        if (I==null) return minWidth(height);
        return I.getWidth();
    }

    @Override
    public double maxWidth(double height)
    {
        return 16384;
    }

    @Override
    public double minHeight(double width)
    {
        return 40;
    }

    @Override
    public double prefHeight(double width)
    {
        Image I=getImage();
        if (I==null) return minHeight(width);
        return I.getHeight();
    }

    @Override
    public double maxHeight(double width)
    {
        return 16384;
    }

    @Override
    public boolean isResizable()
    {
        return true;
    }

    @Override
    public void resize(double width, double height)
    {
        setFitWidth(width);
        setFitHeight(height);
    }
}
