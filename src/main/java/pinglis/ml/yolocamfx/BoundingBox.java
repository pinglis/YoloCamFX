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

import javafx.scene.paint.Paint;

/**
 * This object represents the bounding box of a class detected in an image
 */
public class BoundingBox
{
    private final String label;
    private final double confidence;
    private final Paint paint;
    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;

    public BoundingBox(String label, double confidence, Paint paint, double x1, double y1, double x2, double y2)
    {
        this.label = label;
        this.confidence = confidence;
        this.paint = paint;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public String getLabel()
    {
        return label;
    }

    public double getConfidence()
    {
        return confidence;
    }

    public Paint getPaint()
    {
        return paint;
    }

    public double getX1()
    {
        return x1;
    }

    public double getY1()
    {
        return y1;
    }
    
    public double getX2()
    {
        return this.x2;
    }
    
    public double getY2()
    {
        return this.y2;
    }
}
