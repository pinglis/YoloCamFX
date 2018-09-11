# YoloCamFX
YoloCamFX is JavaFX Application that shows your webcam (if you have one) in a window and then runs either the default Yolo or Tiny Yolo implementation from dl4j to detect objects in the stream.

Each detected object is highlighted with a bounding box and marked with its label and confidence values.

You can select to run either yolo or tiny yolo from dl4j and the confidence threshold.

Yolo can return duplicate predictions for the same object so to remove that non-max suppression algorithm taken from [here](https://dzone.com/articles/java-autonomous-driving-car-detection-1).

This project is an extension of [yolo-dl4j](https://github.com/jesuino/java-ml-projects/tree/master/utilities/yolo-dl4j) from user jesunino but updated to support webcams.