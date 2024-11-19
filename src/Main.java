import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

/**
 * Welcome to Image Processing application.
 * Main class handles user inputs, image loading, and initializing the ImagePainter.
 */

public class Main {

    public static void main(String[] args) { 
        if (!validateArguments(args)) {      // Stopping program if any of the arguments missing
            return;
        }
        do { 
        String imageFile = args[0];                 // File path of input image
        int squareSize = parseSquareSize(args[1]);  // Size of square for processing image
        if (squareSize == -1) return;

        Boolean multiThreaded = parseProcessingMode(args[2]); // Processing mode: single or multi-threaded
        if (multiThreaded == null) return;

        BufferedImage image = loadImage(imageFile); // Load the image
        if (image == null) return;
        
        Scanner sc = new Scanner(System.in);

        
        processImage(image, squareSize, multiThreaded); // Start processing image 
        System.out.println("Would you like to process another image? : Y/N");
        String user = sc.next().trim().toUpperCase();
        if(!user.equals("Y"))
        	break;
        System.out.println("Please provide an <image file>, <square_size>, and <processing mode (S|M)>.");
        imageFile = sc.next();
        squareSize = sc.nextInt();
        multiThreaded = sc.nextBoolean();
        
        } while(true);
        
    }

    // Validate input arguments for completeness
    private static boolean validateArguments(String[] args) {
        if (args.length < 3) {
            System.out.println("Error: Please provide an <image file>, <square_size>, and <processing mode (S|M)>.");
            return false;
        }
        return true;
    }

    // Parse and validate square size input
    private static int parseSquareSize(String squareSizeArg) {
        try {
            int squareSize = Integer.parseInt(squareSizeArg);
            if (squareSize <= 0) {
                System.out.println("Error: Square size must be a positive integer.");
                return -1;
            }
            return squareSize;
        } catch (NumberFormatException e) {
            System.out.println("Error: Square size must be an integer. " + e.getMessage());
            return -1;
        }
    }

    // Parse processing mode input (single or multi-threaded)
    private static Boolean parseProcessingMode(String modeArg) {
        if (modeArg.equalsIgnoreCase("M")) {
            return true;
        } else if (modeArg.equalsIgnoreCase("S")) {
            return false;
        } else {
            System.out.println("Error: Processing mode must be 'S' (single-threaded) or 'M' (multi-threaded).");
            return null;
        }
    }

    // Load an image from the specified file
    private static BufferedImage loadImage(String imageFile) {
        try {
            File file = new File(imageFile);
            if (!file.exists() || !file.canRead()) {
                System.out.println("Error: Cannot read the image file. Check the file path and permissions.");
                return null;
            }
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                System.out.println("Error: The file is not a valid image.");
                return null;
            }
            return image;
        } catch (IOException e) {
            System.out.println("Error: An IO exception occurred. " + e.getMessage());
            return null;
        }
    }

    // Process the image and display results in a JFrame
    private static void processImage(BufferedImage image, int squareSize, boolean multiThreaded) {
        try {
            ImagePainter repainter = new ImagePainter(image, squareSize, multiThreaded);
            JFrame frame = new JFrame("Image Painter");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            frame.setSize(screenSize.width, screenSize.height);

            frame.add(repainter);
            frame.setVisible(true);

            repainter.repaintImage(); // Start processing

            ImageIO.write(image, "jpg", new File("result.jpg")); // Save the final image
            System.out.println("Final image saved as 'result.jpg'.");
        } catch (Exception e) {
            System.out.println("Error during image processing: " + e.getMessage());
        }
    }
}
