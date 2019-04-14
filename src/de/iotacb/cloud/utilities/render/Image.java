package de.iotacb.cloud.utilities.render;

import de.iotacb.cloud.core.display.Display;
import de.iotacb.cloud.core.exceptions.InitializeException;
import de.iotacb.cloud.utilities.math.Vector;
import de.iotacb.cloud.utilities.time.Timer;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;

import static org.lwjgl.opengl.GL11.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Image {

    public String imagePath;
    public Image[] images;

    int imageID, imageIndex, width, height;

    ByteBuffer pixelBuffer;

    boolean animated;

    Timer frameDelay;

    public Image(final String imagePath, final Display display) {
        this.imagePath = imagePath;
        display.currentScreen.images.add(this);
        initialize();
    }

    public Image(final Image[] images, final boolean animated, final Display display) {
        this.images = images;
        this.animated = animated;
        this.frameDelay = new Timer();
        display.currentScreen.images.add(this);
        initialize();
    }

    private void initialize() {
        try {
            this.imageID = glGenTextures();
            BufferedImage image = ImageIO.read(new File(imagePath));
            this.width = image.getWidth();
            this.height = image.getHeight();

            int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
            pixelBuffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                pixelBuffer.put((byte) (pixel >> 16 & 0xFF)); // Red color channel
                pixelBuffer.put((byte) (pixel >> 8 & 0xFF)); // Green color channel
                pixelBuffer.put((byte) (pixel & 0xFF)); // Blue color channel;
                pixelBuffer.put((byte) (pixel >> 24 & 0xFF)); // Alpha color channel;
            }

            pixelBuffer.flip();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Draw a image
     * @param x The x location of the image
     * @param y The y location of the image
     * @param width The width of the image
     * @param height The height of the image
     */
    public void draw(double x, double y, double width, double height) {
        if (!Render.shouldBeRendered(x, y, width, height)) return;
        if (pixelBuffer == null) {
            try {
                throw new InitializeException("Pixel buffer is not initialized!");
            } catch (InitializeException e) {
                e.printStackTrace();
            }
        }

        width /= 2;
        height /= 2;

        Render.push();
        Render.color(Color.white); // Reset rendering color

        Render.enable(GL_TEXTURE_2D);
        Render.enable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Render.disable(GL_LIGHTING);
        glBindTexture(GL_TEXTURE_2D, imageID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, this.width, this.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);

        double wDiff = Math.abs(this.width - width);
        double hDiff = Math.abs(this.height - height);

        x += (this.width - wDiff);
        y += (this.height - hDiff);

        Render.begin(GL_QUADS);
        {
            glTexCoord2d(0, 1);
            Render.vertex(x - width, y + height);
            glTexCoord2d(1, 1);
            Render.vertex(x + width, y + height);
            glTexCoord2d(1, 0);
            Render.vertex(x + width, y - height);
            glTexCoord2d(0, 0);
            Render.vertex(x - width, y - height);
        }
        Render.end();
        Render.enable(GL_LIGHTING);
        Render.disable(GL_BLEND);
        Render.disable(GL_TEXTURE_2D);

        Render.pop();
    }

    /**
     * Draw a image
     * @param location The location of the image as a vector
     * @param dimensions The dimensions of the image as a vector
     */
    public void draw(final Vector location, final Vector dimensions) {
        draw(location.x, location.y, dimensions.x, dimensions.y);
    }

    /**
     * Draw a image with the dimensions of the image file
     * @param x The x location of the image
     * @param y The y location of the image
     */
    public void draw(final double x, final double y) {
        draw(x, y, this.width, this.height);
    }

    /**
     * Draw a image with the dimensions of the image file
     * @param location The location of the image as a vector
     */
    public void draw(final Vector location) {
        draw(location.x, location.y, this.width, this.height);
    }

    /**
     * Draw a image from the image list
     * @param x The x location of the image
     * @param y The y location of the image
     * @param width The width of the image
     * @param height The height of the image
     * @param imageIndex The index of the image in the image list
     */
    public void draw(final double x, final double y, final double width, final double height, final int imageIndex) {
        if (this.images == null || this.images.length <= 0) return;
        this.images[imageIndex].draw(x, y, width, height);
    }

    /**
     * Draw a image from the image list
     * @param location The location of the image as a vector
     * @param dimensions The dimensions of the image as a vector
     * @param imageIndex The index of the image in the image list
     */
    public void draw(final Vector location, final Vector dimensions, final int imageIndex) {
        draw(location.x, location.y, dimensions.x, dimensions.y, imageIndex);
    }

    /**
     * Draw a image from the image list with the dimensions of the image file
     * @param x The x location of the image
     * @param y The y location of the image
     * @param imageIndex The index of the image in the image list
     */
    public void draw(final double x, final double y, final int imageIndex) {
        if (this.images == null || this.images.length <= 0) return;
        this.images[imageIndex].draw(x, y, this.width, this.height);
    }

    /**
     * Draw a image from the image list with the dimensions of the image file
     * @param location The location of the image as a vector
     * @param imageIndex The index of the image int the image list
     */
    public void draw(final Vector location, final int imageIndex) {
        draw(location.x, location.y, imageIndex);
    }

    /**
     * Draw an animated image
     * @param x The x location of the image
     * @param y The y location of the image
     * @param width The width of the image
     * @param height The height of the image
     * @param delay The delay between the frames in milliseconds
     */
    public void drawAnimated(final double x, final double y, final double width, final double height, final long delay) {
        if (!this.animated || this.images == null || this.images.length <= 0) return;
        if (frameDelay.havePassed(delay)) {
            if (this.imageIndex < this.images.length - 1) {
                this.imageIndex++;
            } else {
                this.imageIndex = 0;
            }
            draw(x, y, width, height, this.imageIndex);
        }
    }

    /**
     * Draw an animated image
     * @param location The location of the image as a vector
     * @param dimensions The dimensions of the image
     * @param delay The delay between the frames in milliseconds
     */
    public void drawAnimated(final Vector location, final Vector dimensions, final long delay) {
        drawAnimated(location.x, location.y, dimensions.x, dimensions.y, delay);
    }

    /**
     * Draw an animated image with the dimensions of the image file
     * @param x The x location of the image
     * @param y The y location of the image
     * @param delay The delay between the frames in milliseconds
     */
    public void drawAnimated(final double x, final double y, final long delay) {
        drawAnimated(x, y, this.width, this.height, delay);
    }

    /**
     * Draw an animated image with the dimensions of the image file
     * @param location The location of the image as a vector
     * @param delay The delay between the frame in milliseconds
     */
    public void drawAnimated(final Vector location, final long delay) {
        drawAnimated(location.x, location.y, delay);
    }

    /**
     * Reload the image
     */
    public void reload() {
        initialize();
    }

}
