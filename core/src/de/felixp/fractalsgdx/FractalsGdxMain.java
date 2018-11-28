package de.felixp.fractalsgdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Matrix3;
import com.kotcrab.vis.ui.VisUI;

public class FractalsGdxMain extends ApplicationAdapter {
	SpriteBatch batch;
	Texture img;
	Texture palette;
	ShaderProgram shader;
	ShaderProgram sobelShader;
	Matrix3 matrix = new Matrix3(new float[] {1,0,0, 0,1,0, 0,0,1, 0,0,0});

	String fragmentPrefix = "CalculateFloatHtml";
	public static int iterations = 1000;

	long lastIncrease = System.currentTimeMillis();

	public static double xPos = 0;
	public static double yPos = 0;

	public static double velocityX = 0;
	public static double velocityY = 0;

	public static double velocityDecay = 5; //decay factor per second

	public static double scale = 3;
	public static double scalingFactor = 0;

	public static boolean invertY = false;

	public static boolean postprocessing = true;

	public static float biasReal = 0;
	public static float biasImag = 0;

	FrameBuffer fbo;

	int width;
	int height;

	@Override
	public void create () {
		VisUI.load();
		batch = new SpriteBatch();
		img = new Texture("badlogic.jpg");
		palette = new Texture("palette.png");
		setupShaders();
		batch.setShader(shader);
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(new GestureDetector(new FractalsGestureListener()));
		multiplexer.addProcessor(new FractalsInputProcessor());
		Gdx.input.setInputProcessor(new GestureDetector(new FractalsGestureListener()));
		fbo = new FrameBuffer(Pixmap.Format.RGB888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
		if (postprocessing)
			FractalsInputProcessor.yMultiplier *= -1;
	}

	@Override
	public void render () {
		handleInput();

		if (postprocessing)
			fbo.begin();
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		palette.bind();
		shader.begin();

		setShaderUniforms();
		batch.begin();
		batch.setShader(shader);
		batch.draw(img, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		batch.end();

		shader.end();
//		shader.begin();
//
//		shader.setUniformf("scale", 5);
//		shader.setUniformf("center", (float) 0, (float) 0);
//		//shader.setUniformf("resolution", (float) 250, (float) 250);
//		//if (Gdx.graphics.getWidth() > 600 && Gdx.graphics.getHeight() > 600)
//			batch.begin();
//			batch.draw(img, Gdx.graphics.getWidth()-300, Gdx.graphics.getHeight()-300, 250, 250);
//		batch.end();

		shader.end();
		if (postprocessing) {
			fbo.end();

			sobelShader.begin();
			sobelShader.setUniformf("resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

			batch.begin();
			batch.setShader(sobelShader);
			batch.draw(fbo.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			batch.end();

			sobelShader.end();
		}
	}

	private void handleInput() {
		float deltaTime = Gdx.graphics.getDeltaTime();
		double absFactor = deltaTime * scale;

		//scale *= 1 + scalingFactor * deltaTime;

		if (Gdx.input.isKeyPressed(Keys.LEFT)) {
			xPos -= absFactor * FractalsInputProcessor.speedMultiplier_pan;
		}
		if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
			xPos += absFactor * FractalsInputProcessor.speedMultiplier_pan;
		}
		if (Gdx.input.isKeyPressed(Keys.DOWN)) {
			yPos -= FractalsInputProcessor.yMultiplier * absFactor * FractalsInputProcessor.speedMultiplier_pan;
		}
		if (Gdx.input.isKeyPressed(Keys.UP)) {
			yPos += FractalsInputProcessor.yMultiplier * absFactor * FractalsInputProcessor.speedMultiplier_pan;
		}

		if (Gdx.input.isKeyPressed(Keys.PLUS) && !Gdx.input.isKeyPressed(Keys.MINUS)){
			FractalsInputProcessor.iterationsStep = FractalsInputProcessor.iterationsChangeSpeed;
		} else if (Gdx.input.isKeyPressed(Keys.MINUS)){
			FractalsInputProcessor.iterationsStep = -FractalsInputProcessor.iterationsChangeSpeed;
		} else {
			FractalsInputProcessor.lastIterationStepTime = System.currentTimeMillis();
			FractalsInputProcessor.iterationsStep = 0;
		}

		double biasChangeSpeed = 0.1;
		if (Gdx.input.isKeyPressed(Keys.I) && !Gdx.input.isKeyPressed(Keys.K)){
			biasReal += biasChangeSpeed*absFactor;
		} else if (Gdx.input.isKeyPressed(Keys.K)){
			biasReal -= biasChangeSpeed*absFactor;
		}
		if (Gdx.input.isKeyPressed(Keys.O) && !Gdx.input.isKeyPressed(Keys.L)){
			biasImag += biasChangeSpeed*absFactor;
		} else if (Gdx.input.isKeyPressed(Keys.L)){
			biasImag -= biasChangeSpeed*absFactor;
		}

		xPos += velocityX * absFactor;
		yPos += FractalsInputProcessor.yMultiplier * velocityY * absFactor;

		double decayFactor = (velocityDecay*deltaTime);
		if (velocityX > 0) {
			velocityX -= Math.min(decayFactor, velocityX);
		} else if (velocityX < 0) {
			velocityX += Math.min(decayFactor, -velocityX);
		}
		if (velocityY > 0) {
			velocityY -= Math.min(decayFactor, velocityY);
		} else if (velocityY < 0) {
			velocityY += Math.min(decayFactor, -velocityY);
		}

		if (FractalsInputProcessor.iterationsStep != 0){
			long t = System.currentTimeMillis();
			double deltaT = (t - FractalsInputProcessor.lastIterationStepTime)/1000.0;
			int change = (int)(FractalsInputProcessor.iterationsStep * deltaT * (iterations+1000));
			if (change < 0 && iterations < -change)
				change = -iterations;
			if (change != 0) {
				FractalsInputProcessor.lastIterationStepTime = t;
				iterations += change;
				System.out.println("set iterations to " + iterations);
			}
		}
	}

	private void setShaderUniforms() {
		float currentWidth = Gdx.graphics.getWidth();
		float currentHeight = Gdx.graphics.getHeight();
		shader.setUniformMatrix("u_projTrans", matrix);
		shader.setUniformf("ratio", currentWidth/(float)currentHeight);
		shader.setUniformf("resolution", width, height);

//		long t = System.currentTimeMillis();
//		if (t-lastIncrease > 1) {
//			lastIncrease = t;
//			iterations++;
//		}
		shader.setUniformi("iterations", iterations);
		shader.setUniformf("scale", (float)scale);
		shader.setUniformf("center", (float) xPos, (float) yPos);
		shader.setUniformf("biasReal", biasReal);
		shader.setUniformf("biasImag", biasImag);
		//shader.setUniformi("u_texture", 0);
		shader.setUniformi("palette", 0);
	}

	public void setupShaders() {
		ShaderProgram.pedantic = false;
		shader = new ShaderProgram(Gdx.files.internal("PassthroughVertex.glsl"),
				Gdx.files.internal(fragmentPrefix+"Fragment.glsl"));
		if (!shader.isCompiled()) {
			throw new IllegalStateException("Error compiling shaders: "+shader.getLog());
		}

		sobelShader = new ShaderProgram(Gdx.files.internal("PassthroughVertex.glsl"),
				Gdx.files.internal("SobelFragment.glsl"));
		if (!sobelShader.isCompiled()) {
			throw new IllegalStateException("Error compiling shaders: "+sobelShader.getLog());
		}


		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
	}

	@Override
	public void resize(int width, int height) {
//		shader.begin();
//		shader.setUniformf("resolution", width, height);
//		shader.end();
		fbo = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
	}

//	public void setIterations() {
//		shader.begin();
//		shader.setUniformf("iterations", iterations);
//		shader.end();
//	}

	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
	}
}
