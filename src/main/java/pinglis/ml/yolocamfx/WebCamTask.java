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

import com.github.sarxos.webcam.Webcam;
import java.awt.image.BufferedImage;
import javafx.concurrent.Task;

/**
 * Task that captures input from your default webcam
 */
public class WebCamTask 
        extends Task<Void>
{
    private final WebCamView view;
    private Webcam webCam = null;
    private boolean stopCamera = false;
    
    public WebCamTask(WebCamView view)
    {
        this.view = view;    
    }
    
    @Override
    protected Void call() 
            throws Exception
    {
        if (webCam != null)
        {
            close();
        }
        webCam = Webcam.getDefault();
                
        if ( webCam != null )
        {
            stopCamera = false;
            webCam.setViewSize(webCam.getViewSizes()[webCam.getViewSizes().length - 1]);
            webCam.open();
                    
            while (!stopCamera)
            {
                try
                {
                    BufferedImage grabbedImageBuffer=null;
                            
                    if ((grabbedImageBuffer = webCam.getImage()) != null)
                    {
                        if ( !view.pausedProperty().get() )
                        {
                            view.imageBufferProperty().set(grabbedImageBuffer);
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
                
        return null;
    }
    
    public void close()
    {
        if (webCam != null)
        {
            webCam.close();
        }
        stopCamera=true;
    }   
}
