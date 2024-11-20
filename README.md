# Image Coloring project


## Table of Contents
1. [Introduction](#introduction)
* Overview of Physical Concurrency Demonstration
* Single-Threaded Mode
* Multi-Threaded Mode  
2. [Overview](#overview) 
* Program Implementation
* Subprograms Overview 
3. [Detailed Implementation](#explanation)  
* ImagePainter Class
 * Class Initialization
 * repaintSquare() Method
 * repaintArea() Method
 * repaintImage() Method
 * paintComponent() Method
* Main Class

4. [Running the Program](#usage)  
5. [Results](#output)  




## Introduction
 This document demonstrates the concept of physical concurrency by implementing an Image Painter program. The program visualizes an image recoloring process, performed either with a single thread or multiple threads, based on the user's choice. It effectively illustrates and compares the performance of single-threaded and multi-threaded processing, highlighting how threads operate on single or multiple CPU cores.


**In single-threaded** mode, the program calls the image recoloring subprogram and processes the entire image sequentially, utilizing only one CPU core. The starting and ending coordinates of the image are passed to the subprogram, which handles the recoloring for the entire image in one go.


**In multi-threaded** mode, the program divides the image into distinct regions, with the number of regions determined by the number of available CPU cores. Each region's starting and ending coordinates are assigned to separate threads, allowing concurrent processing across multiple cores. The same recoloring subprogram is invoked for each thread, but with parameters tailored to the specific region it processes.

This approach not only demonstrates the benefits of parallel processing but also provides a clear visualization of how tasks are divided among and handled by threads.

The document further details the implementation of the program and its components, including the subprograms and their respective roles in achieving efficient image recoloring.


## Overview
 This program has been implemented using java programming language. The program makes use of Swing library to create the frame and draw the image. Subprograms implemented in the program are as follows: repaintSquare(), repaintArea(), repaintImage(), and paintComponent(). Subprograms has been written to simulate the repainting process by visiting and applying average color of that square to the whole square. Then the larger area is then colored by repainting a range of squares inside the bounds of that area. The whole image is repainted as the result of call to the larger area repainter subprogram. 

### ImagePainter class ###
 ImagePainter class is initialized with image object, squaresize, and multhithreading mode arguments. The number of available CPU cores is determined using the Runtime class. Specifically, the availableProcessors() method of Runtime.getRuntime() is called to fetch the count of CPU cores available to the Java Virtual Machine.

```c
  public ImagePainter(BufferedImage image, int squareSize, boolean multiThreaded) {
        this.image = image;
        this.squareSize = squareSize;
        this.multiThreaded = multiThreaded;
        this.cores = Runtime.getRuntime().availableProcessors(); // Fetch system CPU core count
    }

```

*repaintSquare() method* - to repaint a single square with the average color of that square. 

```c
  private synchronized void repaintSquare(int startX, int startY) {
        int endX = Math.min(startX + squareSize, image.getWidth());  // Ensuring square does not exceed image bounds
        int endY = Math.min(startY + squareSize, image.getHeight());

        int red = 0, green = 0, blue = 0, count = 0;                 // initializing base colors 

        // Calculating the average color of the square
        for (int y = startY; y < endY; y++) {                        
            for (int x = startX; x < endX; x++) {                    
                Color pixel = new Color(image.getRGB(x, y));         
                red += pixel.getRed();                              
                green += pixel.getGreen();							 
                blue += pixel.getBlue();                            
                count++;											
            }
        }

        red /= count;												// finding average for each color component
        green /= count;
        blue /= count;

        Color averageColor = new Color(red, green, blue);			// Creating a new color using the average RGB values

        // Apply the average color to the square
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                synchronized (image) {                              // Ensuring thread-safe access to the image while updating its pixel values
                    image.setRGB(x, y, averageColor.getRGB());
                }
            }
        }
    }
```
## Explanation 
**repaintSquare()** method takes as its arguments the starting and  coordinates for a square *- (startX, startY)*. End of the square is reached with square size away from the staring coordinates. To ensure the sqaure is within the bounds of image, minimum check is done in between the image bounds and square end points, and sqaure area is set to be minimum among them. 

To find the average color for the square, firstly, base colors RGB is set to 0. For each pixel of the square, color of the pixel is obtained -*(new Color(image.getRGB(x, y)))* and RGB color components updated. After the whole square is captured, each color component is divided over total pixel count to find the average of that color. Derived colors are used to create the average color for the whole square - *(new Color(red, green, blue))*. Finally, the square is colored with the average color.

Since the program may use multiple threads to process different parts of the image simultaneously, the synchronized (image) block ensures that only one thread modifies the shared image object at a time, preventing race conditions and ensuring thread-safe operations.



**repaintArea()** is used to process coloring on a specific area of the image and accepts as its arguments starting and ending positions the area - *int startX, int startY, int endX, int endY*. Area is stepped through by moving along square-wise. Each captired square are recolored by calling  *repaintSquare()*. After every square coloring, the working thread is paused for a certain amount of time, to enable repainting observation possible. 

```c
    private void repaintArea(int startX, int startY, int endX, int endY) {
        for (int y = startY; y < endY; y += squareSize) {      // capturing the area moving by squares along the height
            for (int x = startX; x < endX; x += squareSize) {  // capturing the area moving by squares along width
                repaintSquare(x, y); // Processing each square block by applying average color
                repaint();           
                try {
                    Thread.sleep(20); . 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
```

**repaintImage()** method is designed to process the actual repaiting whole image. Method makes call/s to the repaintArea() method to process the image with single thread or with multiple threads. If the user has prompted the single threaded processing, a single call to the repaintArea is made with passing image coordinates to it. In this case, the area to be repainted is processed by a single CPU core. Comparingly, if the user prompt indicates multithreading, several calls to the repaintArea() are made depending on the number of CPU cores. To implement this logic, image has been divided into regions by the number of cores. (As noted previously, number of cores has been fetched making use of    Runtime class.) Fpr concurrent exection, Thread class from java.lang package is used. Array of thread objects are intialized with the number of cores available. For each of the region separated, a new thread object is created and assigned to the i-th position of the threads array. This thread is tasked with processing the repaintArea method for a specific region of the image.  To ensure proper synchronization, each thread in the threads array is joined to the main thread. This means the main thread waits for all the worker threads to finish processing before continuing execution. If a thread is interrupted during this waiting period, the interrupted status is preserved, and an error message is logged.


```c
public void repaintImage() {
        int width = image.getWidth();       // Total image width
        int height = image.getHeight();     // Total image height

        if (multiThreaded) {
            int regionHeight = height / cores; // Dividing image height corresponding to available cores
            Thread[] threads = new Thread[cores];

            // Creating and starting threads for parallel processing
            for (int i = 0; i < cores; i++) {
                int startY = i * regionHeight;             // Starting coordinate for Y to be used by current thread
                int endY = (i == cores - 1) ? height : startY + regionHeight; // Ending Y-coordinate for the thread

                threads[i] = new Thread(() -> repaintArea(0, startY, width, endY)); 
                                                            
                threads[i].start();   // Start the thread to begin processing the assigned region
            }

       
            for (Thread thread : threads) {
                try {
                    // Join the current thread to the main thread, blocking until the thread finishes
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Thread interrupted: " + e.getMessage());
                }
            }
        } else {
            repaintArea(0, 0, width, height); // Single-threaded processing for the whole image utilizing one core
        }
    }

```

**paintComponent()**  This method is overridden from the JPanel class to render the image on the panel. It is called automatically by the Swing framework whenever the panel needs to be repainted, such as during resizing or updating.

The Graphics object (g) is used to perform custom drawing on the panel, and in this case, it scales and draws the image to fit the panel's dimensions.
The call to super.paintComponent(g) ensures that the panel is properly cleared and that any default painting behavior is preserved before custom rendering.

 ```c
   @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), null); // Scaling the image to fit to the panel size
        }
    }
```


### `Main` class. 

This program accept three arguments from the command line: file name, square size and the processing mode. To ensure the valid arguments are passed to the program, validateArguments() method has been implemented. In case validation is not succesful, program stops the execution.

```c
private static boolean validateArguments(String[] args) {
        if (args.length < 3) {
            System.out.println("Error: Please provide an <image file>, <square_size>, and <processing mode (S|M)>.");
            return false;
        }
        return true;
    }
```


* User Input Parsing: The program begins by parsing the user provided command-line arguments separately. Each parameter is validated independently to ensure correctness and catch potential exceptions. If any parameter is invalid, an appropriate error message is displayed, and the program exits without proceeding further.

* Image Processing Initialization:
	1. Image Loading: The specified image file is loaded using the ImageIO class.
    2. Image Processing Setup: The loaded image, square size, and processing mode are passed to the processImage() function, which is responsible for preparing and launching the main processing workflow.
* Frame and GUI Setup:
 * Inside processImage():
	* A JFrame is created to display the image processing visualization in a graphical user interface (GUI).
    * The program captures the screen size using java.awt.Toolkit.getDefaultToolkit() to ensure the frame fits the screen dimensions.
    * The ImagePainter class is instantiated to handle the actual image repainting process, and the frame is made visible to start the processing.
* Processing and Output:
  * The repaintImage() method of the ImagePainter class is called to begin recoloring the image based on the average color of square regions.
  * After processing, the resulting image is saved to a file named result.jpg using the ImageIO.write() method.
* Exception Handling:
  * Exception handling is implemented throughout the program to ensure error messages are displayed.


## Usage
The program is executed through the command line and requires three input parameters: file name, square size and the processing mode.Below are some of the examples:

# Example 
```c
(base) lemanguluzada@Lemans-MacBook-Pro src % java Main /Users/lemanguluzada/eclipse-workspace/As3/src/Images/example.jpg 30 S 
Final image saved as 'result.jpg'.
(base) lemanguluzada@Lemans-MacBook-Pro src % java Main /Users/lemanguluzada/eclipse-workspace/As3/src/Images/example1.jpg 50 S
Final image saved as 'result.jpg'.
(base) lemanguluzada@Lemans-MacBook-Pro src % java Main /Users/lemanguluzada/eclipse-workspace/As3/src/Images/example3.jpg 100 M
Final image saved as 'result.jpg'.
```
## Results ##
Example image results can be found in the 'Images' folder within the main branch of the repository on GitHub. You can access them by navigating to the repository's main branch and opening the 'Images' directory.

