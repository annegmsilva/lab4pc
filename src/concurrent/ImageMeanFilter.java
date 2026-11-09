import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class provides functionality to apply a mean filter to an image.
 * The mean filter is used to smooth images by averaging the pixel values
 * in a neighborhood defined by a kernel size.
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * ImageMeanFilter.applyMeanFilter("input.jpg", "output.jpg", 3);
 * }
 * </pre>
 * 
 * <p>Supported image formats: JPG, PNG</p>
 * 
 * <p>Author: temmanuel@comptuacao.ufcg.edu.br</p>
 */
public class ImageMeanFilter {
    
    /**
     * Applies mean filter to an image
     * 
     * @param inputPath  Path to input image
     * @param outputPath Path to output image 
     * @param kernelSize Size of mean kernel
     * @throws IOException If there is an error reading/writing
     */
    public static void applyMeanFilter(String inputPath, String outputPath, int kernelSize, int numThreads) throws IOException {
        BufferedImage originalImage = ImageIO.read(new File(inputPath));

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage filteredImage = new BufferedImage(
            width, 
            height, 
            BufferedImage.TYPE_INT_RGB
        );

        int[] entrada = originalImage.getRGB(0, 0, width, height, null, 0, width);
        int[] saida = new int[entrada.length];
        Thread[] threads = new Thread[numThreads];
        long[] alteradosThread = new long[numThreads];
        long[] inalteradosThread = new long[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int indiceThread = i;

            final int inicioY = (int) ((long) i * height / numThreads);
            final int fimY = (int) ((long) (i+1) * height / numThreads);

            threads[i] = new Thread(() -> {
                long alterados = 0;
                long inalterados = 0;

                for (int y = inicioY; y < fimY; y++) {
                    for (int x = 0; x < width; x++) {
                        int indicePixel = y*width + x;
                        int novaCor = calculateNeighborhoodAverage(
                            entrada, width, height, x, y, kernelSize);

                        saida[indicePixel] = novaCor;

                        int corOriginal = entrada[indicePixel];
                        if (novaCor == corOriginal) {
                            inalterados++;
                        } else {
                            alterados++;
                        }
                    }
                }

                alteradosThread[indiceThread] = alterados;
                inalteradosThread[indiceThread] = inalterados;

            }, "filtro " + i);

            threads[i].start();
        }
 
        for (Thread t : threads){
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
        }

        long totalAlterados = 0;
        long totalInalterados = 0;
        
        for (int i = 0; i < numThreads; i ++) {
            totalAlterados += alteradosThread[i];
            totalInalterados += inalteradosThread[i];
        }
        
        filteredImage.setRGB(0, 0, width, height, saida, 0, width);

        System.out.println("Total de celulas alteradas - " + totalAlterados);
        System.out.println("Total de celulas inalteradas - " + totalInalterados);

        ImageIO.write(filteredImage, "jpg", new File(outputPath));
    }
    
    
    private static int calculateNeighborhoodAverage(int[] image, int width, int height, int centerX, int centerY, int kernelSize) {
        int pad = kernelSize / 2;
        
        // Arrays for color sums
        long redSum = 0, greenSum = 0, blueSum = 0;
        int pixelCount = 0;
        
        // Process neighborhood
        for (int dy = -pad; dy <= pad; dy++) {
            for (int dx = -pad; dx <= pad; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                
                // Check image bounds
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    // Get pixel color
                    int rgb = image[y*width + x];
                    
                    // Extract color components
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;
                    
                    // Sum colors
                    redSum += red;
                    greenSum += green;
                    blueSum += blue;
                    pixelCount++;
                }
            }
        }
        
        int red = (int) (redSum / pixelCount);
        int blue = (int) (blueSum / pixelCount);
        int green = (int) (greenSum / pixelCount);
        return (red << 16) | (green << 8) | blue;
    }
    
    /**
     * Main method for demonstration
     * 
     * Usage: java ImageMeanFilter <input_file>
     * 
     * Arguments:
     *   input_file - Path to the input image file to be processed
     *                Supported formats: JPG, PNG
     * 
     * Example:
     *   java ImageMeanFilter input.jpg
     * 
     * The program will generate a filtered output image named "filtered_output.jpg"
     * using a 7x7 mean filter kernel
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ImageMeanFilter <input_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        int numThreads =  Integer.parseInt(args[1]);
        try {
            applyMeanFilter(inputFile, "filtered_output.jpg", 7, numThreads);
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
        }
    }
}
