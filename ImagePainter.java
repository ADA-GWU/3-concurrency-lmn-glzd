import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * The ImagePainter class is responsible for rendering a BufferedImage on a JPanel
 * and performing image processing with options for single-threaded or multi-threaded execution.
 */

public class ImagePainter extends JPanel {
    private static final long serialVersionUID = 1L;
	private final BufferedImage image;      // The image to be processed and displayed
    private final int squareSize;           // The size of each square for processing 
    private final boolean multiThreaded;    // Indicates if multi-threading is selected
    private final int cores;                // Number of CPU cores available for parallel processing

    /**
     * This is a constructor for initializing the ImagePainter.
     *
     * @param image         The image to be painted and processed.
     * @param squareSize    The size of the squares for block-based averaging.
     * @param multiThreaded set to True for multi-threaded processing, false otherwise.
     */
    public ImagePainter(BufferedImage image, int squareSize, boolean multiThreaded) {
        this.image = image;
        this.squareSize = squareSize;
        this.multiThreaded = multiThreaded;
        this.cores = Runtime.getRuntime().availableProcessors(); // Fetch system CPU core count
    }

    /**
     * Overridden paintComponent method draws the processed image onto the JPanel.
     *
     * @param g The Graphics object used for rendering.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), null); // Scaling the image to fit to the panel size
        }
    }

    /**
     * Actual repainting of the image
     */
    
    public void repaintImage() {
        int width = image.getWidth();       // Total image width
        int height = image.getHeight();     // Total image height

        if (multiThreaded) {
            int regionHeight = height / cores; // Dividing image height corresponding to available cores
            Thread[] threads = new Thread[cores];

            // Creating and starting threads for parallel processing
            for (int i = 0; i < cores; i++) {
                int startY = i * regionHeight;                        // Starting coordinate for Y to be used by current thread
                int endY = (i == cores - 1) ? height : startY + regionHeight; // Ending Y-coordinate for the thread

                threads[i] = new Thread(() -> repaintArea(0, startY, width, endY)); // Create a new thread to process a specific region of the image (from startY to endY)
                                                                                    // The thread executes the repaintArea method for this region
                threads[i].start();   // Start the thread to begin processing the assigned region
            }

         // Wait for all threads to complete their execution before proceeding
         // This ensures that all image regions are processed before moving forward
            for (Thread thread : threads) {
                try {
                    // Join the current thread to the main thread, blocking until the thread finishes
                    thread.join();
                } catch (InterruptedException e) {
                    // If the thread is interrupted, re-interrupt the thread to preserve the interrupted status
                    Thread.currentThread().interrupt();
                    // Log an error message indicating that the thread was interrupted
                    System.err.println("Thread interrupted: " + e.getMessage());
                }
            }
        } else {
            repaintArea(0, 0, width, height); // Single-threaded processing for the whole image utilizing one core
        }
    }

    /**
     * Processes a specific area of the image and repaints the area.
     *
     * @param startX The starting X-coordinate.
     * @param startY The starting Y-coordinate.
     * @param endX   The ending X-coordinate.
     * @param endY   The ending Y-coordinate.
     */
   
    private void repaintArea(int startX, int startY, int endX, int endY) {
        for (int y = startY; y < endY; y += squareSize) {      // capturing the area moving by squares along the height
            for (int x = startX; x < endX; x += squareSize) {  // capturing the area moving by squares along width
                repaintSquare(x, y); // Processing each square block by applying average color
                repaint();           // Refreshing the panel to create the visual effect of coloring
                try {
                    Thread.sleep(20); // Pausing updates to panel 20 seconds to visualize the repainting process. 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Method for processing a square region of the image by calculating and and applying the average color.
     *
     * @param startX The starting X-coordinate of the square.
     * @param startY The starting Y-coordinate of the square.
     */
    
    private synchronized void repaintSquare(int startX, int startY) {
        int endX = Math.min(startX + squareSize, image.getWidth());  // Ensuring square does not exceed image bounds
        int endY = Math.min(startY + squareSize, image.getHeight());

        int red = 0, green = 0, blue = 0, count = 0;                 // initializing base colors 

        // Calculating the average color of the square
        for (int y = startY; y < endY; y++) {                        // capturing square along its height
            for (int x = startX; x < endX; x++) {                    // capturing square along its width
                Color pixel = new Color(image.getRGB(x, y));         // Extracting the color of the pixel at (x, y) coordinates
                red += pixel.getRed();                               // Adding the red component of the current pixel to the total red sum
                green += pixel.getGreen();							 // Adding the green component of the current pixel to the total green sum
                blue += pixel.getBlue();                             // Adding the blue component of the current pixel to the total blue sum
                count++;											 // Incrementing the total number of pixels processed so far
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
}
