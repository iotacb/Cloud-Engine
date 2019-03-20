package xyz.iotacb.cloud.core.display;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowOpacity;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.glfw.GLFW.nglfwGetFramebufferSize;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glViewport;

import java.awt.Color;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import xyz.iotacb.cloud.core.display.input.KeyHandler;
import xyz.iotacb.cloud.core.exceptions.CloudCreateException;
import xyz.iotacb.cloud.core.exceptions.CloudInitializeException;
import xyz.iotacb.cloud.core.gui.Button;
import xyz.iotacb.cloud.utilities.math.vector.VectorI;
import xyz.iotacb.cloud.utilities.time.Timer;

public class Display {

	public VectorI displayDimensions, frameBufferSize; // Stores sizes

	public boolean resizeable, fullscreen; // Window should be resizeable, window should be fullscreen

	public long mainMonitor, window; // Main monitor ID, window ID

	public String title; // Window title

	public Screen currentScreen, previousScreen; // Screen which will be renderd, previous screen

	public int mouseX, mouseY, swapInterval = 1; // Mouse location, swap interval of GLFW

	public KeyHandler keyHandler; // Keyhandler for key input

	public int currentFPS, fpsLock; // Current FPS and V-Sync
	int fpsCounter;

	GLFWFramebufferSizeCallback frameBufferCallback;
	GLFWWindowSizeCallback windowSizeCallback;

	Timer fpsTimer;

	public Display(final VectorI dimensions, final String title, final boolean resizeable, final boolean fullscreen)
			throws CloudInitializeException, CloudCreateException {
		this.displayDimensions = dimensions;
		this.frameBufferSize = new VectorI();
		this.title = title;
		this.resizeable = resizeable;
		this.fullscreen = fullscreen;
		init();
	}

	/**
	 * Initialize the window and set default settings
	 * 
	 * @throws CloudInitializeException
	 * @throws CloudCreateException
	 */
	void init() throws CloudInitializeException, CloudCreateException {
		if (!glfwInit()) {
			throw new CloudInitializeException("Failed to initialize GLFW");
		}

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, (resizeable ? GLFW_TRUE : GLFW_FALSE));
		glfwWindowHint(GLFW_SAMPLES, 4);

		fpsTimer = new Timer();

		mainMonitor = glfwGetPrimaryMonitor();

		GLFWVidMode vidMode = glfwGetVideoMode(mainMonitor);

		if (fullscreen) {
			displayDimensions = new VectorI(vidMode.width(), vidMode.height());
			frameBufferSize = new VectorI(vidMode.width(), vidMode.height());
		}

		window = glfwCreateWindow(displayDimensions.x, displayDimensions.y, title, (fullscreen ? mainMonitor : 0L), 0L);
		
		glfwSetWindowPos(window, (vidMode.width() - displayDimensions.x) / 2, (vidMode.height() - displayDimensions.y) / 2);
		
		if (window == 0L)
			throw new CloudCreateException("Failed to create Window");

		glfwSetFramebufferSizeCallback(window, frameBufferCallback = new GLFWFramebufferSizeCallback() {

			@Override
			public void invoke(long window, int width, int height) {
				if (width > 0 && height > 0 && (frameBufferSize.x != width || frameBufferSize.y != height)) {
					frameBufferSize.set(width, height);
				}
			}
		});

		glfwSetWindowSizeCallback(window, windowSizeCallback = new GLFWWindowSizeCallback() {

			@Override
			public void invoke(long window, int width, int height) {
				if (width > 0 && height > 0 && (displayDimensions.x != width || displayDimensions.y != height)) {
					displayDimensions.set(width, height);
					currentScreen.screenDimensions = displayDimensions;
				}
			}
		});

		glfwMakeContextCurrent(window);
		glfwSwapInterval(swapInterval);
		glfwShowWindow(window);

		IntBuffer bufferSize = BufferUtils.createIntBuffer(2);
		nglfwGetFramebufferSize(window, MemoryUtil.memAddress(bufferSize), MemoryUtil.memAddress(bufferSize) + 4);
		frameBufferSize = new VectorI(bufferSize.get(0), bufferSize.get(1));

		GL.createCapabilities();

		keyHandler = new KeyHandler(window);
	}

	/**
	 * Stops GLFW and exits the application (Stuff that should be runned at shutdown
	 * can be hooked here)
	 */
	public void shutdown() {
		glfwTerminate();
		System.exit(0);
	}

	/**
	 * Updates the cursor location
	 */
	void updateCursor() {
		DoubleBuffer x = BufferUtils.createDoubleBuffer(1);
		DoubleBuffer y = BufferUtils.createDoubleBuffer(1);
		glfwGetCursorPos(window, x, y);
		mouseX = (int) x.get();
		mouseY = (int) y.get();
	}

	public void updateComponents() throws CloudInitializeException {
		updateCursor();

		if (currentScreen == null)
			throw new CloudInitializeException("Screen is not initialized!");

		currentScreen.update();
		keyHandler.update();
		fpsCounter++;
		if (fpsTimer.isPassed(1000)) {
			currentFPS = fpsCounter;
			fpsCounter = 0;
		}
	}

	public void drawComponents() throws CloudInitializeException {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glfwPollEvents();
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glViewport(0, 0, displayDimensions.x, displayDimensions.y);
		glOrtho(0, displayDimensions.x, displayDimensions.y, 0, -1, 1);

		if (currentScreen == null)
			throw new CloudInitializeException("Screen is not initialized!");

		currentScreen.draw();

		glfwSwapBuffers(window);
	}

	/**
	 * Updates the window
	 * 
	 * @throws CloudInitializeException
	 */
	public void start() throws CloudInitializeException {
		while (!glfwWindowShouldClose(window)) {
			Sync.sync(fpsLock);
			updateComponents();
			drawComponents();
		}
		shutdown();
	}

	/**
	 * Change the main screen which should be rendered
	 * 
	 * @param mainScreen
	 */
	public void setScreen(final Screen mainScreen) {
		this.currentScreen = mainScreen;
		this.currentScreen.init();
		for (Button b : this.currentScreen.buttonList) {
			b.clicked = false;
		}
	}

	/**
	 * Change the main screen which should be rendered
	 * 
	 * @param clazz
	 */
	public void setScreen(final Class<? extends Screen> clazz) {
		try {
			this.currentScreen = (Screen) clazz.getConstructor(this.getClass()).newInstance(this);
			this.currentScreen.init();
			for (Button b : this.currentScreen.buttonList) {
				b.clicked = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Change the display opacit of the window
	 * 
	 * @param opacity
	 */
	public void setDisplayOpacity(final float opacity) {
		glfwSetWindowOpacity(window, opacity);
	}

	/**
	 * Change the background color
	 * 
	 * @param color
	 */
	public void setBackgroundColor(final Color color) {
		float red = color.getRed() / 255.0F;
		float green = color.getGreen() / 255.0F;
		float blue = color.getGreen() / 255.0F;
		float alpha = color.getAlpha() / 255.0F;
		glClearColor(red, green, blue, alpha);
	}

	/**
	 * Change the swap interval of GLFW
	 * 
	 * @param swapInterval
	 */
	public void setSwapInterval(int swapInterval) {
		this.swapInterval = swapInterval;
	}

	/**
	 * Set refresh rate limit
	 * 
	 * @param fpsLock
	 */
	public void setFpsLock(int fpsLock) {
		this.fpsLock = fpsLock;
	}
}
