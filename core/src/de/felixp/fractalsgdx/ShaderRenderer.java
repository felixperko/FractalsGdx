package de.felixp.fractalsgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public class ShaderRenderer extends AbstractRenderer {

    final static String shader1 = "CalcMandelbrotFragment.glsl";
    final static String shader2 = "SobelDecodeFragment.glsl";

    boolean refresh = true;

    boolean screenshot = false;

    FrameBuffer fbo;
    FrameBuffer fbo2;

    int currentRefreshes = 0;
    float decode_factor = 1f;

    ShaderProgram shader;
    ShaderProgram sobelShader;
    ShaderProgram passthroughShader;

    Matrix3 matrix = new Matrix3(new float[] {1,0,0, 0,1,0, 0,0,1, 0,0,0});

    float colorShift = 0f;

    boolean burningship = false;
    boolean juliaset = false;

    float xPos;
    float yPos;
    float biasReal = 0f;
    float biasImag = 0f;

    int width;
    int height;
    int iterations = 1000;

    float scale = 3f;

    int sampleLimit = 100;

    SystemContext systemContext = new GPUSystemContext();

    public ShaderRenderer(){
        super();
        setupShaders();
    }

    @Override
    public void init(){
//		palette = new Texture("palette.png");
//		palette.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        fbo2 = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
        ((GPUSystemContext)systemContext).init();

        ParamContainer paramContainer = systemContext.getParamContainer();

        addListener(new ActorGestureListener(){
            @Override
            public void tap(InputEvent event, float x, float y, int count, int button) {

                boolean changed = false;
                NumberFactory nf = paramContainer.getClientParameter("numberFactory").getGeneral(NumberFactory.class);
                Number zoom = paramContainer.getClientParameter("zoom").getGeneral(Number.class);
                Number factor = null;
                if (button == Input.Buttons.LEFT) {
                    factor = nf.createNumber(0.5);
                    changed = true;
                } else if (button == Input.Buttons.RIGHT) {
                    factor = nf.createNumber(2);
                    changed = true;
                }

                if (factor != null)
                    zoom.mult(factor);

                if (changed) {
                    reset();
                }
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                xPos += deltaX;
                yPos += deltaY;
                NumberFactory nf = paramContainer.getClientParameter("numberFactory").getGeneral(NumberFactory.class);
                Number zoom = paramContainer.getClientParameter("zoom").getGeneral(Number.class);
                ComplexNumber midpoint = paramContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class);
                ComplexNumber delta = nf.createComplexNumber(deltaX, -deltaY);
                delta.divNumber(nf.createNumber(Gdx.graphics.getHeight()));
                delta.multNumber(zoom);
                midpoint.sub(delta);
                FractalsGdxMain.forceRefresh = true;
            }
        });
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();

        Gdx.gl.glClearColor( 0, 0, 0, 1 );
        if (refresh)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        fbo.begin();

        if (refresh) {
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            currentRefreshes = 0;
            decode_factor = 1f;
        }

        if (currentRefreshes < sampleLimit){
            currentRefreshes++;

            //palette.bind();
            shader.begin();

            setShaderUniforms();
            batch.end();
            batch.begin();
            batch.setShader(shader);

            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, 1.0f);

            Texture tex = fbo.getColorBufferTexture();
            TextureRegion texReg = new TextureRegion(tex);
            texReg.flip(false, true);
            batch.draw(texReg, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.end();

            shader.end();

            //		shader.begin();
            //		shader.setUniformf("scale", 5);
            //		shader.setUniformf("center", (float) 0, (float) 0);
            //		shader.setUniformf("resolution", (float) 250, (float) 250);
            //		if (Gdx.graphics.getWidth() > 600 && Gdx.graphics.getHeight() > 600) {
            //			batch.begin();
            //			batch.draw(palette, Gdx.graphics.getWidth() - 300, Gdx.graphics.getHeight() - 300, 250, 250);
            //			batch.end();
            //		}
            //		shader.end();

            fbo.end();
            fbo2.begin();

            if (refresh)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            sobelShader.begin();
            sobelShader.setUniformf("resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            sobelShader.setUniformf("samples", currentRefreshes + 1f);
            sobelShader.setUniformf("colorShift", colorShift);
            //			for (int i = 0 ; i < currentRefreshes ; i++){
            //				decode_factor += (byte)(1f/(i+1));
            //			}
            sobelShader.setUniformf("decode_factor", 1f / decode_factor);

            batch.begin();
            batch.setShader(sobelShader);

            //Texture texture = fbo.getColorBufferTexture();
            //TextureRegion textureRegion = new TextureRegion(texture);
            //textureRegion.flip(false, true);

            Color c2 = batch.getColor();
            batch.enableBlending();
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            float r = c2.r;
            float g = c2.g;
            float b = c2.b;
            float a = 1.0f / (currentRefreshes);
            //float a = 1.0f/sampleLimit;
            //decode_factor = currentRefreshes+1;
            //			if (decode_factor > 1.5f)
            //				decode_factor = 1.5f;
            //decode_factor += 0.5f*(2.0f-(decode_factor));
            decode_factor += a;
            //decode_factor += ((float)((byte)Math.floor(a*255)))/255f;
            System.out.println(currentRefreshes + "  " + decode_factor + "  " + a + "  " + ((float) ((byte) (a * 255))) / 255f);
            batch.setColor(a, a, a, a);

            batch.draw(fbo.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//			batch.disableBlending();
            batch.end();

            sobelShader.end();
            fbo2.end();
        }

        batch.begin();
        batch.setShader(passthroughShader);
        Texture tex2 = fbo2.getColorBufferTexture();
        TextureRegion texReg2 = new TextureRegion(tex2);
        texReg2.flip(false, true);
        batch.draw(texReg2, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//        batch.end();
//        super.draw(batch, parentAlpha);
    }

    public void setupShaders() {
        ShaderProgram.pedantic = false;
        String vertexPassthrough = "PassthroughVertex.glsl";
        shader = compileShader(vertexPassthrough, shader1);
        sobelShader = compileShader(vertexPassthrough, shader2);
        passthroughShader = compileShader(vertexPassthrough, "PassthroughFragment.glsl");

        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
    }

    private void setShaderUniforms() {
        float currentWidth = Gdx.graphics.getWidth();
        float currentHeight = Gdx.graphics.getHeight();

        ParamContainer paramContainer = systemContext.getParamContainer();

        shader.setUniformMatrix("u_projTrans", matrix);
        shader.setUniformf("ratio", currentWidth/(float)currentHeight);
        shader.setUniformf("resolution", width, height);

        shader.setUniformf("colorShift", colorShift);

//		long t = System.currentTimeMillis();
//		if (t-lastIncrease > 1) {
//			lastIncrease = t;
//			iterations++;
//		}
        shader.setUniformi("iterations", paramContainer.getClientParameter("iterations").getGeneral(Integer.class));
        shader.setUniformf("scale", (float)paramContainer.getClientParameter("zoom").getGeneral(Number.class).toDouble());

        float xVariation = (float)((Math.random()-0.5)*(scale/width));
        float yVariation = (float)((Math.random()-0.5)*(scale/width));

        ComplexNumber midpoint = systemContext.getMidpoint();
        ComplexNumber c = paramContainer.getClientParameter("c").getGeneral(ComplexNumber.class);

//        shader.setUniformf("center", (float) midpoint.getReal().toDouble() + xVariation, (float) midpoint.getImag().toDouble() + yVariation);
        shader.setUniformf("center", (float) midpoint.getReal().toDouble(), (float) midpoint.getImag().toDouble());
        shader.setUniformf("biasReal", (float)c.getReal().toDouble());
        shader.setUniformf("biasImag", (float)c.getImag().toDouble());
        shader.setUniformf("samples", currentRefreshes+1);
        shader.setUniformf("flip", currentRefreshes%2 == 1 ? -1 : 1);
        //shader.setUniformi("u_texture", 0);
        //palette.bind(1);
        shader.setUniformi("palette", 0);
        shader.setUniformf("burningship", burningship ? 1f : 0f);
        shader.setUniformf("juliaset", juliaset ? 1f : 0f);
    }

    public ShaderProgram compileShader(String vertexPath, String fragmentPath){
        ShaderProgram shader = new ShaderProgram(Gdx.files.internal(vertexPath),
                Gdx.files.internal(fragmentPath));
        if (!shader.isCompiled()) {
            throw new IllegalStateException("Error compiling shaders ("+vertexPath+", "+fragmentPath+"): "+shader.getLog());
        }
        return shader;
    }

    public void setRefresh(){
        refresh = true;
    }

    @Override
    public double getXShift() {
        return 0;
    }

    @Override
    public double getYShift() {
        return 0;
    }

    @Override
    public boolean isScreenshot(boolean reset) {
        boolean curr = screenshot;
        if (reset)
            screenshot = false;
        return curr;
    }

    @Override
    public SystemContext getSystemContext() {
        return systemContext;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setScreenshot(boolean screenshot) {
        this.screenshot = screenshot;
    }
}
